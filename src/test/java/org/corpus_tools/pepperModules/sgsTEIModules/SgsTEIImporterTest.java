package org.corpus_tools.pepperModules.sgsTEIModules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.corpus_tools.pepper.testFramework.PepperImporterTest;
import org.corpus_tools.pepper.testFramework.PepperTestUtil;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SOrderRelation;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.util.DataSourceSequence;
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
public class SgsTEIImporterTest {
	public static final String TEXT_DIPL_0 = "bueno , pues hbía visto subir con hombrnoes jóvenes . que subían mm a medianoche . y bajaban a ltas horas de la madrugada . bueno , yo no yo cre mira te voy a decir la verdad . ";
	public static final String TEXT_DIPL_1 = "¿ crees que podrían mantener alguna relación amorosa ? ";
	public static final String TEXT_NORM_0 = "bueno , pues había visto subir con hombretones jóvenes . que subían a medianoche . y bajaban a altas horas de la madrugada . bueno , yo no yo creo mira te voy a decir la verdad . ";
	public static final String TEXT_NORM_1 = "¿ crees que podrían mantener alguna relación amorosa ? ";
	public static final String TEXT_PAUSE_0 = "long ";
	public static final String TEXT_PAUSE_1 = "long ";
	public static final String SPEAKER_0 = "ANT";
	public static final String SPEAKER_1 = "S02";
	
	private SgsTEI2SaltMapper fixture = null;
	
	private static final String EXAMPLE_FILE = "example.xml";
	
	private static final String LEMMA = "lemma";
	
	private static final String POS = "pos";
	
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
		SDocumentGraph docGraph = SaltFactory.createSDocumentGraph();
		
		STimeline timeline = docGraph.createTimeline();
		STimelineRelation tRel = null;
		SOrderRelation oRel = null;
		SToken tok = null;
		
		STextualDS diplDS0 = docGraph.createTextualDS(TEXT_DIPL_0);
		STextualDS normDS0 = docGraph.createTextualDS(TEXT_NORM_0);
		STextualDS pauseDS0 = docGraph.createTextualDS(TEXT_PAUSE_0);
		STextualDS diplDS1 = docGraph.createTextualDS(TEXT_DIPL_1);
		STextualDS normDS1 = docGraph.createTextualDS(TEXT_NORM_1);
		STextualDS pauseDS1 = docGraph.createTextualDS(TEXT_PAUSE_1);
		{
			diplDS0.setName(String.join(DELIMITER, SPEAKER_0, getModuleProperties().getDiplName()));
			diplDS1.setName(String.join(DELIMITER, SPEAKER_1, getModuleProperties().getDiplName()));
			normDS0.setName(String.join(DELIMITER, SPEAKER_0, getModuleProperties().getNormName()));
			normDS1.setName(String.join(DELIMITER, SPEAKER_1, getModuleProperties().getNormName()));
			pauseDS0.setName(String.join(DELIMITER, SPEAKER_0, getModuleProperties().getPauseName()));
			pauseDS1.setName(String.join(DELIMITER, SPEAKER_1, getModuleProperties().getPauseName()));
		}

		STextualDS diplDS = diplDS0;
		STextualDS normDS = normDS0;
		STextualDS pauseDS = pauseDS0;
		
		ArrayList<SToken> diplTokens = new ArrayList<SToken>();
		ArrayList<SToken> normTokens = new ArrayList<SToken>();
		ArrayList<SToken> pauseTokens = new ArrayList<SToken>();

		int point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 0, 5, point) );
		normTokens.add( createToken(docGraph, normDS, 0, 5, point, SPEAKER_0, "bueno", "ADJ") );

		point = newPointOfTime(timeline);	
		pauseTokens.add( createToken(docGraph, pauseDS, 0, 4, point) );

		point = newPointOfTime(timeline);		
		diplTokens.add( createToken(docGraph, diplDS, 6, 7, point) );
		normTokens.add( createToken(docGraph, normDS, 6, 7, point) );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 8, 12, point) );
		normTokens.add( createToken(docGraph, normDS, 8, 12, point, SPEAKER_0, "pues", "CSUBF") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 13, 17, point) );		
		normTokens.add( createToken(docGraph, normDS, 13, 18, point, SPEAKER_0, "haber", "VHfin") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 18, 23, point) );
		normTokens.add( createToken(docGraph, normDS, 19, 24, point, SPEAKER_0, "ver", "VLadj") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 24, 29, point) );
		normTokens.add( createToken(docGraph, normDS, 25, 30, point, SPEAKER_0, "subir", "VLinf") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 30, 33, point) );
		normTokens.add( createToken(docGraph, normDS, 31, 34, point, SPEAKER_0, "con", "PREP") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 34, 43, point) );
		normTokens.add( createToken(docGraph, normDS, 35, 46, point, SPEAKER_0, "hombretones", "NC") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 44, 51, point) );
		normTokens.add( createToken(docGraph, normDS, 47, 54, point, SPEAKER_0, "joven", "ADJ") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 52, 53, point) );
		normTokens.add( createToken(docGraph, normDS, 55, 56, point) );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 54, 57, point) );
		normTokens.add( createToken(docGraph, normDS, 57, 60, point, SPEAKER_0, "que", "CQUE") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 58, 64, point) );
		normTokens.add( createToken(docGraph, normDS, 61, 67, point, SPEAKER_0, "subir", "VLfin") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 65, 67, point) );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 68, 69, point) );
		normTokens.add( createToken(docGraph, normDS, 68, 69, point, SPEAKER_0, "a", "PREP") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 70, 80, point) );
		normTokens.add( createToken(docGraph, normDS, 70, 80, point, SPEAKER_0, "medianoche", "NC") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 81, 82, point) );
		normTokens.add( createToken(docGraph, normDS, 81, 82, point) );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 83, 84, point) );
		normTokens.add( createToken(docGraph, normDS, 83, 84, point, SPEAKER_0, "y", "CC") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 85, 92, point) );
		normTokens.add( createToken(docGraph, normDS, 85, 92, point, SPEAKER_0, "bajar", "VLfin") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 93, 94, point) );
		normTokens.add( createToken(docGraph, normDS, 93, 94, point, SPEAKER_0, "a", "PREP") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 95, 99, point) );
		normTokens.add( createToken(docGraph, normDS, 95, 100, point, SPEAKER_0, "alto", "ADJ") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 100, 105, point) );
		normTokens.add( createToken(docGraph, normDS, 101, 106, point, SPEAKER_0, "hora", "NC") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 106, 108, point) );
		normTokens.add( createToken(docGraph, normDS, 107, 109, point, SPEAKER_0, "de", "PREP") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 109, 111, point) );
		normTokens.add( createToken(docGraph, normDS, 110, 112, point, SPEAKER_0, "el", "ART") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 112, 121, point) );
		normTokens.add( createToken(docGraph, normDS, 113, 122, point, SPEAKER_0, "madugada", "NC") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 122, 123, point) );
		normTokens.add( createToken(docGraph, normDS, 123, 124, point) );

		diplDS = diplDS1;
		normDS = normDS1;
		pauseDS = pauseDS1;

		addOrderRelations(docGraph, diplTokens, SPEAKER_0, getModuleProperties().getDiplName());
		addOrderRelations(docGraph, normTokens, SPEAKER_0, getModuleProperties().getNormName());
		addOrderRelations(docGraph, pauseTokens, SPEAKER_0, getModuleProperties().getPauseName());
		
		diplTokens.clear();
		normTokens.clear();
		pauseTokens.clear();

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 0, 1, point) );
		normTokens.add( createToken(docGraph, normDS, 0, 1, point) );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 2, 7, point) );
		normTokens.add( createToken(docGraph, normDS, 2, 7, point, SPEAKER_1, "crear|creer", "VLfin") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 8, 11, point) );
		normTokens.add( createToken(docGraph, normDS, 8, 11, point, SPEAKER_1, "que", "CQUE") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 12, 19, point) );
		normTokens.add( createToken(docGraph, normDS, 12, 19, point, SPEAKER_1, "poder", "VMfin") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 20, 28, point) );
		normTokens.add( createToken(docGraph, normDS, 20, 28, point, SPEAKER_1, "mantener", "VLinf") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 29, 35, point) );
		normTokens.add( createToken(docGraph, normDS, 29, 35, point, SPEAKER_1, "alguno", "QU") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 36, 44, point) );
		normTokens.add( createToken(docGraph, normDS, 36, 44, point, SPEAKER_1, "relación", "NC") );

		point = newPointOfTime(timeline);
		pauseTokens.add( createToken(docGraph, pauseDS, 0, 4, point) );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 45, 52, point) );
		normTokens.add( createToken(docGraph, normDS, 45, 52, point, SPEAKER_1, "amoroso", "ADJ") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 53, 54, point) );
		normTokens.add( createToken(docGraph, normDS, 53, 54, point) );

		diplDS = diplDS0;
		normDS = normDS0;
		pauseDS = pauseDS0;
		
		addOrderRelations(docGraph, diplTokens, SPEAKER_1, getModuleProperties().getDiplName());
		addOrderRelations(docGraph, normTokens, SPEAKER_1, getModuleProperties().getNormName());
		addOrderRelations(docGraph, pauseTokens, SPEAKER_1, getModuleProperties().getPauseName());

		diplTokens.clear();
		normTokens.clear();
		pauseTokens.clear();

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 124, 129, point) );
		normTokens.add( createToken(docGraph, normDS, 125, 130, point, SPEAKER_0, "bueno", "ADJ") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 130, 131, point) );
		normTokens.add( createToken(docGraph, normDS, 131, 132, point) );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 132, 134, point) );
		normTokens.add( createToken(docGraph, normDS, 133, 135, point, SPEAKER_0, "yo", "PPX") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 135, 137, point) );
		normTokens.add( createToken(docGraph, normDS, 136, 138, point, SPEAKER_0, "no", "NEG") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 138, 140, point) );
		normTokens.add( createToken(docGraph, normDS, 139, 141, point, SPEAKER_0, "yo", "PPX") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 141, 144, point) );
		normTokens.add( createToken(docGraph, normDS, 142, 146, point, SPEAKER_0, "cre", "VLfin") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 145, 149, point) );
		normTokens.add( createToken(docGraph, normDS, 147, 151, point, SPEAKER_0, "mirar", "VLfin") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 150, 152, point) );
		normTokens.add( createToken(docGraph, normDS, 152, 154, point, SPEAKER_0, "tú", "PPX") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 153, 156, point) );
		normTokens.add( createToken(docGraph, normDS, 155, 158, point, SPEAKER_0, "ir", "VLfin") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 157, 158, point) );
		normTokens.add( createToken(docGraph, normDS, 159, 160, point, SPEAKER_0, "a", "PREP") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 159, 164, point) );
		normTokens.add( createToken(docGraph, normDS, 161, 166, point, SPEAKER_0, "decir", "VLinf") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 165, 167, point) );
		normTokens.add( createToken(docGraph, normDS, 167, 169, point, SPEAKER_0, "el", "ART") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 168, 174, point) );
		normTokens.add( createToken(docGraph, normDS, 170, 176, point, SPEAKER_0, "verdad", "NC") );

		point = newPointOfTime(timeline);
		diplTokens.add( createToken(docGraph, diplDS, 175, 176, point) );
		normTokens.add( createToken(docGraph, normDS, 177, 178, point) );
		
		addOrderRelations(docGraph, diplTokens, SPEAKER_0, getModuleProperties().getDiplName());
		addOrderRelations(docGraph, normTokens, SPEAKER_0, getModuleProperties().getNormName());
		addOrderRelations(docGraph, pauseTokens, SPEAKER_0, getModuleProperties().getPauseName());

		return docGraph;
	}
	
	private static final String DELIMITER = "_";
	
	private void addAnnotations(SToken tok, String speakerName, String lemmaValue, String posValue) {		
		tok.createAnnotation(null, String.join(DELIMITER, speakerName, LEMMA), lemmaValue);
		tok.createAnnotation(null, String.join(DELIMITER, speakerName, POS), posValue);
	}
	
	private void addTimelineRelation(SDocumentGraph docGraph, SToken tok, int point) {
		STimeline timeline = docGraph.getTimeline();
		STimelineRelation tRel = SaltFactory.createSTimelineRelation();
		tRel.setStart(point - 1);
		tRel.setEnd(point);
		tRel.setSource(tok);
		tRel.setTarget(timeline);
		docGraph.addRelation(tRel);
	}
	
	private int newPointOfTime(STimeline timeline) {
		timeline.increasePointOfTime();
		return timeline.getEnd();
	}
	
	private SToken createToken(SDocumentGraph graph, STextualDS ds, int start, int end, int point) {
		SToken tok = graph.createToken(ds, start, end);
		addTimelineRelation(graph, tok, point);
		return tok;
	}
	
	private SToken createToken(SDocumentGraph graph, STextualDS ds, int start, int end, int point, String speakerName, String lemmaValue, String posValue) {
		SToken tok = createToken(graph, ds, start, end, point);
		addAnnotations(tok, speakerName, lemmaValue, posValue);
		return tok;
	}
	
	private void addOrderRelations(SDocumentGraph graph, List<SToken> tokenList, String speakerName, String name) {
		String qName = String.join(DELIMITER, speakerName, name);
		SOrderRelation oRel = null;
		for (int i = 1; i < tokenList.size(); i++) {
			oRel = SaltFactory.createSOrderRelation();
			oRel.setSource(tokenList.get(i - 1));
			oRel.setTarget(tokenList.get(i));
			oRel.setType(qName);
			graph.addRelation(oRel);
		}
	}
	
	
	@Test
	public void testPrimaryData() {
		SDocumentGraph goalGraph = getGoalDocumentGraph(null);
		File resourceDir = new File(PepperTestUtil.getTestResources());
		File example = new File(resourceDir, EXAMPLE_FILE);
		getFixture().setResourceURI(URI.createFileURI(example.getAbsolutePath()));
		getFixture().mapSDocument();
		
		assertNotNull(getFixture().getDocument());
		SDocumentGraph fixGraph = getFixture().getDocument().getDocumentGraph();
		assertNotNull(fixGraph);		 
		assertEquals(goalGraph.getTextualDSs().size(), fixGraph.getTextualDSs().size());
		assertEquals(goalGraph.getTokens().size(), fixGraph.getTokens().size());
		HashMap<STextualDS, STextualDS> dsMapping = new HashMap<>();
		HashSet<STextualDS> served = new HashSet<>();
		for (STextualDS ds : goalGraph.getTextualDSs()) {
			for (STextualDS fds : fixGraph.getTextualDSs()) {
				if (!served.contains(fds) && ds.getName() != null && ds.getName().equals(fds.getName())) {
					dsMapping.put(ds, fds);
					served.add(fds);
				}
			}
		}
		for (Entry<STextualDS, STextualDS> e : dsMapping.entrySet()) {
			STextualDS goalDS = e.getKey();
			STextualDS fixDS = e.getValue();
			assertEquals(goalDS.getText(), fixDS.getText());
			DataSourceSequence goalSeq = new DataSourceSequence<Number>(goalDS, goalDS.getStart(), goalDS.getEnd());
			DataSourceSequence fixSeq = new DataSourceSequence<Number>(fixDS, fixDS.getStart(), fixDS.getEnd());
			List<SToken> goalTokens = goalGraph.getSortedTokenByText( goalGraph.getTokensBySequence(goalSeq) );
			List<SToken> fixTokens = fixGraph.getSortedTokenByText( fixGraph.getTokensBySequence(fixSeq) );
			assertEquals(goalTokens.size(), fixTokens.size());
			for (int i = 0; i < goalTokens.size(); i++) {
				assertEquals(goalGraph.getText(goalTokens.get(i)), fixGraph.getText(fixTokens.get(i)));
//				System.out.println("|" + goalGraph.getText(goalTokens.get(i)) + "|<->|" + fixGraph.getText(fixTokens.get(i)) + "|");
			}
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
