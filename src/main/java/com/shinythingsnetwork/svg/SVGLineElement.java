package com.shinythingsnetwork.svg;

import org.teavm.jso.JSProperty;

public interface SVGLineElement extends SVGGeometryElement {
  @JSProperty
  int getX1();  

  @JSProperty
  int getY1();  

  @JSProperty
  int getX2();  

  @JSProperty
  int getY2();  
}