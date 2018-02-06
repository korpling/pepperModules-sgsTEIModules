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
package org.corpus_tools.pepperModules.sgsTEIModules.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.util.DataSourceSequence;

public class ReferenceSgsTEIExample extends SyntaxSgsTEIExample {
	private static final String XML_EXAMPLE_FILE = "example_morphology_syntax_reference.xml";	
	protected ReferenceSgsTEIExample(String xmlExampleFile, String saltExampleFile) {
		super(xmlExampleFile, saltExampleFile);
	}
	
	public ReferenceSgsTEIExample() {
		this(XML_EXAMPLE_FILE, null);
	}
	
	@Override
	protected void createSaltGraph() {
		super.createSaltGraph();
		createReference();
	}
	
	private void createReference() {
		SDocumentGraph graph = getSaltGraph();
		STimeline timeline = graph.getTimeline();
		List<SSpan> refexSpans = new ArrayList<>();
		for (int[] spanCoords : REFEX_SPANS) {
			List<SToken> spanTokens = graph.getTokensBySequence( new DataSourceSequence<Number>(timeline, spanCoords[0], spanCoords[1]) )
					.stream().filter(IS_SYNTACTIC_TOKEN).collect(Collectors.<SToken>toList());
			refexSpans.add( graph.createSpan(spanTokens) );
		}
		for (int i = 0; i < DISCOURSE_ENTITIES.length; i++) {
			for (int k = 0; k < DISCOURSE_ENTITIES[i].length; k++) {
				int ix = DISCOURSE_ENTITIES[i][k];
				for (int j = 0; j < DISCOURSE_ANNO_NAMES.length; j++) {
					SSpan de = refexSpans.get(ix);
					if (DISCOURSE_ANNO_VALUES[i][j] != null) {
						de.createAnnotation(null, DISCOURSE_ANNO_NAMES[j], DISCOURSE_ANNO_VALUES[i][j]);
					}
				}
				if (k > 0) {
					graph.createRelation(refexSpans.get(k), refexSpans.get(k - 1), SALT_TYPE.SPOINTING_RELATION, null).setType("coreference");
					refexSpans.get(k).createAnnotation(null, "given", "1"); //FIXME -> hard-coded value
				}
			}
		}
		for (int i = 0; i < REF_LINKS.length; i++) {
			graph.createRelation(refexSpans.get(DISCOURSE_ENTITIES[REF_LINKS[i][1]][0]), refexSpans.get(DISCOURSE_ENTITIES[REF_LINKS[i][0]][0]), SALT_TYPE.SPOINTING_RELATION, String.join("=", "type", REF_LINK_TYPES[i]));			
		}		
	}
}
