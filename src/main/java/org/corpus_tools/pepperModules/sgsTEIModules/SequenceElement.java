package org.corpus_tools.pepperModules.sgsTEIModules;

import java.util.List;
import java.util.Map;

public interface SequenceElement {
	public ElementType getElementType();
	public String getValue();
	public List<SequenceElement> getElements();
	public Map<String, String> getAnnotations();
	public void addAnnotation(String key, String value);
	public void setOverlap(SequenceElement elem);
	public SequenceElement getOverlap();
	public String getId();
	public void setId(String id);
}
