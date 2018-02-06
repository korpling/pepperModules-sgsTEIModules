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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SStructure;
import org.corpus_tools.salt.common.STextualRelation;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.util.DataSourceSequence;

public class SyntaxSgsTEIExample extends MorphosyntaxSgsTEIExample {
	private static final String XML_EXAMPLE_FILE = "example_morphology_syntax.xml";		
	protected SyntaxSgsTEIExample(String xmlExampleFile, String saltExampleFile) {
		super(xmlExampleFile, saltExampleFile);
	}
	
	public SyntaxSgsTEIExample() {
		this(XML_EXAMPLE_FILE, null);
	}
	
	@Override
	protected void createSaltGraph() {
		super.createSaltGraph();
		createTrees();
	}
	
	private void createTrees() {
		for (int i = 0; i < TREES.length; i++) {
			createTree(TREES[i], TREE_LABELS[i], SPELLOUT[i]);
		}
	}
	
	private void createTree(int[][] nodeOrder, String[] labels, int[][] spellout) {
		Map<Integer, Integer> orderMap = new HashMap<>();
		for (int[] order : nodeOrder) {
			orderMap.put(order[0], order[1]);
		}
		createChildren(null, 0, orderMap, createSpelloutMap(spellout), Arrays.stream(labels).iterator());
	}
	
	private Map<Integer, SToken> createSpelloutMap(int[][] spellout) {
		Map<Integer, SToken> spoMap = new HashMap<>();
		SDocumentGraph graph = getSaltGraph();
		STimeline timeline = graph.getTimeline();
		for (int[] coords : spellout) {
			spoMap.put(coords[2], 
					graph.getTokensBySequence(new DataSourceSequence<Number>(timeline, coords[0], coords[1])).stream().filter(IS_SYNTACTIC_TOKEN).findFirst().get()
			);
		}
		return spoMap;
	}
	
	protected static final Predicate<SToken> IS_SYNTACTIC_TOKEN = new Predicate<SToken>() {
		@Override
		public boolean test(SToken t) {
			SRelation<?, ?> txtRel = t.getOutRelations().stream().filter(IS_TEXTUAL_RELATION).findFirst().get();
			return txtRel != null && ((STextualRelation) txtRel).getTarget().getName().endsWith("_syn");			
		}
	};
	

	private int createChildren(SStructure parent, int preorder, Map<Integer, Integer> orderMap, Map<Integer, SToken> spelloutMap, Iterator<String> labels) {
		SStructure structure = SaltFactory.createSStructure();
		structure.setGraph( getSaltGraph() ); 
		String[] values = labels.next().split(":");
		if (parent != null) {
			getSaltGraph().createRelation(parent, structure, SALT_TYPE.SDOMINANCE_RELATION, String.join("=", "func", values[0]));
		}
		structure.createAnnotation(null, "cat", values[1]);
		int postorder = orderMap.get(preorder);
		for (int o = preorder + 1; o < postorder; ) {
			o = createChildren(structure, o, orderMap, spelloutMap, labels) + 1;
		}
		if (spelloutMap.get(postorder) != null) {
			getSaltGraph().createRelation(structure, spelloutMap.get(postorder), SALT_TYPE.SDOMINANCE_RELATION, null);	
		}
		return orderMap.get(preorder);
	}
	
}
