package org.corpus_tools.pepperModules.sgsTEIModules;

import java.util.ArrayList;
import java.util.List;

public class LinguisticParent extends AbstractSequenceElement{
	private String speaker;
	private long start;
	private long end;
	private List<SequenceElement> elements;
	
	public LinguisticParent(String speaker, long start, long end) {
		this.speaker = speaker;
		this.start = start;
		this.end = end;
		this.elements = new ArrayList<SequenceElement>();
	}
	
	public String getSpeaker() {
		return speaker;
	}
	
	public void setSpeaker(String speaker) {
		this.speaker = speaker;
	}
	
	public long getStart() {
		return start;
	}
	
	public void setStart(long start) {
		this.start = start;
	}
	
	public long getEnd() {
		return end;
	}
	
	public void setEnd(long end) {
		this.end = end;
	}
	
	public void addElement(SequenceElement elem) {
		this.elements.add(elem);
	}

	@Override
	public ElementType getElementType() {
		return ElementType.PARENT;
	}

	@Override
	public String getValue() {
		return null;
	}

	@Override
	public List<SequenceElement> getElements() {
		return elements;
	}
	
	@Override
	public String toString() {
		return String.join(":", super.toString(), Long.toString(getStart()), Long.toString(getEnd()));
	}
}
