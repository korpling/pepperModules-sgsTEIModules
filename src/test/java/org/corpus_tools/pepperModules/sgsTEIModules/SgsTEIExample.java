package org.corpus_tools.pepperModules.sgsTEIModules;

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
