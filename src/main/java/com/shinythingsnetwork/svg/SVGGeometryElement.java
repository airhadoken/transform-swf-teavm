package com.shinythingsnetwork.svg;

import org.teavm.jso.JSProperty;

public interface SVGGeometryElement extends SVGGraphicsElement {
  @JSProperty
  int getPathLength();
}