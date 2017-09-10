package org.corpus_tools.pepperModules.sgsTEIModules.lib;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public interface SequenceElement {
	public ElementType getElementType();
	public String getValue();
	public List<SequenceElement> getElements();
	public List<Pair<String, String>> getAnnotations();
	public void addAnnotation(String key, String value);
	public void setOverlap(SequenceElement elem);
	public SequenceElement getOverlap();
}
