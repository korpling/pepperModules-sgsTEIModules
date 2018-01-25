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
//					graph.createRelation(refexSpans.get(k), refexSpans.get(k - 1), SALT_TYPE.SPOINTING_RELATION, null).setType("coreference");
				}
			}
		}
		for (int i = 0; i < REF_LINKS.length; i++) {
//			graph.createRelation(refexSpans.get(REF_LINKS[i][1]), refexSpans.get(REF_LINKS[i][0]), SALT_TYPE.SPOINTING_RELATION, String.join("=", "type", REF_LINK_TYPES[i]));
//			refexSpans.get(REF_LINKS[i][0]).createAnnotation(null, "given", "1"); //FIXME: hard-coded
		}
	}
}
