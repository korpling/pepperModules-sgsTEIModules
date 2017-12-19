package org.corpus_tools.pepperModules.sgsTEIModules.builders;

import java.util.ArrayList;
import java.util.List;

import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SOrderRelation;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STextualRelation;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SLayer;

public class Segmentation {
	private List<Segment> segments;
	private String name; 
	private String delimiter;
	private STimeline timeline;
	private int lastTextEnd;
	private int lastTimeEnd;
	
	protected Segmentation(STimeline timeline, String name, String delimiter) {
		this.timeline = timeline;
		this.segments = new ArrayList<>();
		this.name = name;
		this.delimiter = delimiter;
		this.lastTextEnd = 0;
		this.lastTimeEnd = 0;
	}
	
	protected SLayer build() {
		SLayer partialGraph = SaltFactory.createSLayer();
		STextualDS ds = SaltFactory.createSTextualDS();
		ds.setText( getText() );
		ds.setName( getName() );
		alignSegments(partialGraph, ds);		
		return partialGraph;
	}
	
	protected void addElement(String id, String text, int emptySlotsBefore, int timeSpan) {
		segments.add( new Segment(id, text, lastTextEnd, lastTextEnd + text.length(), emptySlotsBefore + lastTimeEnd, emptySlotsBefore + lastTimeEnd + timeSpan) );
		lastTextEnd += text.length() + delimiter.length();
		move(emptySlotsBefore + timeSpan);
	}
	
	protected void addElement(String id, String text) {
		addElement(id, text, 0, 1);
	}
	
	protected void move(int moveBy) {
		lastTimeEnd += moveBy;
	}
	
	private void alignSegments(SLayer partialGraph, STextualDS targetDS) {		
		List<SToken> tokens = new ArrayList<>();
		for (Segment segment : getSegments()) {
			tokens.add( alignSegment(segment, partialGraph, targetDS) );
		}
		addOrderRelations(tokens, partialGraph);
	}
	
	private SToken alignSegment(Segment segment, SLayer partialGraph, STextualDS targetDS) {
		SToken sToken = SaltFactory.createSToken();
		sToken.setId( segment.getId() );
		segment.getTextualRelation().setSource(sToken);
		segment.getTextualRelation().setTarget(targetDS);
		segment.getTimelineRelation().setSource(sToken);
		segment.getTimelineRelation().setTarget( getTimeline() );
		partialGraph.addNode(sToken);
		partialGraph.addRelation(segment.getTextualRelation());
		partialGraph.addRelation(segment.getTimelineRelation());		
		return sToken;
	}
	
	private void addOrderRelations(List<SToken> tokens, SLayer partialGraph) {
		SOrderRelation rel = null;
		for (int i = 0; i < tokens.size() - 1; i++) {
			rel = SaltFactory.createSOrderRelation();
			rel.setSource(tokens.get(i));
			rel.setTarget(tokens.get(i + 1));
			rel.setType( getName() );
			partialGraph.addRelation(rel);
		}
	}
	
	private STimeline getTimeline() {
		return this.timeline;
	}
	
	protected String getName() {
		return this.name;
	}
	
	private String getText() {
		List<String> textSegments = new ArrayList<>();
		for (Segment segment : getSegments()) {
			textSegments.add( segment.getText() );
		}
		return String.join(getDelimiter(), textSegments);
	}
	
	private String getDelimiter() {
		return this.delimiter;
	}
	
	private List<Segment> getSegments() {
		return this.segments;
	}
	
	private class Segment {
		private String id;
		private String text;
		private STextualRelation textualRelation;
		private STimelineRelation timelineRelation;
		
		private Segment(String id, String text, int startIndex, int endIndex, int startTime, int endTime) {
			this.id = id;
			this.text = text;
			textualRelation = SaltFactory.createSTextualRelation();
			textualRelation.setStart(startIndex);
			textualRelation.setEnd(endIndex);
			timelineRelation = SaltFactory.createSTimelineRelation();
			timelineRelation.setStart(startTime);
			timelineRelation.setEnd(endTime);
		}
		
		private String getId() {
			return this.id;
		}
		
		private String getText() {
			return text;
		}
		
		private STextualRelation getTextualRelation() {
			return textualRelation;
		}
		
		private STimelineRelation getTimelineRelation() {
			return timelineRelation;
		}
	}
}
