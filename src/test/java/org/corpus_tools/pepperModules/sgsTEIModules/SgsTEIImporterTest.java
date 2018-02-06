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
package org.corpus_tools.pepperModules.sgsTEIModules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.corpus_tools.pepper.testFramework.PepperImporterTest;
import org.corpus_tools.pepper.testFramework.PepperTestUtil;
import org.corpus_tools.pepperModules.sgsTEIModules.examples.MorphosyntaxSgsTEIExample;
import org.corpus_tools.pepperModules.sgsTEIModules.examples.ReferenceSgsTEIExample;
import org.corpus_tools.pepperModules.sgsTEIModules.examples.SgsTEIExample;
import org.corpus_tools.pepperModules.sgsTEIModules.examples.SyntaxSgsTEIExample;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.util.DataSourceSequence;
import org.corpus_tools.salt.util.Difference;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

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
