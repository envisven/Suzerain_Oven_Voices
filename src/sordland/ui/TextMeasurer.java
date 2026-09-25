package sordland.ui;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import sordland.graph.Graph;
import sordland.layout.LayoutEngine;
import java.util.*;

public final class TextMeasurer implements LayoutEngine.Measurer {
    public static final Font TITLE=Font.font("System",FontWeight.SEMI_BOLD,13);
    public static final Font BODY=Font.font("System",14);
    public static final Font META=Font.font("System",12);
    private final Map<String,Double> widths=new HashMap<>();
    private record MeasureKey(Graph.Kind kind,String title,String body,String metadata) {}
    private final Map<MeasureKey,LayoutEngine.Size> sizes=new HashMap<>();
    private double width(String s,Font f){String key=f.toString()+s;return widths.computeIfAbsent(key,k->{Text t=new Text(s);t.setFont(f);return t.getLayoutBounds().getWidth();});}
    public List<String> wrap(String text,double max,Font font) {
        if(text==null||text.isBlank())return List.of();var lines=new ArrayList<String>();
        for(String paragraph:text.split("\\R",-1)){
            if(paragraph.isEmpty()){lines.add("");continue;}
            StringBuilder current=new StringBuilder();
            for(String word:paragraph.split("(?<=\\s)")){
                if(width(current+word,font)<=max){current.append(word);continue;}
                if(!current.isEmpty()){lines.add(current.toString().stripTrailing());current.setLength(0);}
                if(width(word,font)<=max){current.append(word.stripLeading());continue;}
                for(int i=0;i<word.length();){int cp=word.codePointAt(i);String c=new String(Character.toChars(cp));i+=Character.charCount(cp);if(width(current+c,font)>max&&!current.isEmpty()){lines.add(current.toString());current.setLength(0);}current.append(c);}
            }
            if(!current.isEmpty())lines.add(current.toString().stripTrailing());
        }
        return List.copyOf(lines);
    }
    @Override public LayoutEngine.Size measure(Graph.Node n,boolean expanded) {
        if(n.kind==Graph.Kind.JUNCTION)return new LayoutEngine.Size(2,2,List.of(),List.of(),List.of());
        var key=new MeasureKey(n.kind,n.title,n.text,expanded?n.metadata:"");
        var cached=sizes.get(key);if(cached!=null)return cached;
        double w=switch(n.kind){case EVENT->350;case CONDITION,EFFECT->390;case CONTROL,TERMINAL->290;default->350;};
        double usable=w-32-(n.kind==Graph.Kind.EVENT?36:0);
        var title=wrap(n.title,usable,TITLE);var body=wrap(n.text,w-32,BODY);var meta=expanded?wrap(n.metadata,w-32,META):List.<String>of();
        double h=24+Math.max(1,title.size())*18+(body.isEmpty()?0:14+body.size()*20)+(meta.isEmpty()?0:18+meta.size()*17);
        var size=new LayoutEngine.Size(w,h,title,body,meta);sizes.put(key,size);return size;
    }
}
