package sordland.layout;

import sordland.graph.Graph;
import sordland.graph.Graph.*;
import sordland.layout.LayoutEngine.*;
import java.util.*;

                                                                                                                      
public final class PanelLayout {
    private record Metrics(double width,double height,double headerY,double conditionY,double effectY,List<Double> spans) {}
    public Result layout(Graph graph,Set<String> expanded,Measurer measurer){
        var byId=new LinkedHashMap<String,Node>();var sizes=new HashMap<String,Size>();
        for(Node n:graph.nodes){byId.put(n.id,n);sizes.put(n.id,measurer.measure(n,expanded.contains(n.id)));}
        var owners=new HashMap<String,PanelGroup>();var metrics=new HashMap<String,Metrics>();
        for(PanelGroup p:graph.panels){
            p.members().forEach(id->owners.put(id,p));
            double headerHeight=0,conditionHeight=0,effectHeight=0;var spans=new ArrayList<Double>();
            for(PanelCategory category:p.categories()){
                headerHeight=Math.max(headerHeight,sizes.get(category.headerId()).height());double span=0;
                for(PanelBranch b:category.branches()){
                    Size effect=sizes.get(b.effectId());Size condition=b.conditionId().isEmpty()?null:sizes.get(b.conditionId());
                    span+=Math.max(effect.width(),condition==null?0:condition.width())+30;
                    effectHeight=Math.max(effectHeight,effect.height());if(condition!=null)conditionHeight=Math.max(conditionHeight,condition.height());
                }
                spans.add(Math.max(sizes.get(category.headerId()).width(),Math.max(0,span-30))+60);
            }
            double width=Math.max(sizes.get(p.entryId()).width()+80,spans.stream().mapToDouble(Double::doubleValue).sum()+60);
            double hy=sizes.get(p.entryId()).height()+70,cy=hy+headerHeight+70,ey=conditionHeight==0?cy:cy+conditionHeight+60;
            metrics.put(p.id(),new Metrics(width,ey+effectHeight+90,hy,cy,ey,spans));
        }
        var nodes=graph.nodes.stream().filter(n->!owners.containsKey(n.id)||owners.get(n.id).entryId().equals(n.id)).toList();
        var edges=new ArrayList<Edge>();var originals=new IdentityHashMap<Edge,Edge>();
        for(Edge e:graph.edges){
            PanelGroup from=owners.get(e.from),to=owners.get(e.to);
            if(from!=null&&from==to)continue;
            String f=from==null?e.from:from.entryId(),t=to==null?e.to:to.entryId();
            Edge proxy=new Edge(f,t,e.label,e.back);edges.add(proxy);originals.put(proxy,e);
        }
        Graph macro=new Graph(graph.title,nodes,edges,graph.diagnostics);
        Result base=new LayoutEngine().dialogue(macro,expanded,(n,ex)->{
            PanelGroup p=owners.get(n.id);if(p==null)return sizes.get(n.id);Metrics m=metrics.get(p.id());return new Size(m.width(),m.height(),List.of(),List.of(),List.of());
        });
        var boxes=new ArrayList<Box>();var groups=new ArrayList<Group>();
        for(Box b:base.boxes){
            PanelGroup p=owners.get(b.node().id);if(p==null){boxes.add(b);continue;}
            Metrics m=metrics.get(p.id());
            boxes.add(at(byId,sizes,p.entryId(),b.cx(),b.y()));
                                                                                      
            boxes.add(new Box(byId.get(p.completionId()),b.cx()-1,b.bottom()-2,new Size(2,2,List.of(),List.of(),List.of())));
            double x=b.x()+30;
            for(int ci=0;ci<p.categories().size();ci++){
                PanelCategory category=p.categories().get(ci);double span=m.spans().get(ci);
                boxes.add(at(byId,sizes,category.headerId(),x+span/2,b.y()+m.headerY()));
                double total=0;for(PanelBranch branch:category.branches())total+=branchWidth(branch,sizes)+30;total=Math.max(0,total-30);
                double bx=x+(span-total)/2;
                for(PanelBranch branch:category.branches()){
                    double w=branchWidth(branch,sizes),cx=bx+w/2;
                    if(!branch.conditionId().isEmpty())boxes.add(at(byId,sizes,branch.conditionId(),cx,b.y()+m.conditionY()));
                    boxes.add(at(byId,sizes,branch.effectId(),cx,b.y()+m.effectY()));bx+=w+30;
                }
                x+=span;
            }
            groups.add(new Group(p.id(),b.x(),b.y(),b.w(),b.h(),p.entryId(),p.completionId(),p.members()));
        }
        var placed=new HashMap<String,Box>();boxes.forEach(b->placed.put(b.node().id,b));var lines=new ArrayList<Line>();
        for(Line line:base.lines){Edge original=originals.get(line.edge());lines.add(new Line(original,placed.get(original.from),placed.get(original.to),line.lane(),line.points()));}
        for(Edge e:graph.edges){
            PanelGroup p=owners.get(e.from);if(p==null||p!=owners.get(e.to))continue;
            Box from=placed.get(e.from),to=placed.get(e.to);var points=new ArrayList<Point>();
            if(e.to.equals(p.completionId())){
                Group g=groups.stream().filter(gr->gr.id().equals(p.id())).findFirst().orElseThrow();
                                                                                                      
                double rail=g.x()+g.width()-15;
                points.add(new Point(from.x()+from.w(),from.y()+from.h()/2));points.add(new Point(rail,from.y()+from.h()/2));points.add(new Point(rail,g.bottom()-24));points.add(new Point(to.cx(),g.bottom()-24));points.add(new Point(to.cx(),to.y()));
            }else{
                double channel=from.bottom()+24;
                points.add(new Point(from.cx(),from.bottom()));points.add(new Point(from.cx(),channel));points.add(new Point(to.cx(),channel));points.add(new Point(to.cx(),to.y()));
            }
            lines.add(new Line(e,from,to,0,List.copyOf(points)));
        }
        return new Result(graph,boxes,lines,base.sectors,groups,base.width,base.height);
    }
    private static double branchWidth(PanelBranch branch,Map<String,Size> sizes){return Math.max(sizes.get(branch.effectId()).width(),branch.conditionId().isEmpty()?0:sizes.get(branch.conditionId()).width());}
    private static Box at(Map<String,Node> nodes,Map<String,Size> sizes,String id,double cx,double y){Size size=sizes.get(id);return new Box(nodes.get(id),cx-size.width()/2,y,size);}
}
