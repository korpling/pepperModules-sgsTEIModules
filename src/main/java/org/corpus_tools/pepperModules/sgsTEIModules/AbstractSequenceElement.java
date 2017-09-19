package org.corpus_tools.pepperModules.sgsTEIModules;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractSequenceElement implements SequenceElement{
	private String value;
	private HashMap<String, String> annotations;
	private SequenceElement overlaps;
	private String id;
	
	public AbstractSequenceElement (){
		annotations = new HashMap<String, String>();
	}
	
	public String getValue() {
		return this.value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public Map<String, String> getAnnotations() {
		return this.annotations;
	}

	@Override
	public void addAnnotation(String key, String val) {
		annotations.put(key, val);
	}
	
	@Override
	public SequenceElement getOverlap() {
		return overlaps;
	}
	
	@Override
	public void setOverlap(SequenceElement elem) {
		this.overlaps = elem;
	}
	
	@Override
	public String toString() {		
		String[] elements = new String[overlaps == null? 2 : 4];
		elements[0] = getElementType().toString();
		elements[1] = getValue();
		if (overlaps != null) {
			elements[2] = overlaps.getElementType().toString();
			elements[3] = overlaps.getValue();
		}
		return String.join(":", elements);
	}
	
	@Override
	public String getId() {
		return this.id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
}
