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
import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;

public class SgsTEIImporterProperties extends PepperModuleProperties {
	/** This property determines the diplomatic layer's name. */
	private static final String PROP_DIPL_NAME = "dipl.name";
	/** This property determines the normed layer's name. */
	private static final String PROP_NORM_NAME = "norm.name";
	/** This property determines the pause layer's name. */
	private static final String PROP_PAUSE_NAME = "pause.name";
	/** This property determines the syntactic segmentation's name. */
	private static final String PROP_SYN_NAME = "syn.name";	
	/** This property determines the fallback annotation, when more than one syntactic token overlap one token on norm level. Default is "lemma". */
	private static final String PROP_SYN_FALLBACK_ANNO = "syn.fallback.anno";
	/** This property defines how to behave in case an unknown feature is observed. Given the property is true, the unknown feature will be ignored, if false an error will be raised. */
	private static final String PROP_IGNORE_UNKNOWN_FEATURES = "ignore.unknown.features";
	/** This property defines the text value for empty tokens on the syntactic token level. Default is ∅.*/
	private static final String PROP_EMPTY_TEXT_VALUE = "empty.text.value";
	
	public SgsTEIImporterProperties() {
		addProperty(new PepperModuleProperty<String>(PROP_DIPL_NAME, String.class, "This property determines the diplomatic layer's name.", "dipl", false));
		addProperty(new PepperModuleProperty<String>(PROP_NORM_NAME, String.class, "This property determines the normed layer's name.", "norm", false));
		addProperty(new PepperModuleProperty<String>(PROP_PAUSE_NAME, String.class, "This property determines the pause layer's name.", "pause", false));
		addProperty(new PepperModuleProperty<String>(PROP_SYN_NAME, String.class, "This property determines the syntactic segmentation's name.", "syn", false));
		addProperty(new PepperModuleProperty<String>(PROP_SYN_FALLBACK_ANNO, String.class, "This property determines the fallback annotation, when more than one syntactic token overlap one token on norm level. Default is \"lemma\".", "lemma", false));
		addProperty(new PepperModuleProperty<Boolean>(PROP_IGNORE_UNKNOWN_FEATURES, Boolean.class, "This property defines how to behave in case an unknown feature is observed. Given the property is true, the unknown feature will be ignored, if false an error will be raised.", false, false));
		addProperty(new PepperModuleProperty<String>(PROP_EMPTY_TEXT_VALUE, String.class, "This property defines the text value for empty tokens on the syntactic token level. Default is ∅.", "∅", false));
	}
	
	/**
	 * Get the name for the diplomatic segmentation layer.
	 * @return
	 */
	public String getDiplName() {
		return (String) getProperty(PROP_DIPL_NAME).getValue();
	}
	
	/**
	 * Get the name for the normed segmentation layer.
	 * @return
	 */
	public String getNormName() {
		return (String) getProperty(PROP_NORM_NAME).getValue();
	}
	
	/**
	 * Get the name for the pause segmentation layer.
	 * @return
	 */
	public String getPauseName() {
		return (String) getProperty(PROP_PAUSE_NAME).getValue();
	}

	/**
	 * Get the name for the syntactic segmentation layer.
	 * @return
	 */
	public String getSynSegName() {
		return (String) getProperty(PROP_SYN_NAME).getValue();
	}
	
	/**
	 * Returns the annotation name to fill in syntactical subtokens.
	 * @return
	 */
	public String getFallbackAnnotationName() {
		return (String) getProperty(PROP_SYN_FALLBACK_ANNO).getValue();
	}
	
	/**
	 * This method determines whether or not to ignore unknown features observed
	 * while reading the tokenization.
	 * @return
	 */
	public boolean ignoreUnknownFeatures() {
		return (Boolean) getProperty(PROP_IGNORE_UNKNOWN_FEATURES).getValue();
	}

	public String getEmptyTextValue() {
		return (String) getProperty(PROP_EMPTY_TEXT_VALUE).getValue();
	}
}
