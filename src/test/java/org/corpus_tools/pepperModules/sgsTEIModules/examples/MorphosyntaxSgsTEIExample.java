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

/**
 * This class provides the basic example for sgsTEIModules featuring 
 * tokenization and morphosyntactical annotation.
 * @author klotzmaz
 *
 */
public class MorphosyntaxSgsTEIExample extends AbstractSgsTEIExample {
	private static final String XML_EXAMPLE_FILE = "example_morphology.xml";	
	protected MorphosyntaxSgsTEIExample(String xmlExampleFile, String saltExampleFile) {
		super(xmlExampleFile, saltExampleFile);
	}

	public MorphosyntaxSgsTEIExample() {
		this(XML_EXAMPLE_FILE, null);
	}
}
