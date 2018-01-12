package org.corpus_tools.pepperModules.sgsTEIModules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleDataException;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.pepperModules.sgsTEIModules.SgsTEIImporterUtils.READ_MODE;
import org.corpus_tools.pepperModules.sgsTEIModules.SgsTEIImporterUtils.TextBuffer;
import org.corpus_tools.pepperModules.sgsTEIModules.builders.GraphBuilder;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.core.SMetaAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

public class SgsTEI2SaltMapper extends PepperMapperImpl implements SgsTEIDictionary{
	
	/**
	 * {@inheritDoc}
	 */
	public DOCUMENT_STATUS mapSDocument() {
		if (getDocument() == null) {
			setDocument(SaltFactory.createSDocument());
		}
		getDocument().setDocumentGraph( SaltFactory.createSDocumentGraph() );
		SgsTEIReader reader = new SgsTEIReader();
		this.readXMLResource(reader, getResourceURI());
		return (DOCUMENT_STATUS.COMPLETED);
	}
		
	/**
	 * This sub class is the mapper's callback handler processing the input xml.
	 * @author klotzmaz
	 *
	 */
	private class SgsTEIReader extends DefaultHandler2{		
		/** This constant is used for representing empty tokens on the syntactical token level */
		private static final String EMPTY_VALUE = "∅";
		/** This string provides the format to explicitly mark when the fallback is used for syntactical tokens */
		private static final String F_FALLBACK_TEMPLATE = "{%s}";
		/** The name of the utterance token level (to be prefixed with speaker right now) */
		private static final String UTT_NAME = "utterance";
		/** The annotation name for the transliteration annotation (utterance annotation) */
		private static final String NAME_TRANSLATION = "transliteration";
		/** Error message when fallback annotation does not exist */
		private static final String ERR_MSG_FALLBACK = "Fallback annotation does not seem to exist.";
		/** logger */
		private final Logger logger = LoggerFactory.getLogger(SgsTEIReader.class);		
		/** This is the element stack representing the hierarchy of elements to be closed. */
		private Stack<String> stack;
		/** This variable is keeping track of read characters */
		private final TextBuffer textBuffer;
		/** current state of reader */
		private READ_MODE mode; 
		/** the graph builder object connected to the current document */
		private GraphBuilder builder;
		/** additional utils for reading */
		private SgsTEIImporterUtils utils;
		/** currently processed annotation name (most recently read) */
		private String annotationName;
		/** most recently read (general) id */
		private String currentId;
		/** currently associated speaker */
		private String speaker;
		/** mapping from tokenId to token text (all tokenization levels, global map) */
		private Map<String, String> token2text;
		/** follows the span sequence to determine the placing of empty nodes */
		private Queue<Pair<String, String>> spanSeq;
		/** This is later used to build the timeline relations, one index in the list is one timestep.
		 *  The map maps from level name to token id. For one timestep, one level can have several ids
		 *  to allow two tokens on one level being overlapped by only one on another etc. */
		private List<Map<String, List<String>>> sequence;
		/** maps from token id to morphosyntax span id to later associate the syntactic tokens with the right speaker */
		private Map<String, Collection<String>> comesWith;
		/** maps annotation ids to their target node ids */
		private Map<String, String> anaId2targetId;
		/** This array determines, how the text buffer is being written on. Modes: 
		 * {0}: read as norm only 
		 * {1}: read as dipl only
		 * {0, 1}: read as norm and dipl value
		 *  */
		private int[] bufferMode;
		/** current overlap state, computed at utterance start (does new utterance overlap with old?). */
		private boolean overlap;
		/** maps a timeslot id to the given textual value (can be evaluated with function {@link SgsTEIReader::getTimeValue}) */
		private Map<String, String> code2time;
		/** temporal end value of last utterance */
		private Long lastEnd;
		/** value to be given to utterance token (translation or id) */
		private String utteranceValue;
		/** most recently read utterance id */
		private String uid;
		/** all token ids to be associated with the currently read utterance */
		private List<String> utteranceTokens;
		/** values of features given in shift tags */
		private Map<String, String> featureValues;
		/** index in overall sequence (value) when feature shift for given feature (key) happened */
		private Map<String, Integer> featureSpanStart;
		/** overall sequence of syntactic token ids */
		private List<String> overallSequence;
		/** Fallback value for syntactic subtokens. Taken from property. */
		private String fallbackName;
		
		/** name suffix for norm level */
		private String NORM;
		/** name suffix for diplomatic level */
		private String DIPL;
		/** name suffix for pause level */
		private String PAUSE;
		/** name suffix for syntactic tokenization */
		private String SYN;
		
		public SgsTEIReader() {
			/*internal*/
			utils = new SgsTEIImporterUtils();
			stack = new Stack<String>();
			textBuffer = utils.getTextBuffer();
			mode = READ_MODE.BLIND;
			builder = new GraphBuilder(SgsTEI2SaltMapper.this);
			token2text = new HashMap<>();
			sequence = new ArrayList<>();
			comesWith = new HashMap<>();
			anaId2targetId = new HashMap<>();
			bufferMode = new int[] {0, 1};
			spanSeq = new LinkedList<>();
			code2time = new HashMap<>();
			lastEnd = 0L;
			utteranceValue = null;
			uid = null;
			featureValues = new HashMap<>();
			featureSpanStart = new HashMap<>();
			overallSequence = new ArrayList<>();
			init();
		}
		
		private void init() {
			SgsTEIImporterProperties props = (SgsTEIImporterProperties) getProperties();
			DIPL = props.getDiplName();
			NORM = props.getNormName();
			SYN = props.getSynSegName();
			PAUSE = props.getPauseName();
			fallbackName = props.getFallbackAnnotationName();
		}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			localName = qName.substring(qName.lastIndexOf(":") + 1);
			if (TAG_W.equals(localName)) {
				currentId = attributes.getValue(String.join(":", NS_XML, ATT_ID));
			}
			else if (TAG_ADD.equals(localName) && READ_MODE.TEXT.equals(mode)) {
				bufferMode = new int[] {0};
			}
			else if (TAG_PAUSE.equals(localName) && READ_MODE.TEXT.equals(mode)) {
				String pauseValue = attributes.getValue(ATT_TYPE);
				if (pauseValue == null) {
					pauseValue = attributes.getValue(ATT_DURATION);
				}
				token2text.put(builder.registerToken(null, speaker, PAUSE), pauseValue);
			}
			else if (TAG_U.equals(localName) && READ_MODE.TEXT.equals(mode)) {
				utterance(attributes);
			}
			else if (TAG_SPAN.equals(localName)) {
				String targetId = attributes.getValue(ATT_TARGET);
				targetId = targetId == null? null : targetId.substring(1);
				if (READ_MODE.REFERENCE.equals(mode)) {
					builder.registerReferringExpression(attributes.getValue( String.join(":", NS_XML, ATT_ID) ), attributes.getValue(ATT_TARGET).substring(1));
				}
				else if (READ_MODE.MORPHOSYNTAX.equals(mode)) {
					String synId = attributes.getValue(String.join(":", NS_XML, ATT_ID));					
					String anaId = attributes.getValue(ATT_ANA);
					if (anaId != null) {
						anaId2targetId.put(anaId.substring(1), synId);
					}
					spanSeq.add( Pair.of(synId, targetId) );
					if (targetId != null) {
						if (!comesWith.containsKey(targetId)) {
							comesWith.put(targetId, new LinkedHashSet<String>());
						}
						comesWith.get(targetId).add(synId);
					}
				}
			}
			else if (TAG_SEG.equals(localName)) {
				if (attributes.getValue(String.join(":", NS_XML, ATT_LANG)) != null) {
					//i.e. what follows is a transliteration of the last mention tokens
					mode = READ_MODE.TRANSLITERATION;
				}
			}
			else if (TAG_STANDOFF.equals(localName)) {
				mode = READ_MODE.getMode(attributes.getValue(ATT_TYPE));
				textBuffer.clear(0);
				textBuffer.clear(1);
			}
			else if (TAG_SYMBOL.equals(localName) || TAG_NUMERIC.equals(localName)) {
				if (TAG_F.equals(stack.peek())) {
					String annotationValue = attributes.getValue(ATT_VALUE);
					builder.registerAnnotation(anaId2targetId.get(currentId), annotationName, annotationValue, isSpeakerSensitive());
					if (fallbackName.equals(annotationName)) {
						token2text.put(anaId2targetId.get(currentId), String.format(F_FALLBACK_TEMPLATE, annotationValue));
					}
				}
			}
			else if (TAG_F.equals(localName)) {
				annotationName = attributes.getValue(ATT_NAME);
			}
			else if (TAG_FS.equals(localName)) {
				currentId = attributes.getValue(String.join(":", NS_XML, ATT_ID));
			}
			else if (TAG_INTERP.equals(localName)) {
				String id = attributes.getValue(String.join(":", NS_XML, ATT_ID));
				String anaId = attributes.getValue(ATT_ANA).substring(1);
				String instId = attributes.getValue(ATT_INST);
				instId = instId == null? instId : instId.substring(1);
				anaId2targetId.put(anaId, id);
				if (READ_MODE.REFERENCE.equals(mode)) {
					String[] instances = instId.split(" ");
					for (int i = 1; i < instances.length; i++) {
						instances[i] = instances[i].substring(1);
					}
					anaId2targetId.put(anaId, id);
					builder.registerDiscourseEntity(id, instances);
				}
				else if (READ_MODE.SYNTAX.equals(mode)) {
					builder.registerSyntaxNode(id, instId);
				}
			}
			else if (TAG_LINK.equals(localName)) {
				String[] targetSource = attributes.getValue(ATT_TARGET).split(" ");
				if (READ_MODE.SYNTAX.equals(mode)) {
					builder.registerSyntaxLink(attributes.getValue(String.join(":", NS_XML, ATT_ID)), attributes.getValue(ATT_TYPE), targetSource[1].substring(1), targetSource[0].substring(1));
				}
				else if (READ_MODE.REFERENCE.equals(mode)) {
					builder.registerReferenceLink(attributes.getValue(String.join(":", NS_XML, ATT_ID)), attributes.getValue(ATT_TYPE), targetSource[1].substring(1), targetSource[0].substring(1));
				}
			}
			else if (TAG_SHIFT.equals(localName) && READ_MODE.TEXT.equals(mode)) {
				String feature = attributes.getValue(ATT_FEATURE);				
				if (feature != null) {
					feature(feature, attributes.getValue(ATT_NEW));
				}
			}
			else if (TAG_WHEN.equals(localName) && READ_MODE.TEXT.equals(mode)) {
				String id = attributes.getValue(String.join(":", NS_XML, ATT_ID));
				code2time.put(id, attributes.getValue(ATT_ABSOLUTE));
			}
			else if (TAG_DESC.equals(localName) && TAG_VOCAL.equals(stack.peek())) {
				bufferMode = new int[] {1};
			}
			else if (TAG_SIC.equals(localName) && READ_MODE.TEXT.equals(mode)) {
				bufferMode = new int[] {1};
			}
			else if (TAG_TEXT.equals(localName)) {
				normalizeTimes();
				mode = READ_MODE.TEXT;
				textBuffer.clear(0);
				textBuffer.clear(1);
			}
			else if (TAG_LISTPERSON.equals(localName)) {
				annotationName = attributes.getValue(ATT_TYPE);
			}
			else if (TAG_PERSON.equals(localName) && TAG_LISTPERSON.equals(stack.peek())) {
				String personId = attributes.getValue( String.join(":", NS_XML, ATT_ID) );
				SMetaAnnotation meta = getDocument().getMetaAnnotation(annotationName);
				if (meta == null) {
					getDocument().createMetaAnnotation(null, annotationName, personId);
				} else {
					meta.setValue( String.join(", ", meta.getValue_STEXT(), personId) );
				}
				annotationName = null;
			}
			stack.push(localName);
		}
		
		private void finishFeatures() {
			for (Entry<String, Integer> f : featureSpanStart.entrySet()) {
				buildFeature(f.getKey(), featureValues.get(f.getKey()), overallSequence.subList(f.getValue(), overallSequence.size()));
			}
		}
		
		private void buildFeature(String feature, String value, List<String> tokenIds) {
			String spanId = builder.registerSpan(null, tokenIds);
			builder.registerAnnotation(spanId, feature, value, isSpeakerSensitive());
		}
		
		private void feature(String feature, String value) {
			if (featureSpanStart.containsKey(feature)) {
				buildFeature(feature, featureValues.get(feature), overallSequence.subList(featureSpanStart.get(feature), overallSequence.size()));
			}
			featureSpanStart.put(feature, overallSequence.size());
			featureValues.put(feature, value);
		}
		
		private void utterance(Attributes attributes) {
			speaker = attributes.getValue(ATT_WHO).substring(1);
			long start = getTimeValue( attributes.getValue(ATT_START).substring(1) );
			overlap = start < lastEnd;
			lastEnd = getTimeValue( attributes.getValue(ATT_END).substring(1) );
			uid = attributes.getValue(String.join(":", NS_XML, ATT_ID));
			utteranceTokens = new ArrayList<>();
			String translation = attributes.getValue(ATT_TRANS);
			if (translation != null) {
				builder.registerAnnotation(uid, NAME_TRANSLATION, translation, isSpeakerSensitive());				
			}
			builder.registerAnnotation(uid, ATT_WHO, speaker, isSpeakerSensitive());
		}
		
		/* warning: this method should always concatenate, since sometimes several calls are used for text-node (built in multiple steps) */
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {			
			String next = (new String(Arrays.copyOfRange(ch, start, start + length))).trim();
			textBuffer.append(next, bufferMode);		
		}
		
		/** 
		 * This method turns a given timeslot id in a parse {@link Long} value.
		 * @param timeslotId
		 * @return
		 */
		private long getTimeValue(String timeslotId) {
			return Long.parseLong( code2time.get(timeslotId).replaceAll("\\.|:", "") );
		}
		
		/**
		 * This method makes sure, all read timeslot time values have the same number of digits.
		 */
		private void normalizeTimes() {
			int w = 0;
			for (String val : code2time.values()) {
				if (val != null) {
					w = Math.max(w, val.substring(val.indexOf(".")).length());
				}
			}
			for (Entry<String, String> e : code2time.entrySet()) {
				String k = e.getKey();
				String v = e.getValue();
				int we = v.substring(v.indexOf(".")).length(); 
				if (we < w) {
					code2time.put(k, v + StringUtils.repeat('0', w - we));
				}
			}
		}
		
		/**
		 * Returns the current reader state w.r.t. processing annotations.
		 * @return
		 */
		private boolean isSpeakerSensitive() {
			return false;
		}
		
		/**
		 * This method is called when enough information for (a) token(s) is collected.
		 * @param id can be null
		 * @param speaker
		 * @param dipl write on diplomatic level
		 * @param norm write on norm level (if dipl and norm are false, the token will be considered a pause token)
		 * @param value if given, buffer is ignored and not deleted
		 */
		private void tokenDetected(String id, String speaker, boolean dipl, boolean norm, String value) {
			if (!READ_MODE.TEXT.equals(mode)) {
				throw new PepperModuleException();
			}
			Map<String, List<String>> timestep = overlap? sequence.get(sequence.size() - 1) : new HashMap<String, List<String>>();
			boolean pause = !dipl && !norm;
			String emptyId = checkForEmpty(id);
			if (emptyId != null) {
				registerToken(emptyId, speaker, SYN, EMPTY_VALUE, timestep);
				sequence.add(timestep);
				timestep = new HashMap<>();
			}
			if (pause) {
				registerToken(null, speaker, PAUSE, value, timestep);
			} else {
				String normValue = textBuffer.clear(0);
				if (dipl) {
					registerToken(null, speaker, DIPL, textBuffer.clear(1), timestep);
				}
				if (norm) {
					registerToken(id, speaker, NORM, normValue, timestep);
				}
				if (id != null && comesWith.containsKey(id)) {					
					String qName = builder.getQName(speaker, SYN);
					if (!timestep.containsKey(qName)) {
						timestep.put(qName, new ArrayList<String>());
					}
					List<String> synchronousIds = timestep.get(qName);
					boolean useAnnotation = comesWith.get(id).size() > 1;
					for (String synTokenId : comesWith.get(id)) {
						String regId = builder.registerToken(synTokenId, speaker, SYN);
						{
							/* this little section influences the naming of syntactic SUBtokens */
							if (!useAnnotation) {
								token2text.put(regId, normValue);
							}
							if (!token2text.containsKey(regId)) {
								// was not yet registers thus needs a dummy value (that should indicate that something went wrong)
								logger.error(ERR_MSG_FALLBACK);
								throw new PepperModuleDataException(SgsTEI2SaltMapper.this, ERR_MSG_FALLBACK);
							}
						}
						synchronousIds.add(regId);
					}
					add2Sequences(synchronousIds);
				}
			}
			sequence.add(timestep);
			overlap = false;			
		}
		
		/** This method enqueues token ids to collecting variables */
		private void add2Sequences(List<String> ids) {
			utteranceTokens.addAll(ids);
			overallSequence.addAll(ids);
		}
		
		/** 
		 * Determines if an empty token should be inserted first
		 * @param id
		 * @return id of empty span to be created, else null
		 */
		private String checkForEmpty(String id) {
			Pair<String, String> spanIds = spanSeq.poll();
			if (spanIds == null || spanIds.getRight() != null) {
				return null;
			} else {
				return spanIds.getLeft();
			}
		}
		
		/** 
		 * This method registers a token properly and stores all information in the connected variables.
		 * @param id
		 * @param speaker
		 * @param level
		 * @param text
		 * @param timestep
		 */
		private void registerToken(String id, String speaker, String level, String text, Map<String, List<String>> timestep) {
			String id_ = builder.registerToken(id, speaker, level);
			token2text.put(id_, text);
			String qName = builder.getQName(speaker, level);
			if (!timestep.containsKey(qName)) {
				timestep.put(qName, new ArrayList<String>());
			}
			timestep.get(qName).add(id_);
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			localName = qName.substring(qName.lastIndexOf(":") + 1);		
			stack.pop();
			if (TAG_W.equals(localName) && READ_MODE.TEXT.equals(mode)) {
				tokenDetected(currentId, speaker, true, true, null);				
			}
			else if (TAG_PC.equals(localName) && READ_MODE.TEXT.equals(mode)) {
				tokenDetected(null, speaker, true, true, null);
			}
			else if (TAG_ADD.equals(localName)) {
				bufferMode = new int[] {0, 1};
			}
			else if (TAG_DESC.equals(localName) && TAG_VOCAL.equals(stack.peek()) && READ_MODE.TEXT.equals(mode)) {
				tokenDetected(null, speaker, true, false, null);
				bufferMode = new int[] {0, 1};
			}
			else if (TAG_SIC.equals(localName) && READ_MODE.TEXT.equals(mode)) {
				bufferMode = new int[] {0};
			}
			else if (TAG_F.equals(localName)) {
				annotationName = null;
			}
			else if (TAG_FS.equals(localName)) {
				currentId = null;
			}
			else if (TAG_STRING.equals(localName)) {				
				if (TAG_F.equals(stack.peek())) {
					String annotationValue = textBuffer.clear(0);
					builder.registerAnnotation(anaId2targetId.get(currentId), annotationName, annotationValue, isSpeakerSensitive());
					if (fallbackName.equals(annotationName)) {
						token2text.put(anaId2targetId.get(currentId), String.format(F_FALLBACK_TEMPLATE, annotationValue));
					}
				}
			}
			else if (TAG_CHOICE.equals(localName) && READ_MODE.TEXT.equals(mode)) {
				bufferMode = new int[] {0, 1};
			}
			else if (TAG_REG.equals(localName)) {
				textBuffer.clear(0);
				textBuffer.clear(1);
			}
			else if (TAG_SEG.equals(localName)) {
				if (READ_MODE.TRANSLITERATION.equals(mode)) {
					utteranceValue = textBuffer.clear(0);
					textBuffer.clear(1);
					mode = READ_MODE.TEXT;
				}
			}
			else if (TAG_U.equals(localName) && READ_MODE.TEXT.equals(mode)) {				
				uid = builder.registerUtterance(uid, utteranceTokens, speaker, UTT_NAME);
				token2text.put(uid, utteranceValue == null? uid : utteranceValue);
				utteranceValue = null;
			}
			else if (TAG_TITLE.equals(localName)) {
				getDocument().createMetaAnnotation(null, TAG_TITLE, textBuffer.clear(1));
			}
			else if (TAG_TEI.equals(localName)) {
				builder.setGlobalEvaluationMap(token2text);
				finishFeatures();
				builder.build(sequence);
			}
		}
	}	
}