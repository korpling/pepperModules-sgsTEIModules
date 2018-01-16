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
	
	private static final String DELIMITER = "_";
	
	@Test
	public void testPrimaryData() {
		SDocumentGraph goalGraph = SgsTEIExampleBuilder.getInstance().getSaltGraph();
		File resourceDir = new File(PepperTestUtil.getTestResources());
		File example = new File(resourceDir, SgsTEIExampleBuilder.getInstance().getFileNames().get(String.class));
		getFixture().setResourceURI(URI.createFileURI(example.getAbsolutePath()));
		getFixture().mapSDocument();
		
		SDocumentGraph producedGraph = getFixture().getDocument().getDocumentGraph();
		assertEquals(goalGraph.getTextualDSs().size(), producedGraph.getTextualDSs().size());
	}
	
	@Test
	public void testMorphosyntax() {
		SDocumentGraph goalGraph = SgsTEIExampleBuilder.getInstance().getSaltGraph();
		File resourceDir = new File(PepperTestUtil.getTestResources());
		File example = new File(resourceDir, SgsTEIExampleBuilder.getInstance().getFileNames().get(String.class));
		getFixture().setResourceURI(URI.createFileURI(example.getAbsolutePath()));
		getFixture().mapSDocument();
		// ...
	}
	
	@Test
	public void testSyntax() {
		SDocumentGraph goalGraph = SgsTEIExampleBuilder.getInstance().getSaltGraph();
		File resourceDir = new File(PepperTestUtil.getTestResources());
		File example = new File(resourceDir, SgsTEIExampleBuilder.getInstance().getFileNames().get(String.class));
		getFixture().setResourceURI(URI.createFileURI(example.getAbsolutePath()));
		getFixture().mapSDocument();

		// ...
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
