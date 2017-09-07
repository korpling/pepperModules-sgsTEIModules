package org.corpus_tools.pepperModules.sgsTEIModules.lib;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public abstract class AbstractSequenceElement implements SequenceElement{
	private String value;
	private List<Pair<String, String>> annotations;
	private SequenceElement overlaps;
		
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
	public void addAnnotation(String key, String value) {
		annotations.add(Pair.of(key, value));
	}
	
	@Override
	public void setOverlap(SequenceElement elem) {
		this.overlaps = elem;
	}
}
