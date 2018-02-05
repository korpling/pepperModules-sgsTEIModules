package org.corpus_tools.pepperModules.sgsTEIModules.examples;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SOrderRelation;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STextualRelation;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.util.DataSourceSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This singleton class provides salt example graphs to compare conversion results against. 
 * @author klotzmaz
 *
 */
public abstract class AbstractSgsTEIExample implements SgsTEIExample, SaltExampleConstants{
	private static final String F_ERR_READ_XML = "An error occured reading %s.";
	private final Logger logger = LoggerFactory.getLogger(AbstractSgsTEIExample.class);
	private final Map<Class<?>, String> FILE_NAMES;
	private SDocumentGraph salt;
	private String xml;
		
	protected AbstractSgsTEIExample(String xmlExampleFile, String saltExampleFile) {
		FILE_NAMES = new HashMap<>();
		FILE_NAMES.put(String.class, xmlExampleFile);
		FILE_NAMES.put(SDocumentGraph.class, saltExampleFile);
	}

	@Override
	public final SDocumentGraph getSaltGraph() {
		if (salt == null) {
			salt = SaltFactory.createSDocumentGraph();
			SDocument document = SaltFactory.createSDocument();
			document.setId("example");
			salt.setId("exampleGraph");
			salt.setDocument(document);
			salt.createTimeline().increasePointOfTime();
			createSaltGraph();		
		}
		return salt;
	}
	
	protected void createSaltGraph() {
		SDocumentGraph graph = salt;
		STextualDS ds = createTextualDS(graph, SPEAKER_JER, DIPL, DIPL_JER);
		createTokens(ds, DIPL_JER_INDICES, null, null);		
		
		ds = createTextualDS(graph, SPEAKER_JER, NORM, NORM_JER);
		createTokens(ds, NORM_JER_INDICES, null, null);
		
		ds = createTextualDS(graph, SPEAKER_JER, SYN, SYN_JER);
		createTokens(ds, SYN_JER_INDICES, MORPH_JER, MORPH_NAMES);
		
		ds = createTextualDS(graph, SPEAKER_JER, UTT, UTT_JER);
		createTokens(ds, UTT_JER_INDICES, UTT_ANNO_JER, UTT_NAMES);
		
		ds = createTextualDS(graph, SPEAKER_S92, DIPL, DIPL_S92);
		createTokens(ds, DIPL_S92_INDICES, null, null);
		
		ds = createTextualDS(graph, SPEAKER_S92, NORM, NORM_S92);
		createTokens(ds, NORM_S92_INDICES, null, null);
		
		ds = createTextualDS(graph, SPEAKER_S92, PAUSE, PAUSE_S92);
		createTokens(ds, PAUSE_S92_INDICES, null, null);
		
		ds = createTextualDS(graph, SPEAKER_S92, SYN, SYN_S92);
		createTokens(ds, SYN_S92_INDICES, MORPH_S92, MORPH_NAMES);
		
		ds = createTextualDS(graph, SPEAKER_S92, UTT, UTT_S92);
		createTokens(ds, UTT_S92_INDICES, UTT_ANNO_S92, UTT_NAMES);
		
		createSpans(graph);
	}
	
	private final void createSpans(SDocumentGraph graph) {
		STimeline timeline = graph.getTimeline();
		for (int i = 0; i < SPANS.length; i++) {
			int[][] spanGroup = SPANS[i];
			String annoName = SPAN_NAMES[i];
			String[] annoValues = SPAN_VALUES[i];
			for (int j = 0; j < spanGroup.length; j++) {
				int[] span = spanGroup[j];
				List<SToken> spanTokens = graph.getTokensBySequence( new DataSourceSequence<Number>(timeline, span[0], span[1]) );
				List<SToken> filteredTokens = getFilteredTokens(spanTokens);
				SSpan sSpan = graph.createSpan( filteredTokens );
				createAnnotation(sSpan, annoName, annoValues[j]);
				Function<SToken, String> f = new Function<SToken, String>() {					
					@Override
					public String apply(SToken t) {
						return getSaltGraph().getText(t);
					}
				};
				Function<SToken, Boolean> member = new Function<SToken, Boolean>() {
					@Override
					public Boolean apply(SToken t) {
						return getSaltGraph().containsNode(t.getId());
					}					
				};
			}
		}
	}
	
	private final List<SToken> getFilteredTokens(List<SToken> unfiltered) {
		return unfiltered.stream().filter(filterTokenByType("dipl")).collect(Collectors.<SToken>toList());
	}
	
	protected static final Predicate<SToken> filterTokenByType(final String typeName) {
		return new Predicate<SToken>() {		
			@Override
			public boolean test(SToken t) {
				SRelation rel = t.getOutRelations().stream().filter(IS_TEXTUAL_RELATION).findFirst().get();
				return ((STextualDS) rel.getTarget()).getName().endsWith(typeName);
			}
		};
	}
	
	public static final Predicate<SRelation> IS_TEXTUAL_RELATION = new Predicate<SRelation>() {
		@Override
		public boolean test(SRelation rel) {
			return rel instanceof STextualRelation;
		}
	};
	
	private final STextualDS createTextualDS(SDocumentGraph graph, String speaker, String name, String text) {
		STextualDS ds = graph.createTextualDS(text);
		ds.setName( String.join("_", speaker, name) );
		return ds;
	}
	
	private final void createTokens(STextualDS ds, int[][] indices, String[][] annotations, String[] annotationNames) {
		SDocumentGraph graph = ds.getGraph();
		STimeline timeline = graph.getTimeline();
		SToken sToken = null;
		boolean hasAnnotations = annotations != null;
		List<SToken> tokens = new ArrayList<>();
		for (int i = 0; i < indices.length; i++) {
			int[] tuple = indices[i];
			sToken = graph.createToken(ds, tuple[0], tuple[1]);
			createTimelineRelation(timeline, sToken, tuple[2], tuple[3]);
			if (hasAnnotations && annotations[i] != null) {
				for (int j = 0; j < annotationNames.length; j++) {
					if (annotations[i][j] != null) {
						createAnnotation(sToken, annotationNames[j], annotations[i][j]);					
					}
				}
			}
			tokens.add(sToken);
		}
		for (int i = 1; i < tokens.size(); i++) {
			createOrderRelation(tokens.get(i - 1), tokens.get(i), ds.getName());
		}
	}
	
	private final void createAnnotation(SNode sNode, String name, String value) {
		sNode.createAnnotation(null, name, value);
	}

	private final void createTimelineRelation(STimeline timeline, SToken sToken, int start, int end) {
		timeline.increasePointOfTime(end - start);
		STimelineRelation rel = SaltFactory.createSTimelineRelation();
		rel.setStart(start);
		rel.setEnd(end);
		rel.setSource(sToken);
		rel.setTarget(timeline);
		rel.setGraph( sToken.getGraph() );
	}
	
	private final void createOrderRelation(SToken from, SToken to, String name) {
		SOrderRelation oRel = SaltFactory.createSOrderRelation();
		oRel.setSource(from);
		oRel.setTarget(to);
		oRel.setType(name);
		oRel.setGraph( from.getGraph() );
	}

	@Override
	public final String getXML() {
		if (this.xml == null) {
			StringBuilder xmlBuilder = new StringBuilder();		
			String path = getFileNames().get(String.class);
			try {
				for (Object line : Files.lines( Paths.get(path)).toArray() ) {
					xmlBuilder.append(line.toString());
				}
			} catch (IOException e) {
				logger.error(String.format(F_ERR_READ_XML, path));
				e.printStackTrace();
				return null;
			}
			this.xml = xmlBuilder.toString();
		}
		return this.xml;
	}

	@Override
	public final Map<Class<?>, String> getFileNames() {
		return FILE_NAMES;
	}
}
