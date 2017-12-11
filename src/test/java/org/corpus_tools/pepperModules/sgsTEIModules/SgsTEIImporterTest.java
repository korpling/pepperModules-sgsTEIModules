package org.corpus_tools.pepperModules.sgsTEIModules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.corpus_tools.pepper.testFramework.PepperImporterTest;
import org.corpus_tools.pepper.testFramework.PepperTestUtil;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SDominanceRelation;
import org.corpus_tools.salt.common.SOrderRelation;
import org.corpus_tools.salt.common.SStructure;
import org.corpus_tools.salt.common.SStructuredNode;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SNode;
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
	public static final String SPEAKER_SYNTAX_0 = "S92";
	public static final String SPEAKER_SYNTAX_1 = "JER";
	
	public static final String TEXT_SYNTAX_DIPL_0 = "ben , je viens à propos du , j' imagine que tu sais , du du cadavre qu' on a retrouvé au quatrième étage . ";
	public static final String TEXT_SYNTAX_DIPL_1 = "c' était quel quelqu'un qui s' intéressait beaucoup aux autres , aux gens en général . ";
	public static final String TEXT_SYNTAX_NORM_0 = "ben , je viens à propos du , j' imagine que tu sais , du du cadavre qu' on a retrouvé au quatrième étage . ";
	public static final String TEXT_SYNTAX_NORM_1 = "c' était quelqu'un quelqu'un qui s' intéressait beaucoup aux autres , aux gens en général . ";
	public static final String TEXT_SYNTAX_PAUSE_0 = "short ";
	
	private SgsTEI2SaltMapper fixture = null;
	
	private static final String EXAMPLE_FILE = "example.xml";
	private static final String EXAMPLE_SYNTAX_FILE = "example_syntax.xml";
	
	private static final String LEMMA = "lemma";
	
	private static final String POS = "pos";
	
	private static final String CAT = "cat";
	
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
		
		ArrayList<SToken> diplTokens0 = new ArrayList<SToken>();
		ArrayList<SToken> normTokens0 = new ArrayList<SToken>();
		ArrayList<SToken> pauseTokens = new ArrayList<SToken>();

		int point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 0, 5, point) );
		normTokens0.add( createToken(docGraph, normDS, 0, 5, point, SPEAKER_0, "bueno", "ADJ") );

		point = newPointOfTime(timeline);	
		pauseTokens.add( createToken(docGraph, pauseDS, 0, 4, point) );

		point = newPointOfTime(timeline);		
		diplTokens0.add( createToken(docGraph, diplDS, 6, 7, point) );
		normTokens0.add( createToken(docGraph, normDS, 6, 7, point) );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 8, 12, point) );
		normTokens0.add( createToken(docGraph, normDS, 8, 12, point, SPEAKER_0, "pues", "CSUBF") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 13, 17, point) );		
		normTokens0.add( createToken(docGraph, normDS, 13, 18, point, SPEAKER_0, "haber", "VHfin") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 18, 23, point) );
		normTokens0.add( createToken(docGraph, normDS, 19, 24, point, SPEAKER_0, "ver", "VLadj") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 24, 29, point) );
		normTokens0.add( createToken(docGraph, normDS, 25, 30, point, SPEAKER_0, "subir", "VLinf") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 30, 33, point) );
		normTokens0.add( createToken(docGraph, normDS, 31, 34, point, SPEAKER_0, "con", "PREP") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 34, 43, point) );
		normTokens0.add( createToken(docGraph, normDS, 35, 46, point, SPEAKER_0, "hombretones", "NC") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 44, 51, point) );
		normTokens0.add( createToken(docGraph, normDS, 47, 54, point, SPEAKER_0, "joven", "ADJ") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 52, 53, point) );
		normTokens0.add( createToken(docGraph, normDS, 55, 56, point) );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 54, 57, point) );
		normTokens0.add( createToken(docGraph, normDS, 57, 60, point, SPEAKER_0, "que", "CQUE") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 58, 64, point) );
		normTokens0.add( createToken(docGraph, normDS, 61, 67, point, SPEAKER_0, "subir", "VLfin") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 65, 67, point) );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 68, 69, point) );
		normTokens0.add( createToken(docGraph, normDS, 68, 69, point, SPEAKER_0, "a", "PREP") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 70, 80, point) );
		normTokens0.add( createToken(docGraph, normDS, 70, 80, point, SPEAKER_0, "medianoche", "NC") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 81, 82, point) );
		normTokens0.add( createToken(docGraph, normDS, 81, 82, point) );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 83, 84, point) );
		normTokens0.add( createToken(docGraph, normDS, 83, 84, point, SPEAKER_0, "y", "CC") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 85, 92, point) );
		normTokens0.add( createToken(docGraph, normDS, 85, 92, point, SPEAKER_0, "bajar", "VLfin") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 93, 94, point) );
		normTokens0.add( createToken(docGraph, normDS, 93, 94, point, SPEAKER_0, "a", "PREP") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 95, 99, point) );
		normTokens0.add( createToken(docGraph, normDS, 95, 100, point, SPEAKER_0, "alto", "ADJ") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 100, 105, point) );
		normTokens0.add( createToken(docGraph, normDS, 101, 106, point, SPEAKER_0, "hora", "NC") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 106, 108, point) );
		normTokens0.add( createToken(docGraph, normDS, 107, 109, point, SPEAKER_0, "de", "PREP") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 109, 111, point) );
		normTokens0.add( createToken(docGraph, normDS, 110, 112, point, SPEAKER_0, "el", "ART") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 112, 121, point) );
		normTokens0.add( createToken(docGraph, normDS, 113, 122, point, SPEAKER_0, "madugada", "NC") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 122, 123, point) );
		normTokens0.add( createToken(docGraph, normDS, 123, 124, point) );

		diplDS = diplDS1;
		normDS = normDS1;
		pauseDS = pauseDS1;
		
		ArrayList<SToken> diplTokens1 = new ArrayList<SToken>();
		ArrayList<SToken> normTokens1 = new ArrayList<SToken>();

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 0, 1, point) );
		normTokens1.add( createToken(docGraph, normDS, 0, 1, point) );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 2, 7, point) );
		normTokens1.add( createToken(docGraph, normDS, 2, 7, point, SPEAKER_1, "crear|creer", "VLfin") );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 8, 11, point) );
		normTokens1.add( createToken(docGraph, normDS, 8, 11, point, SPEAKER_1, "que", "CQUE") );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 12, 19, point) );
		normTokens1.add( createToken(docGraph, normDS, 12, 19, point, SPEAKER_1, "poder", "VMfin") );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 20, 28, point) );
		normTokens1.add( createToken(docGraph, normDS, 20, 28, point, SPEAKER_1, "mantener", "VLinf") );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 29, 35, point) );
		normTokens1.add( createToken(docGraph, normDS, 29, 35, point, SPEAKER_1, "alguno", "QU") );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 36, 44, point) );
		normTokens1.add( createToken(docGraph, normDS, 36, 44, point, SPEAKER_1, "relación", "NC") );

		point = newPointOfTime(timeline);
		pauseTokens.add( createToken(docGraph, pauseDS, 0, 4, point) );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 45, 52, point) );
		normTokens1.add( createToken(docGraph, normDS, 45, 52, point, SPEAKER_1, "amoroso", "ADJ") );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 53, 54, point) );
		normTokens1.add( createToken(docGraph, normDS, 53, 54, point) );

		diplDS = diplDS0;
		normDS = normDS0;
		pauseDS = pauseDS0;
		
		addOrderRelations(docGraph, diplTokens1, SPEAKER_1, getModuleProperties().getDiplName());
		addOrderRelations(docGraph, normTokens1, SPEAKER_1, getModuleProperties().getNormName());

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 124, 129, point) );
		normTokens0.add( createToken(docGraph, normDS, 125, 130, point, SPEAKER_0, "bueno", "ADJ") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 130, 131, point) );
		normTokens0.add( createToken(docGraph, normDS, 131, 132, point) );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 132, 134, point) );
		normTokens0.add( createToken(docGraph, normDS, 133, 135, point, SPEAKER_0, "yo", "PPX") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 135, 137, point) );
		normTokens0.add( createToken(docGraph, normDS, 136, 138, point, SPEAKER_0, "no", "NEG") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 138, 140, point) );
		normTokens0.add( createToken(docGraph, normDS, 139, 141, point, SPEAKER_0, "yo", "PPX") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 141, 144, point) );
		normTokens0.add( createToken(docGraph, normDS, 142, 146, point, SPEAKER_0, "cre", "VLfin") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 145, 149, point) );
		normTokens0.add( createToken(docGraph, normDS, 147, 151, point, SPEAKER_0, "mirar", "VLfin") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 150, 152, point) );
		normTokens0.add( createToken(docGraph, normDS, 152, 154, point, SPEAKER_0, "tú", "PPX") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 153, 156, point) );
		normTokens0.add( createToken(docGraph, normDS, 155, 158, point, SPEAKER_0, "ir", "VLfin") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 157, 158, point) );
		normTokens0.add( createToken(docGraph, normDS, 159, 160, point, SPEAKER_0, "a", "PREP") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 159, 164, point) );
		normTokens0.add( createToken(docGraph, normDS, 161, 166, point, SPEAKER_0, "decir", "VLinf") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 165, 167, point) );
		normTokens0.add( createToken(docGraph, normDS, 167, 169, point, SPEAKER_0, "el", "ART") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 168, 174, point) );
		normTokens0.add( createToken(docGraph, normDS, 170, 176, point, SPEAKER_0, "verdad", "NC") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 175, 176, point) );
		normTokens0.add( createToken(docGraph, normDS, 177, 178, point) );
		
		addOrderRelations(docGraph, diplTokens0, SPEAKER_0, getModuleProperties().getDiplName());
		addOrderRelations(docGraph, normTokens0, SPEAKER_0, getModuleProperties().getNormName());

		return docGraph;
	}
	
	private SDocumentGraph getGoalDocumentGraphSyntax() {
		SDocument doc = SaltFactory.createSDocument();
		doc.setId("doc");
		SDocumentGraph docGraph = doc.createDocumentGraph();
		STimeline timeline = docGraph.createTimeline();
		
		String anaDelimiter = getModuleProperties().getAnalysesDelimiter();		
		
		STextualDS diplDS0 = docGraph.createTextualDS(TEXT_SYNTAX_DIPL_0);
		STextualDS diplDS1 = docGraph.createTextualDS(TEXT_SYNTAX_DIPL_1);
		STextualDS normDS0 = docGraph.createTextualDS(TEXT_SYNTAX_NORM_0);
		STextualDS normDS1 = docGraph.createTextualDS(TEXT_SYNTAX_NORM_1);
		STextualDS pauseDS0 = docGraph.createTextualDS(TEXT_SYNTAX_PAUSE_0);
		{
			diplDS0.setName(String.join(DELIMITER, SPEAKER_SYNTAX_0, getModuleProperties().getDiplName()));
			diplDS1.setName(String.join(DELIMITER, SPEAKER_SYNTAX_1, getModuleProperties().getDiplName()));
			normDS0.setName(String.join(DELIMITER, SPEAKER_SYNTAX_0, getModuleProperties().getNormName()));
			normDS1.setName(String.join(DELIMITER, SPEAKER_SYNTAX_1, getModuleProperties().getNormName()));
			pauseDS0.setName(String.join(DELIMITER, SPEAKER_SYNTAX_0, getModuleProperties().getPauseName()));			
		}
		
		STextualDS diplDS = diplDS0;
		STextualDS normDS = normDS0;
		STextualDS pauseDS = pauseDS0;

		List<SToken> diplTokens0 = new ArrayList<>();
		List<SToken> normTokens0 = new ArrayList<>();
		List<SToken> pauseTokens0 = new ArrayList<>();
		
		List<SToken> diplTokens1 = new ArrayList<>();
		List<SToken> normTokens1 = new ArrayList<>();
		
		int point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 0, 3, point) );
		normTokens0.add( createToken(docGraph, normDS, 0, 3, point, SPEAKER_SYNTAX_0, "ben", "INT") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 4, 5, point) );
		normTokens0.add( createToken(docGraph, normDS, 4, 5, point) );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 6, 8, point) );
		normTokens0.add( createToken(docGraph, normDS, 6, 8, point, SPEAKER_SYNTAX_0, "je", "PRO:cls") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 9, 14, point) );
		normTokens0.add( createToken(docGraph, normDS, 9, 14, point, SPEAKER_SYNTAX_0, "venir", "VER:pres") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 15, 26, point) );
		normTokens0.add( createToken(docGraph, normDS, 15, 26, point, SPEAKER_SYNTAX_0, String.join(anaDelimiter, "à~propos~du", "le"), String.join(anaDelimiter, "PRP", "DET:def")) );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 27, 28, point) );
		normTokens0.add( createToken(docGraph, normDS, 27, 28, point) );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 29, 31, point) );
		normTokens0.add( createToken(docGraph, normDS, 29, 31, point, SPEAKER_SYNTAX_0, "je", "PRO:cls") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 32, 39, point) );
		normTokens0.add( createToken(docGraph, normDS, 32, 39, point, SPEAKER_SYNTAX_0, "imaginer", "VER:pres") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 40, 43, point) );
		normTokens0.add( createToken(docGraph, normDS, 40, 43, point, SPEAKER_SYNTAX_0, "que", "KON") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 44, 46, point) );
		normTokens0.add( createToken(docGraph, normDS, 44, 46, point, SPEAKER_SYNTAX_0, "tu", "PRO:cls") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 47, 51, point) );
		normTokens0.add( createToken(docGraph, normDS, 47, 51, point, SPEAKER_SYNTAX_0, "savoir", "VER:pres") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 52, 53, point) );
		normTokens0.add( createToken(docGraph, normDS, 52, 53, point) );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 54, 56, point) );
		normTokens0.add( createToken(docGraph, normDS, 54, 56, point, SPEAKER_SYNTAX_0, String.join(anaDelimiter, "de", "le"), String.join(anaDelimiter, "PREP", "DET:def")) );

		point = newPointOfTime(timeline);
		pauseTokens0.add( createToken(docGraph, pauseDS, 0, 5, point) );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 57, 59, point) );
		normTokens0.add( createToken(docGraph, normDS, 57, 59, point, SPEAKER_SYNTAX_0, String.join(anaDelimiter, "de", "le"), String.join(anaDelimiter, "PREP", "DET:def")) );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 60, 67, point) );
		normTokens0.add( createToken(docGraph, normDS, 60, 67, point, SPEAKER_SYNTAX_0, "cadavre", "NOM") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 68, 71, point) );
		normTokens0.add( createToken(docGraph, normDS, 68, 71, point, SPEAKER_SYNTAX_0, "que", "PRO:rel") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 72, 74, point) );
		normTokens0.add( createToken(docGraph, normDS, 72, 74, point, SPEAKER_SYNTAX_0, "on", "PRO:cls") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 75, 76, point) );
		normTokens0.add( createToken(docGraph, normDS, 75, 76, point, SPEAKER_SYNTAX_0, "avoir", "AUX:pres") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 77, 85, point) );
		normTokens0.add( createToken(docGraph, normDS, 77, 85, point, SPEAKER_SYNTAX_0, "retrouver", "VER:pper") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 86, 88, point) );
		normTokens0.add( createToken(docGraph, normDS, 86, 88, point, SPEAKER_SYNTAX_0, String.join(anaDelimiter, "à", "le"), String.join(anaDelimiter, "PRP", "DET:def")) );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 89, 98, point) );
		normTokens0.add( createToken(docGraph, normDS, 89, 98, point, SPEAKER_SYNTAX_0, "quatrième", "ADJ") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 99, 104, point) );
		normTokens0.add( createToken(docGraph, normDS, 99, 104, point, SPEAKER_SYNTAX_0, "étage", "NOM") );

		point = newPointOfTime(timeline);
		diplTokens0.add( createToken(docGraph, diplDS, 105, 106, point) );
		normTokens0.add( createToken(docGraph, normDS, 105, 106, point) );

		diplDS = diplDS1;
		normDS = normDS1;

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 0, 2, point) );
		normTokens1.add( createToken(docGraph, normDS, 0, 2, point, SPEAKER_SYNTAX_1, "ce", "PRO:cls") );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 3, 8, point) );
		normTokens1.add( createToken(docGraph, normDS, 3, 8, point, SPEAKER_SYNTAX_1, "être", "VER:impf") );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 9, 13, point) );
		normTokens1.add( createToken(docGraph, normDS, 9, 18, point, SPEAKER_SYNTAX_1, "quel", "DET:int") );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 14, 23, point) );
		normTokens1.add( createToken(docGraph, normDS, 19, 28, point, SPEAKER_SYNTAX_1, "quelqu’un", "PRO:ind") );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 24, 27, point) );
		normTokens1.add( createToken(docGraph, normDS, 29, 32, point, SPEAKER_SYNTAX_1, "qui", "PRO:rel") );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 28, 30, point) );
		normTokens1.add( createToken(docGraph, normDS, 33, 35, point, SPEAKER_SYNTAX_1, "se", "PRO:clo") );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 31, 42, point) );
		normTokens1.add( createToken(docGraph, normDS, 36, 47, point, SPEAKER_SYNTAX_1, "s’intéresser", "VER:impf") );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 43, 51, point) );
		normTokens1.add( createToken(docGraph, normDS, 48, 56, point, SPEAKER_SYNTAX_1, "beaucoup", "ADV") );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 52, 55, point) );
		normTokens1.add( createToken(docGraph, normDS, 57, 60, point, SPEAKER_SYNTAX_1, String.join(anaDelimiter, "à", "le"), String.join(anaDelimiter, "PRP", "DET:def")) );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 56, 62, point) );
		normTokens1.add( createToken(docGraph, normDS, 61, 67, point, SPEAKER_SYNTAX_1, "autre", "QUA") );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 63, 64, point) );
		normTokens1.add( createToken(docGraph, normDS, 68, 69, point) );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 65, 68, point) );
		normTokens1.add( createToken(docGraph, normDS, 70, 73, point, SPEAKER_SYNTAX_1, String.join(anaDelimiter, "à", "le"), String.join(anaDelimiter, "PRP", "DET:def")) );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 69, 73, point) );
		normTokens1.add( createToken(docGraph, normDS, 74, 78, point, SPEAKER_SYNTAX_1, "gens", "NOM") );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 74, 84, point) );
		normTokens1.add( createToken(docGraph, normDS, 79, 89, point, SPEAKER_SYNTAX_1, "en~général", "ADV") );

		point = newPointOfTime(timeline);
		diplTokens1.add( createToken(docGraph, diplDS, 85, 86, point) );
		normTokens1.add( createToken(docGraph, normDS, 90, 91, point) );
		
		addOrderRelations(docGraph, diplTokens0, SPEAKER_SYNTAX_0, getModuleProperties().getDiplName());
		addOrderRelations(docGraph, diplTokens1, SPEAKER_SYNTAX_1, getModuleProperties().getDiplName());
		addOrderRelations(docGraph, normTokens0, SPEAKER_SYNTAX_0, getModuleProperties().getNormName());
		addOrderRelations(docGraph, normTokens1, SPEAKER_SYNTAX_1, getModuleProperties().getNormName());
		addOrderRelations(docGraph, pauseTokens0, SPEAKER_SYNTAX_0, getModuleProperties().getPauseName());
		
		/* add syntax */
		SLayer syntaxLayer = SaltFactory.createSLayer();
		docGraph.addLayer(syntaxLayer);
		/* speaker 0 */
		String[] syntacticNodes = {"PRN", "V", "P,D", "N", "PRN", "PRN", "V", "V", "P,D", "A", "N"};
		Integer[] indexes = {2, 3, 4, 14, 15, 16, 17, 18, 19, 20, 21};
		List<Integer> indexList = Arrays.asList(indexes);
		SStructure[][] leafStructures = new SStructure[22][2];
		
		for (int i = 0; i < indexes.length; i++) {
			int ix = indexes[i];
			if (syntacticNodes[i] == null) {
				leafStructures[ix][0] = null;
			} else {
				String[] cats = syntacticNodes[i].split(",");
				for (int j = 0; j < cats.length; j++) {
					leafStructures[ix][j] = docGraph.createStructure(normTokens0.get(ix));
					leafStructures[ix][j].createAnnotation(null, CAT, cats[j]);
					syntaxLayer.addNode(leafStructures[ix][j]);
				}
			}
		}
		
		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[21][0], leafStructures[19][1], "det") );
		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[21][0], leafStructures[20][0], "mod") );
		
		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[19][0], leafStructures[21][0], "obj") );
				
		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[18][0], leafStructures[15][0], "obj") );
		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[18][0], leafStructures[16][0], "suj") );
		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[18][0], leafStructures[17][0], "aux_tps") );
		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[18][0], leafStructures[19][0], "mod") );

		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[14][0], leafStructures[4][1], "det") );
		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[14][0], leafStructures[18][0], "mod_rel") );

		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[4][0], leafStructures[14][0], "obj") );

		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[3][0], leafStructures[2][0], "suj") );
		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[3][0], leafStructures[4][0], "mod") );

		SStructure root = SaltFactory.createSStructure();
		root.createAnnotation(null, CAT, "ROOT");
		docGraph.addNode(root);
		syntaxLayer.addRelation( createDominanceRelation(docGraph, root, leafStructures[3][0], "root") ); 
		syntaxLayer.addNode(root);
				
		/* speaker 1 */
		syntacticNodes = new String[] {"PRN", "V", null, "PRN", "PRN", "PRN", "V", "ADV", "P,D", "Q", null, "P,D", "N", "ADV"};
		indexes = new Integer[] {0, 1, 3, 4, 5, 6, 7, 8, 9, 11, 12, 13};
		indexList = Arrays.asList(indexes);
		leafStructures = new SStructure[14][2];	
		for (int ix : indexList) {
			if (syntacticNodes[ix] == null) {
				leafStructures[ix][0] = null;
			} else {
				String[] cats = syntacticNodes[ix].split(",");
				for (int j = 0; j < cats.length; j++) {
					leafStructures[ix][j] = docGraph.createStructure(normTokens1.get(ix));
					leafStructures[ix][j].createAnnotation(null, CAT, cats[j]);
				}
			}			
		}

		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[12][0], leafStructures[11][1], "det") );
		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[12][0], leafStructures[13][0], "mod") );

		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[11][0], leafStructures[12][0], "obj") );

		SStructure coord = SaltFactory.createSStructure();
		coord.createAnnotation(null, CAT, "COORD");
		coord.createAnnotation(null, "form", "elided");
		docGraph.addNode(coord);
		syntaxLayer.addRelation( createDominanceRelation(docGraph, coord, leafStructures[11][0], "dep_coord") );

		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[9][0], leafStructures[8][1], "det") );
		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[8][0], leafStructures[9][0], "obj") );
		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[8][0], coord, "coord") );

		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[6][0], leafStructures[4][0], "suj") );
		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[6][0], leafStructures[5][0], "att") );
		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[6][0], leafStructures[7][0], "mod") );
		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[6][0], leafStructures[8][0], "p_obj") );

		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[3][0], leafStructures[6][0], "mod_rel") );

		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[1][0], leafStructures[0][0], "suj") );
		syntaxLayer.addRelation( createDominanceRelation(docGraph, leafStructures[1][0], leafStructures[3][0], "ats") );

		root = SaltFactory.createSStructure();
		root.createAnnotation(null, CAT, "ROOT");
		docGraph.addNode(root);
		syntaxLayer.addRelation( createDominanceRelation(docGraph, root, leafStructures[1][0], "root") );
		syntaxLayer.addNode(root);
		
		return docGraph;
	}
	
	private void getGoalGraphReferencesAndSyntax() {
		
	}
	
	private void debugPrintNode(SDocumentGraph graph, SNode node) {
		System.out.println(String.join(":", node.getAnnotations().toString(), graph.getText(node)));
	}
	
	private SDominanceRelation createDominanceRelation(SDocumentGraph graph, SStructuredNode source, SStructuredNode target, String edgeLabel) {
		return (SDominanceRelation) graph.createRelation(source, target, SALT_TYPE.SDOMINANCE_RELATION, String.join("=", SgsTEIDictionary.ATT_TYPE, edgeLabel));
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
		
		basicTest(goalGraph, fixGraph);
		primaryDataTest(goalGraph, fixGraph);
	}
	
	@Test
	public void testMorphosyntax() {
		SDocumentGraph goalGraph = getGoalDocumentGraph(null);
		File resourceDir = new File(PepperTestUtil.getTestResources());
		File example = new File(resourceDir, EXAMPLE_FILE);
		getFixture().setResourceURI(URI.createFileURI(example.getAbsolutePath()));
		getFixture().mapSDocument();
		
		SDocumentGraph fixGraph = getFixture().getDocument().getDocumentGraph();
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
			DataSourceSequence<?> goalSeq = new DataSourceSequence<Number>(goalDS, goalDS.getStart(), goalDS.getEnd());
			DataSourceSequence<?> fixSeq = new DataSourceSequence<Number>(fixDS, fixDS.getStart(), fixDS.getEnd());
			List<SToken> goalTokens = goalGraph.getSortedTokenByText( goalGraph.getTokensBySequence(goalSeq) );
			List<SToken> fixTokens = fixGraph.getSortedTokenByText( fixGraph.getTokensBySequence(fixSeq) );
			assertEquals(goalTokens.size(), fixTokens.size());
			for (int i = 0; i < goalTokens.size(); i++) {
				assertEquals(goalGraph.getText(goalTokens.get(i)), fixGraph.getText(fixTokens.get(i)));
				assertEquals(goalTokens.get(i).getAnnotations().size(), fixTokens.get(i).getAnnotations().size());
				assertEquals(goalTokens.get(i).getAnnotation(LEMMA), fixTokens.get(i).getAnnotation(LEMMA));
				assertEquals(goalTokens.get(i).getAnnotation(POS), fixTokens.get(i).getAnnotation(POS));
			}
		}
	}
	
	@Test
	public void testSyntax() {
		SDocumentGraph goalGraph = getGoalDocumentGraphSyntax();
		File resourceDir = new File(PepperTestUtil.getTestResources());
		File example = new File(resourceDir, EXAMPLE_SYNTAX_FILE);
		getFixture().setResourceURI(URI.createFileURI(example.getAbsolutePath()));
		getFixture().mapSDocument();
		
		assertNotNull(getFixture().getDocument());
		SDocumentGraph fixGraph = getFixture().getDocument().getDocumentGraph();

		basicTest(goalGraph, fixGraph);
		primaryDataTest(goalGraph, fixGraph);
		
		//find roots
		List<SStructure> goalStructs = goalGraph.getStructures();
		List<SStructure> fixStructs = fixGraph.getStructures();
		assertEquals(goalStructs.size(), fixStructs.size());
		
		for (int f = 0; f < fixStructs.size(); f++) {
			SStructure fs = fixStructs.get(f);			
			int g = 0;
			SStructure gs = goalStructs.get(g);
			while (g < goalStructs.size() && !fixGraph.getText(fs).equals(goalGraph.getText(goalStructs.get(g))) 
					&& !fs.getAnnotation(CAT).getValue_STEXT().equals(goalStructs.get(g).getAnnotation(CAT).getValue_STEXT())
					&& fs.getInRelations().size() != gs.getInRelations().size()
					&& fs.getOutRelations().size() != gs.getOutRelations().size()
					) {
				g++;
				gs = goalStructs.get(g);
			}
			assertTrue(g <= goalStructs.size());
		}
	}
	
	private void basicTest(SDocumentGraph goalGraph, SDocumentGraph fixGraph) {
		assertEquals(goalGraph.getRelations().size(), fixGraph.getRelations().size());
		assertEquals(goalGraph.getNodes().size(), fixGraph.getNodes().size());		
	}
	
	private void primaryDataTest(SDocumentGraph goalGraph, SDocumentGraph fixGraph) {
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
			DataSourceSequence<?> goalSeq = new DataSourceSequence<Number>(goalDS, goalDS.getStart(), goalDS.getEnd());
			DataSourceSequence<?> fixSeq = new DataSourceSequence<Number>(fixDS, fixDS.getStart(), fixDS.getEnd());
			List<SToken> goalTokens = goalGraph.getSortedTokenByText( goalGraph.getTokensBySequence(goalSeq) );
			List<SToken> fixTokens = fixGraph.getSortedTokenByText( fixGraph.getTokensBySequence(fixSeq) );
			assertEquals(goalTokens.size(), fixTokens.size());
			for (int i = 0; i < goalTokens.size(); i++) {
				assertEquals(goalGraph.getText(goalTokens.get(i)), fixGraph.getText(fixTokens.get(i)));
			}
		}
	}
	
	@Test
	public void testReference() {
		fail();
	}
	
	@Test
	public void testMetaData() {
		fail();
	}
}
