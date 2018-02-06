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

import java.util.Map;

import org.corpus_tools.salt.common.SDocumentGraph;

public interface SgsTEIExample {
	/** This method provides the expected {@link SDocumentGraph} for 
	 *  this example.
	 * @return {@link SDocumentGraph}
	 */
	public SDocumentGraph getSaltGraph();
	
	/** This method returns the expected xml String to create by an
	 *  exporter out of this {@link SDocumentGraph}'s example.
	 * @return
	 */
	public String getXML();
	
	/** This method provides filenames the examples can be read
	 * from for a given goal class ({@link String} or {@link SDocumentGraph},
	 * if available.
	 * @return
	 */
	public Map<Class<?>, String> getFileNames();
}
