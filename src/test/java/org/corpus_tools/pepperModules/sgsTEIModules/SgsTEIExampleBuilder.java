package org.corpus_tools.pepperModules.sgsTEIModules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SOrderRelation;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This singleton class provides salt example graphs to compare conversion results against. 
 * @author klotzmaz
 *
 */
public class SgsTEIExampleBuilder implements SgsTEIExample, SaltExampleConstants{
	private static final String F_ERR_READ_XML = "An error occured reading %s.";
	private final Logger logger = LoggerFactory.getLogger(SgsTEIExampleBuilder.class);
	private static SgsTEIExampleBuilder instance;
	private final Map<Class<?>, String> FILE_NAMES;
	private SDocumentGraph salt;
	private String xml;
	
	private SgsTEIExampleBuilder() {
		FILE_NAMES = new HashMap<>();
		FILE_NAMES.put(String.class, "syntax-and-ref-12-11.xml");
		instance = this;
	}
	
	public static SgsTEIExampleBuilder getInstance() {
		if (instance == null) {
			instance = new SgsTEIExampleBuilder();
		}
		return instance;
	}

	@Override
	public SDocumentGraph getSaltGraph() {
		if (salt == null) {
			salt = SaltFactory.createSDocumentGraph();
			SaltFactory.createSDocument().setDocumentGraph(salt);			
			createSaltGraph();		
		}
		return salt;
	}
	
	private void createSaltGraph() {
		SDocumentGraph graph = salt;
		STextualDS ds = createTextualDS(graph, SPEAKER_JER, DIPL, DIPL_JER);
		createTokens(ds, DIPL_JER_INDICES, null);
		
		ds = createTextualDS(graph, SPEAKER_JER, NORM, NORM_JER);
		createTokens(ds, NORM_JER_INDICES, null);
		
		ds = createTextualDS(graph, SPEAKER_JER, SYN, SYN_JER);
		createTokens(ds, SYN_JER_INDICES, MORPH_JER);
		
		ds = createTextualDS(graph, SPEAKER_S92, DIPL, DIPL_S92);
		createTokens(ds, DIPL_S92_INDICES, null);
		
		ds = createTextualDS(graph, SPEAKER_S92, NORM, NORM_S92);
		createTokens(ds, NORM_S92_INDICES, null);
		
		ds = createTextualDS(graph, SPEAKER_S92, PAUSE, PAUSE_S92);
		createTokens(ds, PAUSE_S92_INDICES, null);
		
		ds = createTextualDS(graph, SPEAKER_S92, SYN, SYN_S92);
		createTokens(ds, SYN_S92_INDICES, MORPH_S92);
	}
	
	private STextualDS createTextualDS(SDocumentGraph graph, String speaker, String name, String text) {
		STextualDS ds = graph.createTextualDS(text);
		ds.setName( String.join("_", speaker, name) );
		return ds;
	}
	
	private void createTokens(STextualDS ds, int[][] indices, String[][] annotations) {
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
				for (int j = 0; j < MORPH_NAMES.length; j++) {
					sToken.createAnnotation(null, MORPH_NAMES[j], annotations[i][j]);					
				}
			}
			tokens.add(sToken);
		}
		for (int i = 1; i < tokens.size(); i++) {
			createOrderRelation(tokens.get(i - 1), tokens.get(i), ds.getName());
		}
	}
	
	private void createTimelineRelation(STimeline timeline, SToken sToken, int start, int end) {
		timeline.increasePointOfTime(end - start);
		STimelineRelation rel = SaltFactory.createSTimelineRelation();
		rel.setStart(start);
		rel.setEnd(end);
		rel.setSource(sToken);
		rel.setTarget(timeline);
		rel.setGraph( sToken.getGraph() );
	}
	
	private void createOrderRelation(SToken from, SToken to, String name) {
		SOrderRelation oRel = SaltFactory.createSOrderRelation();
		oRel.setSource(from);
		oRel.setTarget(to);
		oRel.setType(name);
		oRel.setGraph( from.getGraph() );
	}

	@Override
	public String getXML() {
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
	public Map<Class<?>, String> getFileNames() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}
