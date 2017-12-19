package org.corpus_tools.pepperModules.sgsTEIModules.builders;

import java.util.ArrayList;
import java.util.List;

import org.corpus_tools.pepperModules.sgsTEIModules.builders.time.TokenManager;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
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
	private int lastTextEnd;
	
	protected Segmentation(String name, String delimiter) {
		this.segments = new ArrayList<>();
		this.name = name;
		this.delimiter = delimiter;
		this.lastTextEnd = 0;
	}
	
	public void addSegment(String id, String text) {
		segments.add( new Segment(id, text, lastTextEnd, lastTextEnd + text.length()) );
		lastTextEnd += text.length() + delimiter.length();
	}
	
	public void build(SDocumentGraph graph) {
		STextualDS ds = graph.createTextualDS( getText() );
		ds.setName( getName() );
		alignTokens(graph, ds);
	}
	
	private void alignTokens(SDocumentGraph graph, STextualDS ds) {
		List<SToken> sTokens = new ArrayList<>();
		for (Segment segment : segments) {
			sTokens.add( alignToken(graph, ds, segment) );
		}
		addOrderRelations(graph, sTokens);
	}
	
	private SToken alignToken(SDocumentGraph graph, STextualDS ds, Segment segment) {
		SToken sToken = createSToken(segment);
		graph.addNode(sToken);
		STextualRelation rel = (STextualRelation) graph.createRelation(sToken, ds, SALT_TYPE.STEXTUAL_RELATION, null); 
		rel.setStart(segment.getStart());
		rel.setEnd(segment.getEnd());
		return sToken;
	}
	
	private SToken createSToken(Segment segment) {
		SToken sToken = SaltFactory.createSToken();
		if (segment.getId() != null) {
			sToken.setId(segment.getId());
		}		
		return sToken;
	}
	
	private void addOrderRelations(SDocumentGraph graph, List<SToken> sTokens) {
		for (int i = 1; i < sTokens.size(); i++) {
			graph.createRelation(sTokens.get(i - 1), sTokens.get(i), SALT_TYPE.SORDER_RELATION, null).setType(getName());
		}
	}
	
	public String getName() {
		return name;
	}

	private String getText() {
		List<String> tokens = new ArrayList<>();
		for (Segment segment : segments) {
			tokens.add(segment.getText());
		}
		return String.join(delimiter, tokens);
	}
	
	private class Segment {
		private String id;
		private String text;
		private int start;
		private int end;
		
		private Segment(String id, String text, int startIndex, int endIndex) {
			this.id = id;
			this.text = text;
			this.start = startIndex;
			this.end = endIndex;
		}
		
		public String getText() {
			return text;
		}
		
		public int getStart() {
			return start;
		}
		
		public int getEnd() {
			return end;
		}
		
		public String getId() {
			return id;
		}
	}
}
