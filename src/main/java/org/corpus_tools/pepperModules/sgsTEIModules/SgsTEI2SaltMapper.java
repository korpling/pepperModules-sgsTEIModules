package org.corpus_tools.pepperModules.sgsTEIModules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;

import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SDominanceRelation;
import org.corpus_tools.salt.common.SOrderRelation;
import org.corpus_tools.salt.common.SStructure;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SProcessingAnnotation;
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
		SDocumentGraph docGraph = SaltFactory.createSDocumentGraph();
		getDocument().setDocumentGraph(docGraph);
		SgsTEIReader reader = new SgsTEIReader();
		System.out.println(getResourceURI());
		this.readXMLResource(reader, getResourceURI());
		return (DOCUMENT_STATUS.COMPLETED);
	}
	
	private SgsTEIImporterProperties getModuleProperties() {
		return (SgsTEIImporterProperties) getProperties();
	}
	
	/**
	 * This sub class is the mapper's callback handler processing the input xml.
	 * @author klotzmaz
	 *
	 */
	private class SgsTEIReader extends DefaultHandler2{
		
		private final Logger logger = LoggerFactory.getLogger(SgsTEIReader.class);
		
		/** This is the element stack representing the hierarchy of elements to be closed. */
		private Stack<String> stack;
		/** This variable is keeping track of read characters */
		private TextBuffer textBuffer;
		/** what are we currently reading? */
		private READ_MODE mode; 
		
		/*here variables for collecting information*/	
		/** this maps time slot ids to actual times */
		private HashMap<String, Long> id2TimeMap;
		/** text tracker, list of triples (tokenId, diplValue, normValue), id==null means pause; array list keeps order */
		private ArrayList<TextSegment> textTracker;
		/** speaker and time tracker, each entry is (SPEAKER_NAME, index of last token in utterance, (START-TIME, END-TIME)) */
		private ArrayList<MutableTriple<String, Integer, Pair<String, String>>> speakerTimeTracker;
		/** this variable maps speaker names to STextualDSs-triples (dipl, norm, pause)*/
		private HashMap<String, Triple<STextualDS, STextualDS, STextualDS>> speaker2DSMap;
		/**mapping from markable-id to List of annotations*/
		private HashMap<String, List<SAnnotation>> morphosyntax;
		/**mapping from tokens to markables*/
		private HashMap<String, String> tokenId2markableAnaId; //FIXME this is buggy, one token can have several analyses
		/*WORKAROUND, this does not fix it, but helps for now for syntax*/
		HashMap<String, String> analysisId2tokenId = new HashMap<>(); //FIXME
		/**variable keeping track of last markables annotation list, last annotation can simple be taken from end of the list*/
		private List<SAnnotation> currentAnnotations;
		/** mapping between markableId and markableAnaId */
		private HashMap<String, String> markable2ana;
		
		/** this variable collects the syntax structures; map id 2 structure */
		private HashMap<String, SStructure> nodeId2Structure;
		/** this variable collects the link relations between syntax nodes, mapping from source node to list of pairs (target, function) */
		private HashMap<String, List<Pair<String, String>>> syntaxRelations;
		/** this variable collects the node (not link) annotations, maps ana-ID to annotation */
		private HashMap<String, List<SAnnotation>> nodeAnnotations;
		/** syntax layer to facilitate adding the nodes and relations */
		private SLayer syntaxLayer;
		
		/** records the timeline inside the document */
		private HashMap<String, Long> timeslotId2Time;
		
		
		public SgsTEIReader() {
			stack = new Stack<String>();
			textBuffer = new TextBuffer();
			mode = READ_MODE.BLIND;
			
			textTracker = new ArrayList<>();
			morphosyntax = new HashMap<>();
			tokenId2markableAnaId = new HashMap<>();
			id2TimeMap = new HashMap<>();
			speakerTimeTracker = new ArrayList<>();
			nodeId2Structure = new HashMap<>();
			syntaxRelations = new HashMap<>();
			nodeAnnotations = new HashMap<>();
			markable2ana = new HashMap<>();
			timeslotId2Time = new HashMap<>();
			speaker2DSMap = new HashMap<>();
			
			syntaxLayer = SaltFactory.createSLayer();			
			syntaxLayer.setName(TYPE_SYNTAX);
			getDocument().getDocumentGraph().addLayer(syntaxLayer);
			
		}
		
		private void debugMessage(Object... elements) {
			String[] elems = new String[elements.length];
			for (int i = 0; i < elements.length; i++) {
				elems[i] = elements[i] == null? "null" : (String) elements[i];
			}
			System.out.println(String.join(" ", elems));
		}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			localName = qName.substring(qName.lastIndexOf(":") + 1);
			if (TAG_W.equals(localName)) {
				if (READ_MODE.TEXT.equals(mode)) {
					String tokenId = attributes.getValue(String.join(":", NS_XML, ATT_ID));
					boolean isCorrection = TAG_CORR.equals(stack.peek());
					if (isCorrection) {
						getCurrentToken().setId(tokenId);
					}
					else {
						newToken(tokenId);
					}
				}
			}
			else if (TAG_ADD.equals(localName) && READ_MODE.TEXT.equals(mode)) {
				writeBufferToToken(true, true, false);
			}
			else if (TAG_PAUSE.equals(localName) && READ_MODE.TEXT.equals(mode)) {
				String value = attributes.getValue(ATT_DURATION);
				if (value == null) {
					value = attributes.getValue(ATT_TYPE);
				}
				newToken(null).setPause(value);				
			}
			else if (TAG_PC.equals(localName) && READ_MODE.TEXT.equals(mode)) {
				newToken(null);				
			}
			else if (TAG_U.equals(localName)) {
				if (READ_MODE.TEXT.equals(mode)) {					
					String startTimeCode = attributes.getValue(ATT_START).substring(1);
					String endTimeCode = attributes.getValue(ATT_END).substring(1);
					String speakerCode = attributes.getValue(ATT_WHO).substring(1);
					newUtterance(speakerCode, startTimeCode, endTimeCode);					
				}
			}
			else if (TAG_SPAN.equals(localName)) {
				if (READ_MODE.MORPHOSYNTAX.equals(mode)) {
					String targetValue = attributes.getValue(ATT_TARGET);
					String anaValue = attributes.getValue(ATT_ANA);
					if (targetValue != null && anaValue != null) {
						tokenId2markableAnaId.put(targetValue.substring(1), anaValue.substring(1));
						//WORKAROUNDs FIXME
						markable2ana.put(attributes.getValue(String.join(":", NS_XML, ATT_ID)), attributes.getValue(ATT_ANA).substring(1));
						analysisId2tokenId.put(attributes.getValue(ATT_ANA).substring(1), attributes.getValue(ATT_TARGET).substring(1));
					}
				}
			}
			else if (TAG_STANDOFF.equals(localName)) {
				String standoffType = attributes.getValue(ATT_TYPE);
				mode = READ_MODE.getMode(standoffType);
			}
			else if (TAG_SYMBOL.equals(localName)) {
				if (READ_MODE.MORPHOSYNTAX.equals(mode)) {
					currentAnnotations.get(currentAnnotations.size() - 1).setValue(attributes.getValue(ATT_VALUE));
				}
				else if (READ_MODE.SYNTAX.equals(mode)) {
					currentAnnotations.get(currentAnnotations.size() - 1).setValue(attributes.getValue(ATT_VALUE));
				}
			}
			else if (TAG_F.equals(localName)) {
				if (READ_MODE.MORPHOSYNTAX.equals(mode)) {
					SAnnotation anno = SaltFactory.createSAnnotation();
					anno.setName(attributes.getValue(ATT_NAME));					
					currentAnnotations.add(anno);					
				}
				else if (READ_MODE.SYNTAX.equals(mode)) {
					SAnnotation anno = SaltFactory.createSAnnotation();
					anno.setName(attributes.getValue(ATT_NAME));
					currentAnnotations.add(anno);
				}
			}
			else if (TAG_FS.equals(localName)) {
				if (READ_MODE.MORPHOSYNTAX.equals(mode)) {
					currentAnnotations = new ArrayList<SAnnotation>();
					String markableId = attributes.getValue(String.join(":", NS_XML, ATT_ID));
					morphosyntax.put(markableId, currentAnnotations);
				}
				else if (READ_MODE.SYNTAX.equals(mode)) {
					currentAnnotations = new ArrayList<SAnnotation>();
					String analysisId = attributes.getValue(String.join(":", NS_XML, ATT_ID));
					nodeAnnotations.put(analysisId, currentAnnotations);
				}
			}
			else if (TAG_INTERP.equals(localName)) {
				if (READ_MODE.SYNTAX.equals(mode)) {
					SStructure node = SaltFactory.createSStructure();
					
					SProcessingAnnotation panno = SaltFactory.createSProcessingAnnotation();					
					panno.setName(ATT_ANA);
					panno.setValue(attributes.getValue(ATT_ANA).substring(1));
					node.addProcessingAnnotation(panno);
					
					/*the following is null for root*/
					String value = attributes.getValue(ATT_INST);
					if (value != null) { 
						panno = SaltFactory.createSProcessingAnnotation();
						panno.setName(ATT_INST);
						panno.setValue(value.substring(1));
						node.addProcessingAnnotation(panno);
					}
					
					panno = SaltFactory.createSProcessingAnnotation();
					panno.setName(ATT_ID);
					String id = attributes.getValue(String.join(":", NS_XML, ATT_ID));
					panno.setValue(id);
					node.addProcessingAnnotation(panno);
					
					nodeId2Structure.put(id, node);
//					syntaxLayer.addNode(node);
				}
			}
			else if (TAG_LINK.equals(localName)) {
				if (READ_MODE.SYNTAX.equals(mode)) {
					String[] targetInfo = attributes.getValue(ATT_TARGET).split(SPACE);
					String sourceId = targetInfo[1].substring(1);
					String targetId = targetInfo[0].substring(1);
					String type = attributes.getValue(ATT_TYPE);
					if (!syntaxRelations.containsKey(sourceId) ) {
						syntaxRelations.put(sourceId, new ArrayList<Pair<String, String>>());
					}
					syntaxRelations.get(sourceId).add(Pair.of(targetId, type));				
				}
			}
			else if (TAG_WHEN.equals(localName) && READ_MODE.TEXT.equals(mode)) {
				timeslotId2Time.put(attributes.getValue(String.join(":", NS_XML, ATT_ID)), Long.parseLong(attributes.getValue(ATT_ABSOLUTE).replaceAll("\\.|:", "")));
			}
			else if (TAG_TEXT.equals(localName)) {
				mode = READ_MODE.TEXT;
			}

			stack.push(localName);
		}
		
		/* warning: this method should always concatenate, since sometimes several calls are used for text-node (built in multiple steps) */
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (READ_MODE.TEXT.equals(mode) || READ_MODE.MORPHOSYNTAX.equals(mode)) {
				String next = (new String(Arrays.copyOfRange(ch, start, start + length))).trim(); //TODO figure out if there are cases where trim should not be used;
				textBuffer.append(next);
			}
		}
		
		private void newUtterance(String speakerCode, String startTimeCode, String endTimeCode) {
			speakerTimeTracker.add(MutableTriple.of(speakerCode, 0, Pair.of(startTimeCode, endTimeCode)));
		}
		
		private TextSegment newToken(String tokenId) {
			textTracker.add(new TextSegment(tokenId, "", "", null));
			return getCurrentToken();
		}
		
		private TextSegment getCurrentToken() {
			return textTracker.get(textTracker.size() - 1);
		}
		
		private void writeBufferToToken(boolean dipl, boolean norm, boolean overwrite) {
			TextSegment lastTokenObject = getCurrentToken();
			String text = textBuffer.clear();
			if (dipl) {
				String newText = overwrite? text : lastTokenObject.getDipl().concat(text);
				lastTokenObject.setDipl(newText);
			}
			if (norm) {
				String newText = overwrite? text : lastTokenObject.getNorm().concat(text);
				lastTokenObject.setNorm(newText);
			}			
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			localName = qName.substring(qName.lastIndexOf(":") + 1);			
			stack.pop();
			if (TAG_W.equals(localName)) {
				// read mode TEXT assumed				
				/* also set the set dipl, because for add-cases dipl is also taken from w */
				boolean isCorrection = TAG_CORR.equals(stack.peek());
				writeBufferToToken(!isCorrection, true, false);
			}
			else if (TAG_PC.equals(localName)) {
				// read mode TEXT assumed
				writeBufferToToken(true, true, false);
			}
			else if (TAG_ADD.equals(localName)) {
				// read mode TEXT assumed				
				// here we DON'T add the add-text to middle, because it does not belong to dipl
				writeBufferToToken(false, true, false);
			}
			else if (TAG_DESC.equals(localName) && TAG_VOCAL.equals(stack.peek())) {
				newToken(null);
				writeBufferToToken(true, false, false);
			}
			else if (TAG_SIC.equals(localName) && READ_MODE.TEXT.equals(mode)) {
				newToken(null);
				writeBufferToToken(true, false, false);
			}
			else if (TAG_FS.equals(localName)) {
				currentAnnotations = null;
			}
			else if (TAG_STRING.equals(localName)) {
				if (READ_MODE.MORPHOSYNTAX.equals(mode)) {
					currentAnnotations.get(currentAnnotations.size() - 1).setValue(textBuffer.clear());
				}
			}
			else if (TAG_U.equals(localName)) {
				// set last token id of utterance to last read token's id
				MutableTriple<String, Integer, Pair<String, String>> speakerObject = speakerTimeTracker.get(speakerTimeTracker.size() - 1);
				int nTokens;
				if (speakerTimeTracker.size() == 1) {
					nTokens = textTracker.size() - 1;
				} else {
					nTokens = textTracker.size() - 1 - speakerTimeTracker.get(speakerTimeTracker.size() - 1).getMiddle();
				}
				speakerObject.setMiddle(nTokens);
			}
			else if (TAG_TEI.equals(localName)) {			
				buildGraph();
			}
		}
		
		/* AFTER READING */
		
		private static final String SPACE = " ";
		
		private static final String NORM = "norm";
		
		private static final String DIPL = "dipl";
		
		private static final String PAUSE = "pause";
		
		private void buildGraph() {
			SDocumentGraph docGraph = getDocument().getDocumentGraph();
			STimeline timeline = docGraph.createTimeline();
			{//create all necessary timeslots (maximum, overlaps decrease this number, but remaining timesteps are not a problem
				timeline.increasePointOfTime(textTracker.size());				
			}
			String speaker = null;
			int firstTokenIx = 0;
			int lastTokenIx;
			TextSegment tokenObject = null;
			long startsAt = 0L;
			long endedAt = 0L;
			
			HashMap<Integer, Integer> tokenIndex2timelineSlot = new HashMap<>();
			HashMap<String, ArrayList<Integer>> speaker2TokenIndexes = new HashMap<>();
			HashMap<String, Triple<StringBuilder, StringBuilder, StringBuilder>> speaker2Text = new HashMap<>();
			ArrayList<Triple<Pair<Integer, Integer>, Pair<Integer, Integer>, Pair<Integer, Integer>>> tokenLimits = new ArrayList<>();
			
			boolean overlap = false;
			int offset = 0;			
			for (MutableTriple<String, Integer, Pair<String, String>> utteranceObject : speakerTimeTracker) {
				speaker = utteranceObject.getLeft();							
				lastTokenIx = utteranceObject.getMiddle();			
				
				//init speaker
				if (!speaker2TokenIndexes.containsKey(speaker)) {
					speaker2TokenIndexes.put(speaker, new ArrayList<Integer>());
					speaker2Text.put(speaker, Triple.of(new StringBuilder(), new StringBuilder(), new StringBuilder()));	
				}
				ArrayList<Integer> tokenIndexes = speaker2TokenIndexes.get(speaker);
				Triple<StringBuilder, StringBuilder, StringBuilder> texts = speaker2Text.get(speaker);
				StringBuilder diplText = texts.getLeft();
				StringBuilder normText = texts.getMiddle();
				StringBuilder pauseText = texts.getRight();
								
				// check for overlap
				startsAt = timeslotId2Time.get(utteranceObject.getRight().getLeft());			
				overlap = startsAt < endedAt;
				if (overlap) {
					offset -= 1;
				}
				
				for (int i = firstTokenIx; i <= lastTokenIx; i++) {
					MutableTriple<Pair<Integer, Integer>, Pair<Integer, Integer>, Pair<Integer, Integer>> limitTriple = 
							MutableTriple.of(null, null, null);
					tokenLimits.add(limitTriple);
					tokenIndexes.add(i);
					tokenIndex2timelineSlot.put(i, i + offset);
					tokenObject = textTracker.get(i);
					if (tokenObject.getPause() == null) {
						//is text
						String diplT = tokenObject.getDipl();
						String normT = tokenObject.getNorm();
						if (!diplT.isEmpty()) {
							limitTriple.setLeft(Pair.of(diplText.length(), diplText.length() + diplT.length()));
							diplText.append(diplT).append(SPACE);	
						}
						if (!normT.isEmpty()) {
							limitTriple.setMiddle(Pair.of(normText.length(), normText.length() + normT.length()));
							normText.append(normT).append(SPACE);
						}
					} else {
						//is pause
						String pauseT = tokenObject.getPause();
						limitTriple.setRight(Pair.of(pauseText.length(), pauseText.length() + pauseT.length()));
						pauseText.append(pauseT).append(SPACE);
					}					
				}				
				
				firstTokenIx = lastTokenIx + 1;
				endedAt = timeslotId2Time.get(utteranceObject.getRight().getRight());
			}
			
			HashMap<String, SToken> tokenId2SToken = new HashMap<>();
			
			ArrayList<SToken> diplTokens;
			ArrayList<SToken> normTokens;
			ArrayList<SToken> pauseTokens;
			
			for (Entry<String, Triple<StringBuilder, StringBuilder, StringBuilder>> e : speaker2Text.entrySet()) {
				speaker = e.getKey();				
				StringBuilder diplText = e.getValue().getLeft();
				StringBuilder normText = e.getValue().getMiddle();
				StringBuilder pauseText = e.getValue().getRight(); 
				
				STextualDS diplDS = docGraph.createTextualDS(diplText.toString());
				STextualDS normDS = docGraph.createTextualDS(normText.toString());
				STextualDS pauseDS = docGraph.createTextualDS(pauseText.toString());
				
				ArrayList<Integer> tokenIndexes = speaker2TokenIndexes.get(speaker);				
				
				diplTokens = new ArrayList<>();
				normTokens = new ArrayList<>();
				pauseTokens = new ArrayList<>();
				
				for (Iterator<Integer> itIx = tokenIndexes.iterator(); itIx.hasNext();) {
					int tokenIndex = itIx.next();									
					Triple<Pair<Integer, Integer>, Pair<Integer, Integer>, Pair<Integer, Integer>> limits = tokenLimits.get(tokenIndex);
					int timeslot = tokenIndex2timelineSlot.get(tokenIndex);
					SToken sTok = null;
					if (limits.getRight() == null) {
						// is text
						if (limits.getLeft() != null) {
							//create dipl token
							sTok = docGraph.createToken(diplDS, limits.getLeft().getLeft(), limits.getLeft().getRight());
							diplTokens.add(sTok);
							addTimelineRelation(sTok, timeslot, timeslot + 1);							
						}					
						if (limits.getMiddle() != null) {
							//create and store norm token
							sTok = docGraph.createToken(normDS, limits.getMiddle().getLeft(), limits.getMiddle().getRight());
							normTokens.add(sTok);
							addTimelineRelation(sTok, timeslot, timeslot + 1);
							//add annotations
							String tokId = textTracker.get(tokenIndex).getId();
							if (tokId != null) {
								tokenId2SToken.put(tokId, sTok);								
								List<SAnnotation> annotations = morphosyntax.get( tokenId2markableAnaId.get(tokId) );
								for (SAnnotation anno : annotations) {
									anno.setName(String.join(DELIMITER, speaker, anno.getName()));
									sTok.addAnnotation(anno);
								}
							}							
						}
					} else {
						//create pause token
						sTok = docGraph.createToken(pauseDS, limits.getRight().getLeft(), limits.getRight().getRight());
						pauseTokens.add(sTok);
						addTimelineRelation(sTok, timeslot, timeslot + 1);
					}
				}
				String diplName = String.join(DELIMITER, speaker, DIPL);
				String normName = String.join(DELIMITER, speaker, NORM);
				String pauseName = String.join(DELIMITER, speaker, PAUSE);
				
				diplDS.setName(diplName);
				normDS.setName(normName);
				pauseDS.setName(pauseName);
				
				addOrderRelations(diplTokens, diplName);
				addOrderRelations(normTokens, normName);
				if (docGraph.getText(pauseDS).trim().isEmpty()) {
					docGraph.removeNode(pauseDS);
				} else {
					addOrderRelations(pauseTokens, pauseName);	
				}
				
			}			
			
			/*Build syntax*/
			//TOOLS:			
			//step 1: connect all structures with an instance to their tokens
			SProcessingAnnotation panno = null;
			SStructure node = null;
			HashMap<String, SStructure> nodeId2NewStructure = new HashMap<>();
			for (Entry<String, SStructure> e : nodeId2Structure.entrySet()) {
				node = e.getValue();
				panno = node.getProcessingAnnotation(ATT_INST);
				if (panno != null) {
					// if node has an instance, connect them
					String instanceName = markable2ana.get(panno.getValue_STEXT());
					SToken targetToken = tokenId2SToken.get(analysisId2tokenId.get(instanceName));					
					SStructure struct = docGraph.createStructure(targetToken);
					for (SAnnotation anno : node.getAnnotations()) {
						struct.addAnnotation(anno);
					}
					for (SProcessingAnnotation p_anno : node.getProcessingAnnotations()) {
						struct.addProcessingAnnotation(p_anno);
					}					
					SProcessingAnnotation p_anno = struct.getProcessingAnnotation(ATT_ANA);
					if (p_anno != null) {
						List<SAnnotation> annos = nodeAnnotations.get(p_anno.getValue());
						if (annos != null) {
							for (SAnnotation anno : annos) {
								struct.addAnnotation(anno);
							}
						}					
					}
					nodeId2NewStructure.put(e.getKey(), struct);
					syntaxLayer.addNode(struct);
				}				
			}
			for (Entry<String, SStructure> e : nodeId2NewStructure.entrySet()) {
				nodeId2Structure.put(e.getKey(), e.getValue());
			}
			for (Entry<String, List<Pair<String, String>>> e : syntaxRelations.entrySet()) {
				node = nodeId2Structure.get(e.getKey());
				SStructure target;
				String type;
				SDominanceRelation domRel;
				for (Pair<String, String> relation : e.getValue()) {
					target = nodeId2Structure.get(relation.getKey());
					type = relation.getValue();
					docGraph.addNode(node);
					domRel = (SDominanceRelation) docGraph.createRelation(node, target, SALT_TYPE.SDOMINANCE_RELATION, null);					
					domRel.createAnnotation(null, ATT_TYPE, type);
					syntaxLayer.addRelation(domRel);
				}
				SProcessingAnnotation p_anno = node.getProcessingAnnotation(ATT_ANA);
				if (p_anno != null) {
					List<SAnnotation> annos = nodeAnnotations.get(p_anno.getValue());
					if (annos != null) {
						for (SAnnotation anno : annos) {
							node.addAnnotation(anno);
						}
					}
					node.removeLabel(p_anno.getQName());
				}
			}
		}		
		
		/** Delimiter used in layer names to specify layers with speaker names*/
		public static final String DELIMITER = "_";
				
		private void addOrderRelations(List<SToken> tokens, String name) {
			SDocumentGraph docGraph = getDocument().getDocumentGraph();
			SOrderRelation rel = null;
			for (int i = 0; i < tokens.size() - 1; i++) {
				rel = SaltFactory.createSOrderRelation();
				rel.setSource(tokens.get(i));
				rel.setTarget(tokens.get(i + 1));
				rel.setType(name);
				docGraph.addRelation(rel);
			}
		}
		
		private void addTimelineRelation(SToken tok, int start, int end) {
			STimeline timeline = getDocument().getDocumentGraph().getTimeline();
			STimelineRelation rel = null;
			rel = SaltFactory.createSTimelineRelation();
			rel.setSource(tok);
			rel.setTarget(timeline);
			rel.setStart(start);
			rel.setEnd(end);
			getDocument().getDocumentGraph().addRelation(rel);
		}
	}
	
	private class TextBuffer{
		private StringBuilder text;
		
		private TextBuffer() {
			text = new StringBuilder();
		}
		
		private String clear() {
			String retVal = text.toString();
			text.delete(0, text.length());
			return retVal;
		}
		
		private void append(String text) {
			this.text.append(text);
		}
	}
	
	private class TextSegment{		
		private String id;
		private String dipl;
		private String norm;
		private String pause;
		
		private TextSegment(String id, String dipl, String norm, String pause) {
			this.id = id;
			this.dipl = dipl;
			this.norm = norm;
			this.pause = pause;
		}
		
		private String getId() {
			return this.id;
		}
		
		private void setId(String id) {
			this.id = id;
		}
		
		private String getDipl() {
			return this.dipl;
		}
		
		private void setDipl(String dipl) {
			this.dipl = dipl;
		}
		
		private String getNorm() {
			return this.norm;
		}
		
		private void setNorm(String norm) {
			this.norm = norm;
		}
		
		private String getPause() {
			return this.pause;
		}
		
		private void setPause(String pause) {
			this.pause = pause;
		}
		
		/** For debug purposes*/
		@Override
		public String toString() {
			return String.join(":", getId(), getDipl(), getNorm(), getPause());
		}
	}
	
	private enum READ_MODE {
		TEXT, MORPHOSYNTAX, SYNTAX, REFERENCES, BLIND;
		
		public static READ_MODE getMode(String standoffType) {
			if (TYPE_SYNTAX.equalsIgnoreCase(standoffType)) {
				return READ_MODE.SYNTAX;
			}
			else if (TYPE_MORPHOSYNTAX.equalsIgnoreCase(standoffType)) {
				return READ_MODE.MORPHOSYNTAX;
			}
			else if (TYPE_REFERENCES.equals(standoffType)) {
				return READ_MODE.REFERENCES;
			} 
			else if (TAG_TEXT.equalsIgnoreCase(standoffType)) {
				return READ_MODE.TEXT;
			} else {
				return READ_MODE.BLIND;
			}
		}
	}
}
