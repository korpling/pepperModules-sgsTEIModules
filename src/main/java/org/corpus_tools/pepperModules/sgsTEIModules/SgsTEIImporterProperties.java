package org.corpus_tools.pepperModules.sgsTEIModules;
import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;

public class SgsTEIImporterProperties extends PepperModuleProperties {
	/** This property determines the diplomatic layer's name. */
	private static final String PROP_DIPL_NAME = "dipl.name";
	/** This property determines the normed layer's name. */
	private static final String PROP_NORM_NAME = "norm.name";
	/** This property determines the pause layer's name. */
	private static final String PROP_PAUSE_NAME = "pause.name";
	/** Delimiter for multiple analyses on one token */
	private static final String PROP_ANALYSES_DELIMITER = "ana.del";
	
	public SgsTEIImporterProperties() {
		addProperty(new PepperModuleProperty<String>(PROP_DIPL_NAME, String.class, "This property determines the diplomatic layer's name.", "dipl", false));
		addProperty(new PepperModuleProperty<String>(PROP_NORM_NAME, String.class, "This property determines the normed layer's name.", "norm", false));
		addProperty(new PepperModuleProperty<String>(PROP_PAUSE_NAME, String.class, "This property determines the pause layer's name.", "pause", false));
		addProperty(new PepperModuleProperty<String>(PROP_ANALYSES_DELIMITER, String.class, "Delimiter for multiple analyses on one token", ",", false));
	}
	
	/**
	 * Get the desired name for the diplomatic segmentation layer.
	 * @return
	 */
	public String getDiplName() {
		return (String) getProperty(PROP_DIPL_NAME).getValue();
	}
	
	/**
	 * Get the desired name for the normed segmentation layer.
	 * @return
	 */
	public String getNormName() {
		return (String) getProperty(PROP_NORM_NAME).getValue();
	}
	
	/**
	 * Get the desired name for the pause segmentation layer.
	 * @return
	 */
	public String getPauseName() {
		return (String) getProperty(PROP_PAUSE_NAME).getValue();
	}
	
	/**
	 * Get the delimiter for multiple values of one type of analysis
	 * @return delimiter
	 */
	public String getAnalysesDelimiter() {
		return (String) getProperty(PROP_ANALYSES_DELIMITER).getValue();
	}
	
	
}
