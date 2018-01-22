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
