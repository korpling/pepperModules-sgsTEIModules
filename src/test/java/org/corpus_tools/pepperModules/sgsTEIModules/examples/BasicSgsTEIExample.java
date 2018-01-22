package org.corpus_tools.pepperModules.sgsTEIModules.examples;

/**
 * This class provides the basic example for sgsTEIModules featuring 
 * tokenization and morphosyntactical annotation.
 * @author klotzmaz
 *
 */
public class BasicSgsTEIExample extends AbstractSgsTEIExample {
	private static final String XML_EXAMPLE_FILE = "example_morphology.xml";
	public BasicSgsTEIExample() {
		this(XML_EXAMPLE_FILE, null);		
	}
	
	protected BasicSgsTEIExample(String xmlExampleFile, String saltExampleFile) {
		super(xmlExampleFile, saltExampleFile);
	}
}
