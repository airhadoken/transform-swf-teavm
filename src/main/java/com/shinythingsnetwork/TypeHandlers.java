package com.shinythingsnetwork;

import com.flagstone.transform.button.DefineButton2;
import com.flagstone.transform.font.DefineFont2;
import com.flagstone.transform.image.DefineJPEGImage2;
import com.flagstone.transform.image.DefineJPEGImage;
import com.flagstone.transform.image.JPEGEncodingTable;
import com.flagstone.transform.movieclip.DefineMovieClip;
import com.flagstone.transform.shape.DefineMorphShape;
import com.flagstone.transform.shape.DefineShape2;
import com.flagstone.transform.shape.DefineShape3;
import com.flagstone.transform.shape.DefineShape;
import com.flagstone.transform.sound.DefineSound;
import com.flagstone.transform.sound.SoundStreamBlock;
import com.flagstone.transform.sound.SoundStreamHead;
import com.flagstone.transform.sound.StartSound;
import com.flagstone.transform.text.DefineText2;
import com.flagstone.transform.text.DefineText;
import com.flagstone.transform.text.DefineTextField;
import com.flagstone.transform.Background;
import com.flagstone.transform.DoAction;
import com.flagstone.transform.FrameLabel;
import com.flagstone.transform.MovieHeader;
import com.flagstone.transform.Place2;
import com.flagstone.transform.Protect;
import com.flagstone.transform.Remove2;
import com.flagstone.transform.ShowFrame;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import com.flagstone.transform.MovieTag;
import com.flagstone.transform.shape.*;
import com.flagstone.transform.fillstyle.FillStyle;
import com.flagstone.transform.fillstyle.BitmapFill;
import com.flagstone.transform.fillstyle.Gradient;
import com.flagstone.transform.fillstyle.GradientFill;
import com.flagstone.transform.fillstyle.SolidFill;
import com.flagstone.transform.linestyle.LineStyle;
import com.flagstone.transform.linestyle.LineStyle1;
import com.flagstone.transform.linestyle.LineStyle2;
import com.flagstone.transform.datatype.Color;
import com.flagstone.transform.datatype.Bounds;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.typedarrays.Uint8Array;
import com.shinythingsnetwork.svg.*;

@JSFunctor
interface MovieTagHandler {
  void doThing(MovieTag tag);
}

class DefineShapeHandler implements MovieTagHandler {

  private Integer currentLineStyleIndex = null;
  private Integer currentFillStyleIndex = null;
  private Integer currentAltFillStyleIndex = null;

  public String formatColor(Color c) {
    return String.format(
      "rgba(%d, %d, %d, %f)",
      c.getRed(),
      c.getGreen(),
      c.getBlue(),
      (float)c.getAlpha() / 255.0
    );
  }

  public String[] stringifyLineStyle(LineStyle ls) {
    String[] returnVal = new String[2];
    Color styleColor = null;
    if(ls instanceof LineStyle1) {
      styleColor = ((LineStyle1)ls).getColor();
      returnVal[1] = Integer.toString(((LineStyle1)ls).getWidth());
    } else if(ls instanceof LineStyle2) {
      styleColor = ((LineStyle2)ls).getColor();
      returnVal[1] = Integer.toString(((LineStyle2)ls).getWidth());
    }
    returnVal[0] = String.format(
      "rgba(%d, %d, %d, %f)",
      styleColor.getRed(),
      styleColor.getGreen(),
      styleColor.getBlue(),
      (float)styleColor.getAlpha() / 255.0
    );
    return returnVal;
  }

  public String stringifyFillStyle(FillStyle fs) {
    if(fs instanceof SolidFill) {
      final Color styleColor = ((SolidFill)fs).getColor();
      return formatColor(styleColor);
    }
    if(fs instanceof GradientFill) {
      GradientFill gradient = (GradientFill)fs;
      // Note: look at http://larryhou.github.io/blog/2015/09/05/convert-swf-vector-shape-to-svg-image/
      // line 156 or so in the code, to understand how to convert a 2d matrix to a gradient angle.
      final StringBuilder sb = new StringBuilder("linear-gradient(top");
      for(Gradient g: gradient.getGradients()) {
        sb.append(", ");
        sb.append(formatColor(g.getColor()));
        sb.append(' ');
        sb.append(g.getRatio() * 100 / 255);
        sb.append('%');
      }
      sb.append(')');
      return sb.toString();
    }
    if(fs instanceof BitmapFill) {
      System.out.println("Setting a bitmap fill for id: " + DefineJPEGImageHandler.blobsMap.get(((BitmapFill)fs).getIdentifier()));
      return "url(" + DefineJPEGImageHandler.blobsMap.get(((BitmapFill)fs).getIdentifier()) + ")";
    }
    return null;
  }

  public void doThing(MovieTag tag) {
    final String r = Integer.toString(new Random().nextInt());

    final DefineShape dt = (DefineShape)tag;
    final HTMLDocument document = Window.current().getDocument();

    final Shape shape = dt.getShape();
    final List<ShapeRecord> objects = shape.getObjects();

    int i;
    // Fill Styles can be:
    // SolidFill:  just has a Color
    // GradientFill:
    // - transform: CoordTransform { scaleX (float), scaleY (float), shearX (float), shearY (float), transX (int), transY (int) }
    // - gradients: Array<Gradient>, where Gradient = { ratio (int, range 0-255), color (Color) }
    // BitmapFill:
    // - identifier (int)
    // - transform: CoordTransform
    final List<FillStyle> fillStyles = dt.getFillStyles();

    final List<StringBuilder> fillStylePathBuffers = new ArrayList<StringBuilder>(fillStyles.size());
    for(i = 0; i < fillStyles.size(); i++) {
      fillStylePathBuffers.add(new StringBuilder("M 0 0"));
    } 

    // Line styles are always:
    // - width (int) in twips (20 to a pt)
    // - Color (Color):
    //   - red/green/blue (int) 0 to 255
    final List<LineStyle> lineStyles = dt.getLineStyles();

    final List<StringBuilder> lineStylePathBuffers = new ArrayList<StringBuilder>(lineStyles.size());
    for(i = 0; i < lineStyles.size(); i++) {
      lineStylePathBuffers.add(new StringBuilder("M 0 0"));
    } 

    final SVGGElement group = (SVGGElement)document.createElement("g");
    /*
    Losing my ability to think in code for the night, but my understanding of the loop is now this:
    - Make a string builder for each fill style.
    - when a particular index (nonzero) comes up for the normal fill style, put every path script in the same index builder
    - when a particular index (nonzero) comes up for the alternate fill style, put every path script in the same index builder IN REVERSE
      - this isn't correct.  The problem here is that the order isn't doing what you think it is.
      - there's no guarantee that these points will happen *in order* for any given path.  You have to put together
        edge lists for each fill (with the ones for the alternate fill being reversed) and these either need to use
        absolute coordinates or they need to be stored Point->Vector style
      - I'm not entirely sure about this but I *think* once a move happens in a style record, that effectively starts new shapes.

      - i'm unsure yet if they need sign changes (* -1) but I think they do because they're relative paths.
    - moves are absolute, so put them in every path script
    - to finalize simply output each script into a path tag with its corresponding fill.
       - you may need a particular fill strategy to make this work
       - the default seems to be right, though.
    - doing strokes as separate paths would be easiest; set the fill to transparent
      -- SVG eagerly fills paths
     */


    // int i = -1;
    for(ShapeRecord record : objects) {
      // i++;
      if(record instanceof Curve) {
        final Curve curveRecord = (Curve)record;
        if(currentFillStyleIndex != null) {
          fillStylePathBuffers.get(currentFillStyleIndex).append(
            String.format(
              " s %d %d, %d %d",
              curveRecord.getControlX(),
              curveRecord.getControlY(),
              curveRecord.getControlX() + curveRecord.getAnchorX(),
              curveRecord.getControlY() + curveRecord.getAnchorY()
            )
          );
        }
        if(currentAltFillStyleIndex != null) {
          fillStylePathBuffers.get(currentAltFillStyleIndex).append(
            String.format(
              " s %d %d, %d %d",
              curveRecord.getControlX(),
              curveRecord.getControlY(),
              curveRecord.getControlX() + curveRecord.getAnchorX(),
              curveRecord.getControlY() + curveRecord.getAnchorY()
            )
          );
        }
        if(currentLineStyleIndex != null) {
          lineStylePathBuffers.get(currentLineStyleIndex).append(
            String.format(
              " s %d %d, %d %d",
              curveRecord.getControlX(),
              curveRecord.getControlY(),
              curveRecord.getControlX() + curveRecord.getAnchorX(),
              curveRecord.getControlY() + curveRecord.getAnchorY()
            )
          );
        }
      }
      else if(record instanceof Line) {
        final Line lineRecord = (Line)record;
        if(currentFillStyleIndex != null) {
          fillStylePathBuffers.get(currentFillStyleIndex).append(
            String.format(" l %d, %d", lineRecord.getX(), lineRecord.getY())
          );
        }
        if(currentAltFillStyleIndex != null) {
          fillStylePathBuffers.get(currentAltFillStyleIndex).append(
            String.format(" l %d, %d", lineRecord.getX(), lineRecord.getY())
          );
        }
        if(currentLineStyleIndex != null) {
          lineStylePathBuffers.get(currentLineStyleIndex).append(
            String.format(" l %d, %d", lineRecord.getX(), lineRecord.getY())
          );
        }
      }
      else if(record instanceof ShapeData) {
        System.out.println("ShapeData found.  What's in it?");
      }
      else if(record instanceof ShapeStyle) {

        final ShapeStyle styleRecord = (ShapeStyle) record;
        if(styleRecord.getLineStyle() == null) {
          // noop
        } else if(styleRecord.getLineStyle() == 0) {
          currentLineStyleIndex = null;
        } else {
          currentLineStyleIndex = styleRecord.getLineStyle() - 1;
        }
        // Do same for fill style
        if(styleRecord.getFillStyle() == null) {
          // noop
        } else if(styleRecord.getFillStyle() == 0) {
          currentFillStyleIndex = null;
        } else {
          currentFillStyleIndex = styleRecord.getFillStyle() - 1;
        }

        // Try doing alternate fill style something
        if(styleRecord.getAltFillStyle() == null) {
          // noop
        } else if(styleRecord.getAltFillStyle() == 0) {
          currentAltFillStyleIndex = null;
        } else {
          currentAltFillStyleIndex = styleRecord.getAltFillStyle() - 1;
        }

        // Do same for move;
        // NOTE: moves are absolute here.  they start from the origin and not the previous drawing point
        if(styleRecord.getMoveX() != null || styleRecord.getMoveY() != null) {
          final String moveScript = String.format(" M %d %d", styleRecord.getMoveX(), styleRecord.getMoveY());
          // if(currentFillStyleIndex != null) {
          //   fillStylePathBuffers.get(currentFillStyleIndex).append(moveScript);
          // }
          // if(currentAltFillStyleIndex != null) {
          //   fillStylePathBuffers.get(currentAltFillStyleIndex).append(moveScript);
          // }
          // if(currentLineStyleIndex != null) {
          //   lineStylePathBuffers.get(currentLineStyleIndex).append(moveScript);
          // }
          for (StringBuilder buffer: fillStylePathBuffers) {
            buffer.append(moveScript);
          }
          for (StringBuilder buffer: lineStylePathBuffers) {
            buffer.append(moveScript);
          }
        }
      }
      else if(record instanceof ShapeStyle2) {
        System.out.println("ShapeStyle2 found");
      }
      else {
        throw new RuntimeException("Unhandled shape record found");
      }
    }

    for(i = 0; i < fillStyles.size(); i++) {
      final FillStyle fillStyle = fillStyles.get(i);
      final StringBuilder fillScript = fillStylePathBuffers.get(i);
      final SVGPathElement path = (SVGPathElement)document.createElement("path");
      path.setAttribute("d", fillScript.toString());
      path.setAttribute("fill", stringifyFillStyle(fillStyle));
      path.setAttribute("stroke", "none");
      group.appendChild(path);
    }
    for(i = 0; i < lineStyles.size(); i++) {
      final LineStyle strokeStyle = lineStyles.get(i);
      final StringBuilder strokeScript = lineStylePathBuffers.get(i);
      final SVGPathElement path = (SVGPathElement)document.createElement("path");
      path.setAttribute("d", strokeScript.toString());
      path.setAttribute("fill", "none");
      final String[] strokeStyleStrings = stringifyLineStyle(strokeStyle);
      path.setAttribute("stroke", strokeStyleStrings[0]);
      path.setAttribute("stroke-width", strokeStyleStrings[1]);
      group.appendChild(path);
    }

    final SVGElement svgel = (SVGElement)document.querySelector("svg:last-of-type").cloneNode(false);
    final Bounds b = dt.getBounds();
    svgel.setAttribute(
      "viewBox",
      String.format("%d %d %d %d", b.getMinX(), b.getMinY(), b.getMaxX() - b.getMinX(), b.getMaxY() - b.getMinY())
    );
    document.querySelector("body").appendChild(svgel);
    svgel.setInnerHTML(
      group.getOuterHTML()
    );
  }
}

class DefineJPEGImageHandler implements MovieTagHandler {
  public static Map<Integer, String> blobsMap = new HashMap<Integer, String>(); 

  public void doThing(MovieTag tag) {
    final DefineJPEGImage dj = (DefineJPEGImage)tag;
    final HTMLDocument document = Window.current().getDocument();

    final byte[] image = dj.getImage();
    final Uint8Array uia = Uint8Array.create(image.length - 4);
    for(int i = 4; i < image.length; i++) {
      uia.set(i - 4, image[i]);
    }
    final JSArray jsa = JSArray.create();
    jsa.push(uia);
    final String blobURL = JSBlob.create(jsa).getURL();
    System.out.println("Adding a bitmap to map: " + dj.getIdentifier() + " " + blobURL);
    blobsMap.put(dj.getIdentifier(), blobURL);
    // HTMLImageElement el = (HTMLImageElement)document.createElement("img");
    // el.setAttribute("src", blobURL);
    // el.setAttribute("data-id", Integer.toString(dj.getIdentifier()));
  }
}

class DefineJPEGImage2Handler implements MovieTagHandler {

  public void doThing(MovieTag tag) {
    final DefineJPEGImage2 dj = (DefineJPEGImage2)tag;
    final HTMLDocument document = Window.current().getDocument();

    final byte[] image = dj.getImage();
    final Uint8Array uia = Uint8Array.create(image.length);
    for(int i = 0; i < image.length; i++) {
      uia.set(i, image[i]);
    }
    final JSArray jsa = JSArray.create();
    jsa.push(uia);
    final String blobURL = JSBlob.create(jsa).getURL();
    System.out.println("Adding a bitmap to map: " + dj.getIdentifier() + " " + blobURL);
    DefineJPEGImageHandler.blobsMap.put(dj.getIdentifier(), blobURL);
    // HTMLImageElement el = (HTMLImageElement)document.createElement("img");
    // el.setAttribute("src", blobURL);
    // el.setAttribute("data-id", Integer.toString(dj.getIdentifier()));
  }
}

class MovieHeaderHandler implements MovieTagHandler {
  public static Bounds frameBounds = null;
  public static int originX = 0;
  public static int originY = 0;

  public void doThing(MovieTag tag) {
    MovieHeader header = (MovieHeader) tag;
    frameBounds = header.getFrameSize();
    // TODO when this really becomes a placement frame, use these values:
    // originX = -frameBounds.getMinX();
    // originY = -frameBounds.getMinY();
    originX = (frameBounds.getMaxX() - frameBounds.getMinX()) / 2;
    originY = (frameBounds.getMaxY() - frameBounds.getMinY()) / 2;
    HTMLDocument document = Window.current().getDocument();
    document.querySelector("svg").setAttribute(
      "viewBox",
      "0 0 " + Integer.toString(frameBounds.getMaxX() - frameBounds.getMinX()) + " " + Integer.toString(frameBounds.getMaxX() - frameBounds.getMinX())
    );
  }
}


public class TypeHandlers {
  public static Map<Class, Class<? extends MovieTagHandler>> typeMap = new HashMap<Class, Class<? extends MovieTagHandler>>();

  static {
    typeMap.put(DefineShape.class, DefineShapeHandler.class);
    typeMap.put(MovieHeader.class, MovieHeaderHandler.class);
    typeMap.put(DefineJPEGImage.class, DefineJPEGImageHandler.class);
    typeMap.put(DefineJPEGImage2.class, DefineJPEGImage2Handler.class);
  }
}