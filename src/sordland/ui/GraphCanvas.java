package sordland.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import sordland.graph.Graph;
import sordland.layout.LayoutEngine.*;
import java.util.*;
import java.util.function.*;


public final class GraphCanvas extends Region {
    private final Canvas canvas=new Canvas();
    private Result result; private final Viewport viewport=new Viewport(); private double pressX,pressY,lastX,lastY;private boolean dragged;
    private String selected;private boolean speakerColors;
    private final Map<String,Color> typeColors=new HashMap<>();
    private Map<String,Color> speakerPalette=Map.of();
    public void setSpeakerPalette(Map<String,Color> palette){speakerPalette=Map.copyOf(palette);}
    public BiConsumer<Graph.Node,Boolean> onNode=(n,metadata)->{};
    public Consumer<String> onStatus=s->{};
    public GraphCanvas(){
        getChildren().add(canvas);setMinSize(100,100);setStyle("-fx-background-color: #f3f1ec;");
        canvas.setOnScroll(e->{zoomAt(Math.pow(1.0018,e.getDeltaY()),e.getX(),e.getY());e.consume();});
        canvas.setOnMousePressed(e->{pressX=lastX=e.getX();pressY=lastY=e.getY();dragged=false;canvas.requestFocus();});
        canvas.setOnMouseDragged(e->{if(Math.hypot(e.getX()-pressX,e.getY()-pressY)>4)dragged=true;if(dragged){viewport.panX+=e.getX()-lastX;viewport.panY+=e.getY()-lastY;redraw();}lastX=e.getX();lastY=e.getY();});
        canvas.setOnMouseReleased(e->{if(dragged||result==null||e.getButton()!=MouseButton.PRIMARY)return;double x=(e.getX()-viewport.panX)/viewport.scale,y=(e.getY()-viewport.panY)/viewport.scale;Box b=result.hit(x,y);if(b!=null){selected=b.node().id;redraw();onNode.accept(b.node(),b.node().kind==Graph.Kind.EVENT&&x>=b.x()+b.w()-36&&y<=b.y()+Math.max(42,24+b.size().title().size()*18));}});
        canvas.setOnMouseMoved(e->{if(result==null)return;Box b=result.hit((e.getX()-viewport.panX)/viewport.scale,(e.getY()-viewport.panY)/viewport.scale);setCursor(b==null?javafx.scene.Cursor.OPEN_HAND:javafx.scene.Cursor.HAND);});
    }
    @Override protected void layoutChildren(){canvas.setWidth(getWidth());canvas.setHeight(getHeight());redraw();}
    public Result result(){return result;}
    public void setResult(Result r,boolean reset){if(!reset&&result!=null&&selected!=null){Box old=result.byId.get(selected),next=r.byId.get(selected);if(old!=null&&next!=null){viewport.panX+=(old.x()-next.x())*viewport.scale;viewport.panY+=(old.y()-next.y())*viewport.scale;}}result=r;if(reset){viewport.scale=.85;viewport.panX=48;viewport.panY=40;if(!r.boxes.isEmpty())focus(r.boxes.getFirst().node().id);}redraw();}
    public void setSpeakerColors(boolean value){speakerColors=value;redraw();}
    public void zoom(double factor){zoomAt(factor,getWidth()/2,getHeight()/2);}
    private void zoomAt(double factor,double x,double y){viewport.zoomAt(factor,x,y);redraw();}
    public void fit(){if(result==null)return;viewport.fit(getWidth(),getHeight(),result.width,result.height);redraw();}
    public void focus(String id){focus(id,.9);}
    private void focus(String id,double scale){if(result==null)return;Box b=result.byId.get(id);if(b==null)return;viewport.focus(b.cx(),b.y(),getWidth(),scale);selected=id;redraw();}
    public void actualSize(){if(result!=null&&!result.boxes.isEmpty())focus(selected==null?result.boxes.getFirst().node().id:selected,1);}
    public double zoom(){return viewport.scale;} public double panX(){return viewport.panX;} public double panY(){return viewport.panY;}
    public String selected(){return selected;}
    private Color color(Graph.Node n){
        return switch(n.kind){
            case CONDITION->Color.web("#f7e5f2");case EFFECT->Color.web("#e2f1d8");case NARRATOR->Color.web("#e9e8e4");
            case CHOICE->Color.web("#e0ebfa");case CONTROL,TERMINAL->Color.web("#e5e9e9");case REFERENCE,NOTICE->Color.web("#fff0d2");
            case CHARACTER->speakerColors?speakerPalette.getOrDefault(n.speaker,Color.web("#fffefa")):Color.web("#fffefa");
            case EVENT->typeColors.computeIfAbsent(n.type,t->Color.hsb(Math.floorMod(t.hashCode()*137,360),.18,.98));
        };
    }
    public void redraw(){
        GraphicsContext g=canvas.getGraphicsContext2D();g.setTransform(1,0,0,1,0,0);g.setFill(Color.web("#f3f1ec"));g.fillRect(0,0,getWidth(),getHeight());
        if(result==null)return;if(result.boxes.isEmpty()){g.setFill(Color.web("#526557"));g.setFont(Font.font(16));g.fillText("No items match these filters. Clear the search or choose All types / All turns.",40,70);onStatus.accept("0 matching items");return;}g.save();g.translate(viewport.panX,viewport.panY);g.scale(viewport.scale,viewport.scale);
        double left=-viewport.panX/viewport.scale,top=-viewport.panY/viewport.scale,vw=getWidth()/viewport.scale,vh=getHeight()/viewport.scale;
        int sectorIndex=0;
        for(var s:result.sectors){boolean alt=sectorIndex++%2==0;if(s.y()+s.height()<top||s.y()>top+vh)continue;
            g.setFill(Color.web(alt?"#f3f1ec":"#eae8e2"));g.fillRect(0,s.y(),result.width,s.height());g.setStroke(Color.web("#cfcec8"));g.setLineWidth(1/viewport.scale);g.strokeLine(0,s.y(),result.width,s.y());
            if(viewport.scale>.08){g.save();g.translate(48,s.y()+Math.min(s.height()-25,Math.max(160,s.height()/2)));g.rotate(-90);g.setFill(Color.web("#717c77"));g.setFont(Font.font("System",14));g.fillText(s.label(),0,0);g.restore();}
        }
        g.setLineWidth(Math.max(1.2,1/viewport.scale));
        for(var line:result.lines){var f=line.from();var t=line.to();boolean back=line.edge().back;
            double minY=Math.min(f.bottom(),t.y()),maxY=Math.max(f.bottom(),t.y());if(maxY+25<top||minY-25>top+vh)continue;
            g.setStroke(Color.web(back?"#9672aa":"#87928e"));g.setLineDashes(back?new double[]{6,5}:new double[]{});
            g.beginPath();boolean first=true;for(var point:line.points()){if(first){g.moveTo(point.x(),point.y());first=false;}else g.lineTo(point.x(),point.y());}g.stroke();
            if(result.sectors.isEmpty()&&viewport.scale>.18){
                
                g.setLineDashes();g.strokeLine(t.cx()-4,t.y()-7,t.cx(),t.y());g.strokeLine(t.cx()+4,t.y()-7,t.cx(),t.y());
            }
            if(viewport.scale>.55&&line.edge().from.equals(selected)&&!line.edge().label.isBlank()){g.setFont(Font.font("System",10));g.setFill(Color.web("#62736c"));g.fillText(line.edge().label,t.cx()-130,t.y()-10,260);}
        }
        g.setLineDashes();var visible=result.visible(left,top,vw,vh);
        for(var b:visible)drawNode(g,b);
        g.restore();onStatus.accept(String.format(Locale.ROOT,"%,d / %,d boxes visible   ·   %.0f%%",visible.size(),result.boxes.size(),viewport.scale*100));
    }
    private void drawNode(GraphicsContext g,Box b){
        var n=b.node();g.setFill(color(n));g.fillRoundRect(b.x(),b.y(),b.w(),b.h(),12,12);
        g.setStroke(Color.web(n.id.equals(selected)?"#17634e":n.kind==Graph.Kind.CONDITION?"#9d69ad":"#b9c1ba"));g.setLineWidth(n.id.equals(selected)?2.4:1);g.strokeRoundRect(b.x(),b.y(),b.w(),b.h(),12,12);
        if(viewport.scale<.13)return;
        double clipTop=-viewport.panY/viewport.scale-22,clipBottom=(getHeight()-viewport.panY)/viewport.scale+22;
        double y=b.y()+22;g.setFill(Color.web("#26392f"));g.setFont(TextMeasurer.TITLE);
        for(String line:b.size().title()){if(y>=clipTop&&y<=clipBottom)g.fillText(line,b.x()+16,y);y+=18;}
        if(n.kind==Graph.Kind.EVENT){g.setStroke(Color.web("#b9c1ba"));g.strokeLine(b.x()+b.w()-36,b.y(),b.x()+b.w()-36,b.y()+Math.max(42,24+b.size().title().size()*18));g.setFont(Font.font(19));g.fillText(b.size().metadata().isEmpty()?"›":"⌄",b.x()+b.w()-25,b.y()+27);}
        if(!b.size().body().isEmpty()){y+=12;g.setFont(TextMeasurer.BODY);for(String line:b.size().body()){if(y>=clipTop&&y<=clipBottom)g.fillText(line,b.x()+16,y);y+=20;}}
        if(!b.size().metadata().isEmpty()){y+=14;g.setFont(TextMeasurer.META);g.setFill(Color.web("#526557"));for(String line:b.size().metadata()){if(y>=clipTop&&y<=clipBottom)g.fillText(line,b.x()+16,y);y+=17;}}
    }
}
