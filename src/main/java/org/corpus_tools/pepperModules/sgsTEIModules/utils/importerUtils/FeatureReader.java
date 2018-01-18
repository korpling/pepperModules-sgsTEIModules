package org.corpus_tools.pepperModules.sgsTEIModules.utils.importerUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;
import org.corpus_tools.pepperModules.sgsTEIModules.builders.GraphBuilder;
import org.corpus_tools.pepperModules.sgsTEIModules.lib.SgsTEIDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureReader implements SgsTEIDictionary {
	private static final Logger logger = LoggerFactory.getLogger(FeatureReader.class);	
	private static final String F_ERR_UNKNOWN_FEATURE = "The provided feature is unknown: %s.";
	private static final String F_WARN_UNKNOWN_FEATURE = "Unknown feature observed: %s. The feature will be ignored.";
	private static final String FF_WARN_UNKNOWN_FEATURE_VAL = "Unknown value for feature <%s> observed: %s.";
	private static final String F_DEBUG_FEATURE_OBSERVED = "Feature observed: %s.";
	/** maps feature names to currently active targets */
	private Map<String, Feature> features;
	/** graph builder object */
	private GraphBuilder builder;
	/** ignore unknown features or raise error */
	private boolean ignoreUnknownFeatures;
	
	public FeatureReader(GraphBuilder builder, boolean ignoreUnknownFeatures) {
		this.builder = builder;
		features = new HashMap<>();
		for (int i = 0; i < FEAT_NAMES.length; i++) {
			features.put(FEAT_NAMES[i], new Feature(FEAT_NAMES[i], FEAT_VALUES[i], FEAT_VALUES[i][FEAT_DEFAULT_INDICES[i]]));
		}
		this.ignoreUnknownFeatures = ignoreUnknownFeatures;
	}
	
	public void readId(String id) {
		for (String feature : FEAT_NAMES) {
			readId(feature, id);
		}
	}
	
	public void readId(String featureName, String targetId) {
		Feature feature = features.get(featureName);
		if (feature != null) {
			feature.readId(targetId);
			if (logger.isDebugEnabled()) {
				logger.debug(String.format(F_DEBUG_FEATURE_OBSERVED, featureName));
			}
		} else {
			warnUnknownFeature(featureName);
		}
	}
	
	public void readFeature(String featureName, String value) {
		Feature feature = features.get(featureName);
		if (feature != null) {
			feature.setValue(value);
		}
	}
	
	private GraphBuilder getBuilder() {
		return builder;
	}
	
	private void warnUnknownFeature(String feature) {
		if (!ignoreUnknownFeatures) {
			throw new NotImplementedException(String.format(F_ERR_UNKNOWN_FEATURE, feature));
		} else {
			logger.warn(String.format(F_WARN_UNKNOWN_FEATURE, feature));
		}
	}
	
	private void warnUnknownFeatureValue(String feature, String value) {
		logger.warn(String.format(FF_WARN_UNKNOWN_FEATURE_VAL, feature, value));
	}
	
	private class Feature {
		private String name;
		private Set<String> values;
		private String value;
		private String defaultValue;
		private IdBuffer idBuffer;
		private Feature(String name, String[] values, String defaultValue) {
			this.name = name;
			this.values = new HashSet<>( Arrays.asList(values) );
			this.defaultValue = defaultValue;
			if (!this.values.contains(defaultValue)) {
				warnUnknownFeatureValue(name, defaultValue);
			}
			idBuffer = new IdBuffer();
			setValue(this.defaultValue);
		}
		
		private Feature(String name, String value, String[] values, String defaultValue) {
			this(name, values, defaultValue);			
			setValue(value);
		}
		
		private void setValue(String value) {
			if (!values.contains(value)) {
				warnUnknownFeatureValue(name, value);
			}
			flush();
			this.value = value;			
		}
		
		private void flush() {
			if (!idBuffer.isEmpty()) {
				String id = getBuilder().registerSpan(null, idBuffer.clear());
				getBuilder().registerAnnotation(id, getName(), getValue(), false);
			}
		}
		
		private String getValue() {
			return value;
		}
		
		private String getName() {
			return name;
		}
		
		private void readId(String id) {
			idBuffer.append(id);
		}
	}

	public void flush() {
		for (Feature feature : features.values()) {
			feature.flush();
		}
	}
}
