package org.corpus_tools.pepperModules.sgsTEIModules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.corpus_tools.pepper.testFramework.PepperImporterTest;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SOrderRelation;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

/**
 * This is a dummy implementation of a JUnit test for testing the
 * {@link SgsTEIImporter} class. Feel free to adapt and enhance this test class
 * for real tests to check the work of your importer. If you are not confirm
 * with JUnit, please have a look at <a
 * href="http://www.vogella.com/tutorials/JUnit/article.html">
 * http://www.vogella.com/tutorials/JUnit/article.html</a>. <br/>
 * Please note, that the test class is derived from {@link PepperImporterTest}.
 * The usage of this class should simplfy your work and allows you to test only
 * your single importer in the Pepper environment.
 * 
 * @author Martin Klotz
 */
public class SgsTEIImporterTest implements SgsTEIDictionary {
	private static final String PRIMARY_DATA_DIPL = 
			"bueno, pues había visto subir con hombrnoes jóvenes. "
			+ "que subían mm a medianoche. "
			+ "y bajaban a altas horas de la madrugada. "
			+ "¿crees que podrían mantener alguna relación amorosa? "
			+ "bueno, yo no yo cre mira te voy a decir la verdad.";
	private static final String PRIMARY_DATA_NORM = 
			"bueno, pues había visto subir con hombretones jóvenes. "
			+ "que subían a medianoche. "
			+ "y bajaban a altas horas de la madrugada. "
			+ "¿crees que podrían mantener alguna relación amorosa? "
			+ "bueno, yo no yo creo mira te voy a decir la verdad.";
	
	private SgsTEI2SaltMapper fixture = null;
	
	/**
	 * This method is called by the JUnit environment each time before a test
	 * case starts. So each time a method annotated with @Test is called. This
	 * enables, that each method could run in its own environment being not
	 * influenced by before or after running test cases.
	 */	
	@Before
	public void setUp() {
		setFixture(new SgsTEI2SaltMapper());
	}
	
	public void setFixture(SgsTEI2SaltMapper mapper) {
		this.fixture = mapper;
		fixture.setProperties(new SgsTEIImporterProperties());
	}
	
	public SgsTEI2SaltMapper getFixture( ) {
		return this.fixture;
	}
	
	private SgsTEIImporterProperties getModuleProperties() {
		return (SgsTEIImporterProperties) getFixture().getProperties();
	}
	
	private SDocumentGraph getGoalDocumentGraph(Set<String> features) {
		SDocumentGraph graph = SaltFactory.createSDocument()
				.createDocumentGraph();
		/* primary data */
		STextualDS dipl = graph.createTextualDS(PRIMARY_DATA_DIPL);
		List<SToken> diplTokens = dipl.tokenize();
		STextualDS norm = graph.createTextualDS(PRIMARY_DATA_NORM);
		List<SToken> normTokens = norm.tokenize();
		
		//map to STimeline
		STimeline timeline = graph.createTimeline();
		STimelineRelation timeRel = null;
		List<List<SToken>> tokenSources = new ArrayList<List<SToken>>();
		tokenSources.add(diplTokens);
		tokenSources.add(normTokens);
		for (int i = 0; i < 12; i++) {
			timeline.increasePointOfTime();
			for (List<SToken> tokenSource : tokenSources) {
				timeRel = SaltFactory.createSTimelineRelation();
				timeRel.setSource(tokenSource.get(i));
				timeRel.setTarget(timeline);
				timeRel.setStart(i);
				timeRel.setEnd(i + 1);
				graph.addRelation(timeRel);
			}
		}
		// at position 12 there is "mm", so one more token on dipl than on norm
		timeline.increasePointOfTime();
		timeRel = SaltFactory.createSTimelineRelation();
		timeRel.setSource(diplTokens.get(12));
		timeRel.setTarget(timeline);
		timeRel.setStart(12);
		timeRel.setEnd(13);
		graph.addRelation(timeRel);
		for (int i = 13; i < diplTokens.size(); i++) {
			timeline.increasePointOfTime();
			List<SToken> tokenSource = null;
			for (int j=0; j < 2; j++) {
				tokenSource = tokenSources.get(j);
				timeRel = SaltFactory.createSTimelineRelation();
				timeRel.setSource(tokenSource.get(i - j));
				timeRel.setTarget(timeline);
				timeRel.setStart(i - j);
				timeRel.setEnd(i - j + 1);
				graph.addRelation(timeRel);
			}
		}
		// add order relations
		SOrderRelation orderRel = null;
		SgsTEIImporterProperties props = getModuleProperties();
		String[] names = {props.getDiplName(), props.getNormName()};
		for (int j = 0; j < 2; j++) {
			List<SToken> tokenSource = tokenSources.get(j);
			for (int i = 1; i < tokenSource.size(); i++) {
				orderRel = SaltFactory.createSOrderRelation();
				orderRel.setSource(tokenSource.get(i - 1));
				orderRel.setTarget(tokenSource.get(i));
				orderRel.setType(names[j]);
				graph.addRelation(orderRel);
			}
		}
		
		if (features == null || features.isEmpty()) {
			return graph;
		}
		
		/* morphosyntax if desired */
		if (features.contains(TYPE_MORPHOSYNTAX)) {
			// TODO
		}		
		
		return graph;
	}
	
	@Test
	public void testPrimaryData() {
		SDocumentGraph docGraph = getGoalDocumentGraph(null);
		getFixture().setResourceURI(URI.createFileURI("example.xml"));
		getFixture().mapSDocument();
		SDocumentGraph createdGraph = getFixture().getDocument().getDocumentGraph();
		
		// compare graphs
		assertEquals(docGraph.getTextualDSs().size(), createdGraph.getTextualDSs().size());
		assertEquals(docGraph.getRelations().size(), createdGraph.getTextualDSs().size());
		assertEquals(docGraph.getTokens().size(), createdGraph.getTokens().size());		
		for (int i = 0; i < docGraph.getTokens().size(); i++) {
			assertEquals(docGraph.getText(docGraph.getTokens().get(i)), createdGraph.getText(createdGraph.getTokens().get(i)));
		}		
	}
	
	@Test
	public void testMorphosyntax() {
		fail();
	}
	
	@Test
	public void testReference() {
		fail();
	}
	
	@Test
	public void testTopicality() {
		fail();
	}
	
	@Test
	public void testMetaData() {
		fail();
	}
}
