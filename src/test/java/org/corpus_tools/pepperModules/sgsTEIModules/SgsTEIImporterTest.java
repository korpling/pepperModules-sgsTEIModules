package org.corpus_tools.pepperModules.sgsTEIModules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.corpus_tools.pepper.testFramework.PepperImporterTest;
import org.corpus_tools.pepper.testFramework.PepperTestUtil;
import org.corpus_tools.pepperModules.sgsTEIModules.examples.MorphosyntaxSgsTEIExample;
import org.corpus_tools.pepperModules.sgsTEIModules.examples.ReferenceSgsTEIExample;
import org.corpus_tools.pepperModules.sgsTEIModules.examples.SgsTEIExample;
import org.corpus_tools.pepperModules.sgsTEIModules.examples.SyntaxSgsTEIExample;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.common.SaltProject;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.util.DataSourceSequence;
import org.corpus_tools.salt.util.Difference;
import org.corpus_tools.salt.util.SaltUtil;
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
	
	@Test
	public void testPrimaryData() {
		SgsTEIExample exampleBuilder = new MorphosyntaxSgsTEIExample(); 
		SDocumentGraph goalGraph = exampleBuilder.getSaltGraph();
		File resourceDir = new File(PepperTestUtil.getTestResources());
		File example = new File(resourceDir, exampleBuilder.getFileNames().get(String.class));
		getFixture().setResourceURI(URI.createFileURI(example.getAbsolutePath()));
		getFixture().mapSDocument();
		
		SDocumentGraph producedGraph = getFixture().getDocument().getDocumentGraph();
		assertNotNull(producedGraph);
		assertEquals(goalGraph.getTextualDSs().size(), producedGraph.getTextualDSs().size());
		HashMap<String, STextualDS> dsMap = new HashMap<>();
		for (STextualDS ds : goalGraph.getTextualDSs()) {
			dsMap.put(ds.getName(), ds);
		}
		for (STextualDS producedDS : producedGraph.getTextualDSs()) {
			STextualDS goalDS = dsMap.get( producedDS.getName() );
			assertEquals(goalDS.getText(), producedDS.getText());			
			List<SToken> goalTokens = goalGraph.getSortedTokenByText( goalGraph.getTokensBySequence( new DataSourceSequence<Number>(goalDS, goalDS.getStart(), goalDS.getEnd()) ));
			List<SToken> producedTokens = producedGraph.getSortedTokenByText( producedGraph.getTokensBySequence( new DataSourceSequence<Number>(producedDS, producedDS.getStart(), producedDS.getEnd()) ));
			assertEquals(goalTokens.size(), producedTokens.size());
			for (int i = 0; i < goalTokens.size(); i++) {
				String goalText = goalGraph.getText(goalTokens.get(i));
				String producedText = producedGraph.getText(producedTokens.get(i));
				assertEquals(goalText, producedText);
			}			
		}
	}
	
	@Test
	public void testMorphosyntax() {
		testExample(new MorphosyntaxSgsTEIExample());
	}
	
	@Test
	public void testSyntax() {
		testExample(new SyntaxSgsTEIExample());
	}
	
	@Test
	public void testReference() {
		testExample(new ReferenceSgsTEIExample());
	}
	
	@Test
	public void testDebug() {
//		SgsTEIExample example = new ReferenceSgsTEIExample();
//		SaltFactory f;
//		SDocumentGraph goalGraph = example.getSaltGraph();
//		File resourceDir = new File(PepperTestUtil.getTestResources());
//		File exampleFile = new File(resourceDir, example.getFileNames().get(String.class));
//		getFixture().setResourceURI(URI.createFileURI(exampleFile.getAbsolutePath()));
//		getFixture().mapSDocument();
//
//		SDocumentGraph producedGraph = getFixture().getDocument().getDocumentGraph();
//		Set<String> goal = new HashSet<>();
//		for (SSpan span : goalGraph.getSpans()) {
//			goal.add(goalGraph.getText(span) + " " + span.getAnnotations().size());
//		}
//		Set<String> produced = new HashSet<>();
//		for (SSpan span : producedGraph.getSpans()) {
//			produced.add(producedGraph.getText(span) + " " + span.getAnnotations().size());
//		}
//		System.out.println(Sets.difference(goal, produced).toString() + "\tgoal" + (goal.size() > produced.size()? ">" : "<") + "produced");
//		System.out.println(Sets.symmetricDifference(goal, produced));
//		System.out.println(" ");
//		for (SSpan span : goalGraph.getSpans()) {
//			System.out.println(goalGraph.getText(span) + " " + span.getAnnotations());
//		}
		debuggableTest(new ReferenceSgsTEIExample());
	}
	
	private void debuggableTest(SgsTEIExample example) {
		SDocumentGraph goalGraph = example.getSaltGraph();
		File resourceDir = new File(PepperTestUtil.getTestResources());
		File exampleFile = new File(resourceDir, example.getFileNames().get(String.class));
		getFixture().setResourceURI(URI.createFileURI(exampleFile.getAbsolutePath()));
		getFixture().mapSDocument();

		SDocumentGraph producedGraph = getFixture().getDocument().getDocumentGraph();		
		Consumer<Difference> display = new Consumer<Difference>() {
			@Override
			public void accept(Difference t) {
				if (t.templateObject != null) {
					System.out.println(((SDocumentGraph) ((SNode) t.templateObject).getGraph()).getText((SNode) t.templateObject));
				} else {
					System.out.println("NULL");
				}
				System.out.println(((SDocumentGraph) ((SNode) t.otherObject).getGraph()).getText((SNode) t.otherObject));
			}
		};
		producedGraph.findDiffs(goalGraph).stream().forEach(display);
		SaltProject project = SaltFactory.createSaltProject();
		project.addCorpusGraph(goalGraph.getDocument().getGraph());
		SaltUtil.saveSaltProject(project, URI.createFileURI("/home/klotzmaz/pepper/test/"));
	}
	
	private void testExample(SgsTEIExample example) {
		SDocumentGraph goalGraph = example.getSaltGraph();
		File resourceDir = new File(PepperTestUtil.getTestResources());
		File exampleFile = new File(resourceDir, example.getFileNames().get(String.class));
		getFixture().setResourceURI(URI.createFileURI(exampleFile.getAbsolutePath()));
		getFixture().mapSDocument();

		SDocumentGraph producedGraph = getFixture().getDocument().getDocumentGraph();
		Set<Difference> diffSet = goalGraph.findDiffs(producedGraph);
		assertEquals(diffSet.toString(), 0, diffSet.size());
		diffSet = producedGraph.findDiffs(goalGraph);
		assertEquals(diffSet.toString(), 0, diffSet.size());
	}
}
