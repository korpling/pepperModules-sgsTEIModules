package org.corpus_tools.pepperModules.sgsTEIModules;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public abstract class AbstractSequenceElement implements SequenceElement{
	private String value;
	private List<Pair<String, String>> annotations;
	private SequenceElement overlaps;
	
	public AbstractSequenceElement (){
		annotations = new ArrayList<>();
	}
	
	public String getValue() {
		return this.value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public List<Pair<String, String>> getAnnotations() {
		return this.annotations;
	}

	@Override
	public void addAnnotation(String key, String val) {
		annotations.add(Pair.of(key, val));
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
}
