/**
 * Copyright 2016 University of Cologne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */
package org.corpus_tools.pepperModules.sgsTEIModules.builders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.SToken;

public class Segmentation {
	private List<Segment> segments;
	private Map<String, Segment> id2Segment;
	private String name; 
	private String delimiter;
	private Evaluator evaluator;
	private STextualDS ds;
	private Map<String, int[]> indices;
	
	protected Segmentation(String name, String delimiter) {
		this.segments = new ArrayList<>();
		this.name = name;
		this.delimiter = delimiter;
		this.id2Segment = new HashMap<>();
	}
	
	public void setEvaluator(Evaluator evaluator) {
		this.evaluator = evaluator;
	}

	public List<String> getSequence() {
		List<String> ids = new ArrayList<>();
		for (Segment segment : segments) {
			ids.add( segment.getId() );
		}
		return ids;
	}
	
	public void addSegment(String id) {
		segments.add( new Segment(id) );
		id2Segment.put(id, segments.get( segments.size() - 1 ));
	}
	
	public SToken getSToken(String tokenId) {
		Segment segment = id2Segment.get(tokenId);
		SToken sToken = SaltFactory.createSToken();
		if (segment.getId() != null) {
			sToken.setId(segment.getId());
		}		
		return sToken;
	}
	
	public int[] getIndices(String tokenId) {
		if (indices == null) {
			computeIndices();
		}
		return indices.get(tokenId);
	}
	
	private void computeIndices() {
		int p = 0;
		indices = new HashMap<>();
		for (Segment segment : segments) {
			String txt = segment.getText();
			indices.put(segment.getId(), new int[] {p, p + txt.length()});
			p += txt.length() + getDelimiter().length();
		}
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	private String getDelimiter() {
		return delimiter;
	}

	private String getText() {
		List<String> tokens = new ArrayList<>();
		for (Segment segment : segments) {
			tokens.add( segment.getText() );
		}
		return String.join(delimiter, tokens);
	}
	
	public STextualDS getDS(SDocumentGraph graph) {
		if (ds == null) {
			ds = graph.createTextualDS( getText() );
			ds.setName( getName() );
		} else {
			ds.setText( getText() );
		}
		return ds;
	}
	
	public int getDistance(String id0, String id1) {
		List<String> seq = getSequence();
		return seq.indexOf(id1) - seq.indexOf(id0);
	}
	
	protected Evaluator getEvaluator() {
		return this.evaluator;
	}
	
	private class Segment {
		private String id;
		
		private Segment(String id) {
			this.id = id;
		}
		
		public String getText() {
			return getEvaluator().evaluate( getId() );
		}
		
		public String getId() {
			return id;
		}
	}
	
	public interface Evaluator {
		public String evaluate(String tokenId);
	}
}
