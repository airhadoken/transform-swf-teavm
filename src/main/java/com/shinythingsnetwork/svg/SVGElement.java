package com.shinythingsnetwork.svg;

import org.teavm.jso.dom.xml.Element;
import org.teavm.jso.JSProperty;
import org.teavm.jso.JSObject;

import java.util.Map;

interface DOMStringMap extends JSObject, Map {}

public interface SVGElement extends Element {

  @JSProperty
  String getOuterHTML();

  @JSProperty
  String getInnerHTML();

  @JSProperty
  void setInnerHTML(String innerHTML);

  @JSProperty
  DOMStringMap getDataset();

  @JSProperty
  String getId();

  @JSProperty
  String getXmlbase();

  @JSProperty
  SVGElement getOwnerSVGElement();

  @JSProperty
  SVGElement getViewportElement();
}