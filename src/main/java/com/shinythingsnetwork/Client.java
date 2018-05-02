package com.shinythingsnetwork;
import com.flagstone.transform.Movie;
import com.flagstone.transform.MovieTag;

import com.flagstone.transform.shape.*;

import com.flagstone.transform.sound.SoundStreamHead;
import com.flagstone.transform.sound.SoundStreamBlock;
// sound formats: ADPCM,MP3,NATIVE_PCM,NELLYMOSER,NELLYMOSER_8K,PCM,SPEEX
import com.flagstone.transform.sound.SoundFormat;

import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.typedarrays.Uint8Array;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSBody;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.net.URL;
import java.io.ByteArrayOutputStream;

 abstract class JSBlob implements JSObject {
      @JSBody(params = {}, script = "return this.length;")
     public native int getLength();

      @JSBody(params = {}, script = "return this.size;")
      public native int getSize();

      @JSBody(params = {}, script = "return this.type;")
      public native String getType();

      @JSBody(params = { "start" },
             script = "return this.slice(start);")
      public native JSBlob slice(int start);

      @JSBody(params = { "start", "end" },
             script = "return this.slice(start, end);")
      public native JSBlob slice(int start, int end);

      @JSBody(params = {}, script = "return URL.createObjectURL(this);")
      public native String getURL();

      @JSBody(params = "parts", script = "return new Blob(parts);")
      public static native JSBlob create(JSArray parts);

      @JSBody(params = { "parts", "options" }, script = "return new Blob(parts, options);")
      public static native JSBlob create(JSArray parts, JSObject options);
 }

 abstract class JSMap implements JSObject {
      @JSBody(params = { "key" },
             script = "return this.get(key);")
      public native JSObject get(String key);

      @JSBody(params = { "key" },
             script = "return this.has(key);")
      public native boolean has(String key);

      @JSBody(params = { "key", "value" },
             script = "return this.set(key, value);")
      public native void set(String key, JSObject value);

      @JSBody(params = {}, script = "return new Map();")
      public static native JSMap create();
 }

abstract class GlobalObjects implements JSObject {
  @JSBody(params = { "key", "value" }, script = "window[key] = value")
  public static native void set(String key, JSObject value);

  @JSBody(params = "key", script = "return window[key]")
  public static native JSObject get(String key);
}


class Client {

public static void main(String[] args) {
  System.out.println("Hello");
  final Movie movie = new Movie();
  final HTMLDocument document = Window.current().getDocument();
  final JSArray<Uint8Array> audioTarget = JSArray.create();
  final JSMap frameTypes = JSMap.create();
  try {

      movie.decodeFromUrl(new URL("http://localhost/Projects/transform-swf/transform-swf-teavm/target/transform-swf-teavm-1.0-SNAPSHOT/sbemail118.swf"));

      List<MovieTag> objects = movie.getObjects();
      for(MovieTag obj: objects) {
        if(obj instanceof SoundStreamHead) {

          SoundStreamHead ssh = (SoundStreamHead)obj;
          System.out.println("SoundStreamHead format: " + ssh.getFormat().toString());
          HTMLElement el = document.createElement("audio");
          String type;
          switch (ssh.getFormat()) {
            case MP3:
              type="mpeg";
              break;
            case SPEEX:
              type="ogg";
              break;
            default:
              type="wav";
          }
          el.setAttribute("type", "audio/" + type);
          document.getBody().appendChild(el);
        }
        if(obj instanceof SoundStreamBlock) {
          final byte[] bytes = ((SoundStreamBlock)obj).getSound();
          // the first two bytes of the sound are the number of sample frames contained in it (litte-endian).
          // the second two bytes are the frame(?) seek of the samples (little-endian)
          // The rest of the data is MPEG frames.
          final Uint8Array jsa = Uint8Array.create(bytes.length - 4);
          for(int i = 4; i < bytes.length; i++) {
            jsa.set(i - 4, bytes[i]);
          }
          audioTarget.push(jsa);
        }
        // if(obj instanceof DefineShape) {
        //   System.out.println("Found DefineShape");
        //   final DefineShape dt = (DefineShape)obj;
        //   // for(Object record : dt.getFillStyles()) {
        //   //   System.out.println(String.format("fillStyle found: %s, index %d", record, ((FillStyle)record).getIndex()));
        //   // }
        //   // for(Object record : dt.getLineStyles()) {
        //   //   System.out.println(String.format("lineStyle found: %s, index %d", record, ((LineStyle)record).getIndex()));
        //   // }
        //   List<ShapeRecord> records = ((DefineShape)obj).getShape().getObjects();
        //   for(ShapeRecord record: records) {
        //     if(record instanceof ShapeStyle) {
        //       ShapeStyle styleRecord = (ShapeStyle) record;
        //       System.out.println("ShapeStyle found");
        //       if (styleRecord.getMoveX() != null && styleRecord.getMoveY() != null) 
        //         System.out.println(String.format("Move %d %d", styleRecord.getMoveX(), styleRecord.getMoveY()));
        //       if (styleRecord.getFillStyle() != null) 
        //         System.out.println(String.format("Fill style %d", styleRecord.getFillStyle()));
        //       if (styleRecord.getAltFillStyle() != null) 
        //         System.out.println(String.format("Alt fill style %d", styleRecord.getAltFillStyle()));
        //       if (styleRecord.getLineStyle() != null) 
        //         System.out.println(String.format("Line style %d", styleRecord.getLineStyle()));
        //       if (styleRecord.getFillStyles() != null) 
        //         System.out.println(String.format("Fill styles %s", styleRecord.getFillStyles()));
        //       if (styleRecord.getFillStyles() != null) 
        //         System.out.println(String.format("Line styles %s", styleRecord.getFillStyles()));
        //     }
        //   }
        // }
        if (TypeHandlers.typeMap.containsKey(obj.getClass())) {
          TypeHandlers.typeMap.get(obj.getClass()).newInstance().doThing(obj);
        }

        JSNumber count;
        if(frameTypes.has(obj.getClass().getName())) {
          count = (JSNumber)frameTypes.get(obj.getClass().getName());
        } else {
          count = JSNumber.valueOf(0);
        }
        frameTypes.set(obj.getClass().getName(), JSNumber.valueOf(count.intValue() + 1));

      }
      document.querySelector("audio").setAttribute("src", JSBlob.create(audioTarget).getURL());
      GlobalObjects.set("frameTypes", frameTypes);
  } catch(Exception e) {
      System.err.println(e);
  }
}

} 
