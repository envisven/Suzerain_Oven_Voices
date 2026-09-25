package sordland.data;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.*;
import static sordland.data.Domain.*;

                                                                                                         
public final class Loader {
    public static final String ENTITY_FILE = "SuzerainDataDumper.entity_data.json";
    public static final String CONVERSATIONS_FILE = "SuzerainDataDumper.conversations_Sordland.json";
    private static final Pattern TURN = Pattern.compile("(?:^|[/_ -])Turn(\\d+)(?=[/_ -]|$)",Pattern.CASE_INSENSITIVE);
    private static final Pattern SPEAKER = Pattern.compile("^([^:\\n]{1,80}):\\s*\"");
    private Loader() {}

    public static Dataset load(Path entityPath, Path conversationsPath) throws IOException {
        List<String> diagnostics = new ArrayList<>();
        Map<String,Object> catalogue = requireObject(Json.parse(entityPath), "Entity root");
        for(String key:List.of("AllConversationsData","AllBillsData","AllDecisionsData"))
            requireCollection(catalogue.get(key), "Entity root."+key);
        Map<Integer,Conversation> conversations = readConversations(conversationsPath, diagnostics);
        Map<String,Conversation> byTitle = new LinkedHashMap<>();
        Set<String> ambiguousTitles = new HashSet<>();
        for(Conversation c:conversations.values()) {
            if(ambiguousTitles.contains(c.title()))continue;
            if(byTitle.containsKey(c.title())) {
                byTitle.remove(c.title());ambiguousTitles.add(c.title());
                diagnostics.add("Ambiguous conversation title: "+c.title()+". Catalogue references to it remain unresolved; numeric source identities stay separate.");
            } else byTitle.put(c.title(),c);
        }
        List<Item> items = new ArrayList<>(), ancillary = new ArrayList<>();
        Set<Integer> referenced = new HashSet<>();
        int excluded=0;
        for(var collection:catalogue.entrySet()) {
            if(collection.getKey().equals("_type"))continue;
            if(!(collection.getValue() instanceof Map<?,?> || collection.getValue() instanceof List<?>))continue;
            int index=0;
            List<String> sourceKeys=collection.getValue() instanceof Map<?,?> ? Json.object(collection.getValue()).keySet().stream()
                .filter(k->k.matches("\\d+")).sorted(Comparator.comparing(java.math.BigInteger::new)).toList() : List.of();
            for(Object value:Json.list(collection.getValue())) {
                Map<String,Object> raw = requireObject(value,collection.getKey()+"["+index+"]");
                String id=collection.getKey()+":"+(sourceKeys.isEmpty()?index:sourceKeys.get(index));index++;
                if(!isSordland(raw)){excluded++;continue;}
                Item item=readItem(id,collection.getKey(),raw,byTitle,diagnostics);
                if(item.conversationId()!=null)referenced.add(item.conversationId());
                if(collection.getKey().equals("AllConversationsData") || collection.getKey().equals("AllDecisionsData") || collection.getKey().equals("AllBillsData"))items.add(item);
                else ancillary.add(item);
            }
        }
        int fragments=0;
        for(Conversation c:conversations.values()) {
            if(referenced.contains(c.id()))continue;
            Map<String,Object> raw=new LinkedHashMap<>();raw.put("id",c.id());raw.put("Title",c.title());
            raw.put("Source", "Conversation database only; no campaign entity was supplied for this graph.");
            raw.put("TurnSource", turn(c.title())==null ? "Unspecified in source" : "Conversation.Title path");
            raw.put("Progression", "Unresolved: dialogue fragment links are not campaign event sequencing.");
            String type=isEnding(c.title()) ? "Ending" : "Dialogue fragment";
            items.add(new Item("conversation:"+c.id(),type,lastPart(c.title()),c.title(),c.title(),turn(c.title()),c.id(),"","","","No catalogue entity; retained for complete access to supplied Sordland dialogue.",List.of(),raw));
            fragments++;
        }
        long entryCount=conversations.values().stream().mapToLong(c->c.entries().size()).sum();
        diagnostics.add("Loaded "+conversations.size()+" Sordland conversations with "+entryCount+" dialogue entries.");
        diagnostics.add("Loaded "+(items.size()-fragments)+" campaign catalogue items and "+fragments+" additional dialogue graphs; "+ancillary.size()+" ancillary records.");
        diagnostics.add("Excluded "+excluded+" non-Sordland catalogue records using explicit path/story-pack evidence.");
        diagnostics.add("Campaign execution order is absent from the supplied files. Turn placement and activation predicates are known; event-to-event progression is unresolved.");
        diagnostics.add("No actor database or local portrait assets were supplied. Speaker names are corroborated from source dialogue titles.");
        return new Dataset(items,conversations,ancillary,diagnostics);
    }

    private static Map<Integer,Conversation> readConversations(Path path,List<String> diagnostics)throws IOException {
        Map<String,Object> root=requireObject(Json.parse(path),"Conversation root");
        Object collection=root.get("conversations");requireCollection(collection,"Conversation root.conversations");
        List<Object> values=Json.list(collection);
        if(values.isEmpty())throw new IOException("Conversation database has no conversations.");
        Map<Integer,Map<String,Integer>> names=new LinkedHashMap<>();
                                                                                                     
        for(Object value:values) {
            Map<String,Object> c=requireObject(value,"Conversation");
            if(!Json.string(c,"Title").startsWith("Sordland/"))continue;
            requireCollection(c.get("dialogueEntries"),"Conversation.dialogueEntries");
            for(Object ev:Json.list(c.get("dialogueEntries"))) {
                Map<String,Object> e=requireObject(ev,"DialogueEntry");Matcher matcher=SPEAKER.matcher(Json.string(e,"Title"));
                if(matcher.find()&&!matcher.group(1).equalsIgnoreCase("Jump to"))names.computeIfAbsent(requiredInt(e,"ActorID","DialogueEntry"),x->new LinkedHashMap<>()).merge(matcher.group(1),1,Integer::sum);
            }
        }
        Map<Integer,String> actors=new HashMap<>();
        for(var e:names.entrySet()) {
            String best=e.getValue().entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();actors.put(e.getKey(),best);
            if(e.getValue().size()>1)diagnostics.add("Actor "+e.getKey()+" has multiple source speaker names: "+e.getValue()+"; each spoken entry preserves its own label.");
        }
        Map<Integer,Conversation> result=new LinkedHashMap<>();
        for(Object value:values) {
            Map<String,Object> raw=requireObject(value,"Conversation");String title=requiredString(raw,"Title","Conversation");
            if(!title.startsWith("Sordland/")){diagnostics.add("Excluded non-Sordland conversation "+title);continue;}
            int cid=requiredInt(raw,"id",title);Map<Integer,Entry> entries=new LinkedHashMap<>();
            for(Object ev:Json.list(raw.get("dialogueEntries"))) {
                Map<String,Object> e=requireObject(ev,"DialogueEntry in "+cid);int did=requiredInt(e,"id","DialogueEntry");
                int owner=requiredInt(e,"conversationID","DialogueEntry");if(owner!=cid)throw new IOException("Entry "+cid+":"+did+" declares conversationID "+owner);
                requireCollection(e.get("fields"),"DialogueEntry.fields");requireCollection(e.get("outgoingLinks"),"DialogueEntry.outgoingLinks");
                Map<String,Object> fields=new LinkedHashMap<>();
                for(Object fv:Json.list(e.get("fields"))) {Map<String,Object> f=requireObject(fv,"Field");fields.put(requiredString(f,"title","Field"),f.get("value"));}
                List<Link> links=new ArrayList<>();int order=0;
                for(Object lv:Json.list(e.get("outgoingLinks"))) {
                    Map<String,Object> link=requireObject(lv,"Link");
                    if(requiredInt(link,"originConversationID","Link")!=cid || requiredInt(link,"originDialogueID","Link")!=did)
                        throw new IOException("Outgoing link origin disagrees with entry "+cid+":"+did);
                    links.add(new Link(new EntryKey(requiredInt(link,"destinationConversationID","Link"),requiredInt(link,"destinationDialogueID","Link")),order++,Json.string(link,"priority"),Boolean.TRUE.equals(link.get("isConnector"))));
                }
                int actor=requiredInt(e,"ActorID","DialogueEntry");String etitle=Json.string(e,"Title");Matcher label=SPEAKER.matcher(etitle);
                String speaker=label.find()&&!label.group(1).equalsIgnoreCase("Jump to")?label.group(1):actors.getOrDefault(actor,"Actor "+actor);
                String text=prefer(Json.string(fields,"en"),Json.string(fields,"Dialogue Text"));
                String menu=prefer(Json.string(fields,"Menu Text en"),Json.string(fields,"Menu Text"));
                String sequence=join(Json.string(fields,"Sequence"),Json.string(fields,"Sequence en"));
                Map<String,Object> compact=new LinkedHashMap<>(e);compact.remove("fields");compact.remove("outgoingLinks");
                compact.put("fields",Collections.unmodifiableMap(fields));
                Entry entry=new Entry(new EntryKey(cid,did),actor,speaker,etitle,text,menu,Json.string(e,"conditionsString"),Json.string(e,"userScript"),sequence,links,compact);
                if(entries.putIfAbsent(did,entry)!=null)throw new IOException("Duplicate dialogue identity "+cid+":"+did);
            }
            if(result.putIfAbsent(cid,new Conversation(cid,title,entries))!=null)throw new IOException("Duplicate conversation id "+cid);
        }
        if(result.isEmpty())throw new IOException("Conversation database contains no Sordland conversations.");
        int missing=0,cross=0;
        for(Conversation c:result.values())for(Entry e:c.entries().values())for(Link l:e.links()) {
            Conversation dest=result.get(l.target().conversationId());
            if(dest==null || !dest.entries().containsKey(l.target().dialogueId())) {
                if(missing++<30)diagnostics.add("Unresolved source dialogue link "+e.key()+" -> "+l.target());
            }
            if(l.target().conversationId()!=e.key().conversationId())cross++;
        }
        if(missing>0)diagnostics.add("Unresolved dialogue links: "+missing+" (first 30 listed).");
        diagnostics.add("Source cross-conversation dialogue links: "+cross+". These include subdialogue calls/returns; they do not establish campaign order.");
        return result;
    }

    private static Item readItem(String id,String collection,Map<String,Object> raw,Map<String,Conversation> conversations,List<String> diagnostics)throws IOException {
        String name=requiredString(raw,"NameInDatabase",collection), path=Json.string(raw,"Path");
        Map<String,Object> story=Json.object(raw.get("StoryFragmentProperties"));
        String type,propertiesName;
        switch(collection) {
            case "AllConversationsData" -> {type=isEnding(name)?"Ending":"Conversation";propertiesName="ConversationProperties";}
            case "AllBillsData" -> {type="Bill";propertiesName="BillProperties";}
            case "AllDecisionsData" -> {type="Decision";propertiesName="DecisionProperties";}
            case "NewsData" -> {type="News";propertiesName="NewsProperties";}
            case "conditionalInstructionData" -> {type="Conditional instruction";propertiesName="ConditionalInstructionProperties";}
            default -> {type=Json.string(raw,"_type");propertiesName="";}
        }
        Map<String,Object> p=propertiesName.isEmpty()?Map.of():requireObject(raw.get(propertiesName),collection+"."+propertiesName);
        String title=prefer(Json.string(p,"Title"),Json.string(p,"HubTitle"));if(title.isBlank()||title.equals("-"))title=name;
        String dialogue=Json.string(p,"Dialogue");Integer cid=null;
        if(!dialogue.isBlank()){Conversation c=conversations.get(dialogue);if(c==null)diagnostics.add("Unresolved catalogue dialogue reference: "+name+" -> "+dialogue);else cid=c.id();}
        Integer turn=turn(name);String turnSource=turn==null?"Unspecified in source":"NameInDatabase";
        Integer dialogueTurn=turn(dialogue);
        if(turn==null&&dialogueTurn!=null){turn=dialogueTurn;turnSource="ConversationProperties.Dialogue path";}
        if(turn!=null&&dialogueTurn!=null&&!turn.equals(dialogueTurn))diagnostics.add("Conflicting turn labels for "+name+": name="+turn+", dialogue="+dialogueTurn);
        if(p.get("TurnNo") instanceof Number n){turn=n.intValue();turnSource=propertiesName+".TurnNo";}
        if(turn==null&&turn(path)!=null){turn=turn(path);turnSource="Path";}
        List<Option> options=new ArrayList<>();
        if(type.equals("Bill")) {
            options.add(new Option("SIGN","",Json.string(p,"SignVariables")));
            String disabled=Json.string(p,"IsVetoDisabledCondition");
                                                                                                                   
            options.add(new Option("VETO",disabled.isBlank()?"":"Disabled when: "+disabled,Json.string(p,"VetoVariables")));
        } else if(type.equals("Decision")) {
            requireCollection(p.get("Options"),name+".DecisionProperties.Options");
            for(Object ov:Json.list(p.get("Options"))){Map<String,Object> o=requireObject(ov,"DecisionOption");options.add(new Option(Json.string(o,"Text"),Json.string(o,"Condition"),Json.string(o,"Instruction")));}
        } else if(type.equals("Conditional instruction")) {
            for(Object ov:Json.list(p.get("ConditionalInstructions"))){Map<String,Object> o=requireObject(ov,"ConditionalInstruction");options.add(new Option("Instruction "+(options.size()+1),Json.string(o,"Condition"),Json.string(o,"Instruction")));}
        }
        Map<String,Object> metadata=new LinkedHashMap<>(raw);metadata.put("TurnSource",turnSource);
        return new Item(id,type,title,name,path,turn,cid,Json.string(story,"StoryFragmentCondition"),Json.string(story,"OnStoryFragmentBeginInstruction"),Json.string(story,"OnStoryFragmentEndInstruction"),Json.string(p,"Description"),options,metadata);
    }
    private static boolean isSordland(Map<String,Object> raw) {
        String path=Json.string(raw,"Path"), dialogue=Json.string(Json.object(raw.get("ConversationProperties")),"Dialogue");
        List<Object> packs=Json.list(Json.object(raw.get("AppBundleProperties")).get("StoryPacks"));
        if(path.startsWith("Rizia/") || dialogue.startsWith("Rizia/"))return false;
        if(path.startsWith("Sordland/") || dialogue.startsWith("Sordland/"))return true;
        return packs.contains("StoryPack_Main") && !packs.contains("StoryPack_Rizia");
    }
    public static Integer turn(String value) {Matcher m=TURN.matcher(value);return m.find()?Integer.valueOf(m.group(1)):null;}
    private static boolean isEnding(String s){String l=s.toLowerCase(Locale.ROOT);return l.contains("ending") || l.contains("epilogue");}
    private static String lastPart(String s){return s.substring(s.lastIndexOf('/')+1);}
    private static String prefer(String first,String second){return first.isBlank()?second:first;}
    private static String join(String first,String second){if(first.isBlank())return second;if(second.isBlank()||first.equals(second))return first;return first+"\n"+second;}
    private static Map<String,Object> requireObject(Object value,String context)throws IOException {if(!(value instanceof Map<?,?>))throw new IOException(context+" must be a JSON object.");return Json.object(value);}
    private static void requireCollection(Object value,String context)throws IOException {
        if(!(value instanceof Map<?,?> || value instanceof List<?>))throw new IOException(context+" must be a numbered object or JSON array.");
        if(value instanceof Map<?,?> map)for(Object key:map.keySet())if(!key.equals("_type")&&!String.valueOf(key).matches("\\d+"))throw new IOException(context+" has a non-numeric entry key: "+key);
    }
    private static String requiredString(Map<String,Object> map,String key,String context)throws IOException {if(!(map.get(key) instanceof String s))throw new IOException(context+"."+key+" must be a string.");return s;}
    private static int requiredInt(Map<String,Object> map,String key,String context)throws IOException {
        if(!(map.get(key) instanceof Number n) || n.doubleValue()!=n.intValue())throw new IOException(context+"."+key+" must be an integer.");return n.intValue();
    }
}
