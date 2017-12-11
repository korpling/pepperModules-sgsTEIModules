package org.corpus_tools.pepperModules.sgsTEIModules;

import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;

public class SgsTEIExporterProperties  extends PepperModuleProperties {
	/** This property provides the name of the segmentation that carries the annotations and that will be annotatable in sgsTEI. */
	private static final String PROP_MAIN_SEG = "main.seg";
	/** This property provides the name of the segmentation aligned with the main segmentation. It's annotations will be ignored. This property is optional, no value will result in a single segmentation without alternations. */
	private static final String PROP_ALT_SEG = "alt.seg";
	/** If given, the spans on the named annotation layer will indicate token groups for utterances */
	private static final String PROP_UTTERANCE_SPAN_NAME = "utterance.span.name";
	/** If given, the presence of syntax trees is assumed and the provided node annotation value determines utterance groups by overlapped tokens. Spans will then be ignored */
	private static final String PROP_UTTERANCE_NODE_NAME = "utterance.node.name";
	/** ... the value of the annotation on the node ... */
	private static final String PROP_UTTERANCE_NODE_VALUE = "utterance.node.value";
	
	public SgsTEIExporterProperties() {
		addProperty(new PepperModuleProperty<String>(PROP_MAIN_SEG, String.class, "This property provides the name of the segmentation that carries the annotations and that will be annotatable in sgsTEI.", null, true));
		addProperty(new PepperModuleProperty<String>(PROP_ALT_SEG, String.class, "This property provides the name of the segmentation aligned with the main segmentation. It's annotations will be ignored. This property is optional, no value will result in a single segmentation without alternations.", null, false));
		addProperty(new PepperModuleProperty<String>(PROP_UTTERANCE_SPAN_NAME, String.class, "If given, the spans on the named annotation layer will indicate token groups for utterances", null, false));
		addProperty(new PepperModuleProperty<String>(PROP_UTTERANCE_NODE_NAME, String.class, "", "cat", false)); //FIXME provide appropriate description
		addProperty(new PepperModuleProperty<String>(PROP_UTTERANCE_NODE_VALUE, String.class, "", null, false)); //FIXME provide appropriate description		
	}
	
	/**
	 * This method provides the name of the segmentation that carries the annotations and that will be annotatable in sgsTEI.
	 * @return
	 */
	public String getMainSegmentationName() {
		return (String) getProperty(PROP_MAIN_SEG).getValue();		
	}
	
	/** This method provides the name of the segmentation aligned with the main segmentation. It's annotations will be ignored. 
	 * This property is optional, no value will result in a single segmentation without alternations. 
	 * @return
	 */
	public String getAlternateSegmentationName() {
		Object value = getProperty(PROP_ALT_SEG).getValue();
		return value == null? null : (String) value;
	}
	
	public String getUtteranceSpanName() {
		Object value = getProperty(PROP_UTTERANCE_SPAN_NAME).getValue();
		return value == null? null : (String) value;
	}
	
	public String getUtteranceNodeName() {
		return (String) getProperty(PROP_UTTERANCE_NODE_NAME).getValue();
	}
	
	public String getUtteranceNodeValue() {
		Object value = getProperty(PROP_UTTERANCE_NODE_VALUE).getValue();
		return value == null? null : (String) value;
	}	
}
