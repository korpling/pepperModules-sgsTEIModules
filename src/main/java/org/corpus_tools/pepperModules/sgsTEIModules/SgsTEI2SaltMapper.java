package org.corpus_tools.pepperModules.sgsTEIModules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepperModules.sgsTEIModules.SgsTEIImporterUtils.IdMapper;
import org.corpus_tools.pepperModules.sgsTEIModules.SgsTEIImporterUtils.READ_MODE;
import org.corpus_tools.pepperModules.sgsTEIModules.SgsTEIImporterUtils.TextBuffer;
import org.corpus_tools.pepperModules.sgsTEIModules.SgsTEIImporterUtils.TextSegment;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SOrderRelation;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SStructure;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SNode;
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
		private final TextBuffer textBuffer;
		/** what are we currently reading? */
		private READ_MODE mode; 
		
		/* misc */
		/**variable keeping track of last markables annotation list, last annotation can simple be taken from end of the list*/
		private List<Pair<String, String>> currentAnnotations;
		/** this maps the analysis id to a key-value-pair. Insertion order is relevant for further processing. */
		private HashMap<String, List<Pair<String, String>>> annotations;
		
		/* primary data */
		/** text tracker, list of triples (tokenId, diplValue, normValue), id==null means pause; array list keeps order */
		private List<TextSegment> textTracker;
		/** speaker and time tracker, each entry is (SPEAKER_NAME, index of last token in utterance, (START-TIME, END-TIME)) */
		private List<MutableTriple<String, Integer, Pair<String, String>>> speakerTimeTracker;
			
		/* morphosyntax */
		/** This variable maps tokenIds to their morphosyntactic analyses (ids) */
		private Map<String, List<String>> tokenId2MoSynAnaIds;
		/** this maps a morphosyntactical annotation id to the id addressed by syntactic nodes */
		private IdMapper moSynAna2SynTargetId;
		/** this queue collects tokens mentioned as targets to detect empty nodes (collects pairs of (syntactic target id, token id) */
		private Queue<Pair<String, String>> targetSequence;
		
		/* syntax */
		/** collects the terminals and non-terminals and maps their ids to the syntactical target on the morphosyntactical level */
		private IdMapper synNodeId2synTargetId;
		/** this variable maps synTargetIds (ids of morphosyntax spans) to the syntactical {@link SToken} objects */
		private Map<String, SToken> synTargetId2SToken;
		/** this variable collects the link relations between syntax nodes, mapping from source node to list of pairs (target, function) */
		private Map<String, List<Pair<String, String>>> syntaxLinks;
		/** syntax layer to facilitate adding the nodes and relations */
		private SLayer syntaxLayer;
		/** this variable collects information about which node is pointing at a target (targetSynNodeId -> List<sourceSynNodeId>)*/
		private Map<String, Set<String>> targetId2governorIds;
		/** this finally connects the nodes after they were build, synNodeId -> SNode */
		private Map<String, SNode> synNodeId2SNode;
		/** maps synt. node. id to annotation id */
		private IdMapper synNodeId2AnaId;

		/* references */ 
		/** this variable logs the spans for referring expressions, map id->target*/
		private IdMapper referenceSpans;
		/** this tracks the interpretations, maps id to instance id*/
		private Map<String, List<String>> refInterpId2Inst;
		/** this maps instanceId to their referential analysis id */
		private IdMapper refInstId2AnaId;
		/** this variable stores the reference links; maps source to (target, function) */
		private Map<String, List<Pair<String, String>>> referenceLinks;
		
		private static final String WARN_UNKNOWN_LINKS = "Unknown links will be ignored, this might lead to errors in further processing.";
		private static final String F_ERR_MSG_SYNTAX_NODE_MISSING = "Undefined syntax node: %s";
		
		private void buildSyntax() {			
			SDocumentGraph docGraph = getDocument().getDocumentGraph();
			IdMapper synTargetId2SynNodeId = utils.reversedMapper(synNodeId2synTargetId);			
			//Build lowest layer of inner nodes
			for (Entry<String, SToken> e : synTargetId2SToken.entrySet()) {
				String synTargetId = e.getKey();
				String synNodeId = synTargetId2SynNodeId.get( synTargetId );
				SToken leaf = e.getValue();
				SStructure node = docGraph.createStructure(leaf);
				if (synNodeId != null) {
					addNodeAnnotations(node, synNodeId);
				}
			}
			debugMessage(synNodeId2SNode);
			for (Entry<String, List<Pair<String, String>>> namedLinks : syntaxLinks.entrySet()) {
				String synNodeId = namedLinks.getKey();
				SNode node = synNodeId2SNode.get(synNodeId);
				if (node == null) {
					node = createTreeNode(synNodeId);
					addNodeAnnotations(node, synNodeId);
				}		
				for (Pair<String, String> targetAndType : namedLinks.getValue()) {
					SNode targetNode;
					String targetNodeId;
					targetNodeId = targetAndType.getKey();
					targetNode = synNodeId2SNode.get(targetNodeId);
					if (targetNode == null) {
						targetNode = createTreeNode(targetNodeId);
						addNodeAnnotations(targetNode, targetNodeId);
					}
					docGraph.createRelation(node, targetNode, SALT_TYPE.SDOMINANCE_RELATION, String.join("=", "func", targetAndType.getValue()));					
				}
			}
		}
		
		private void addNodeAnnotations(SNode node, String synNodeId) {
			for (Pair<String, String> kvPair : annotations.get( synNodeId2AnaId.get( synNodeId ) )) {
				node.createAnnotation(null, kvPair.getKey(), kvPair.getValue());
			}				
			debugMessage("Putting", synNodeId, "with", node);
			synNodeId2SNode.put(synNodeId, node);
		}

		private SNode createTreeNode(String id) {			
			SNode node = SaltFactory.createSStructure();
			getDocument().getDocumentGraph().addNode(node);
			synNodeId2SNode.put(id, node);
			return node;
		}
		
		private void buildReferences(Map<String, String> sToken2speaker) {}
		
		/** records the timeline inside the document */
		private HashMap<String, Long> timeslotId2Time;
		
		private SgsTEIImporterUtils utils;
		
		public SgsTEIReader() {
			/*internal*/
			utils = new SgsTEIImporterUtils();
			stack = new Stack<String>();
			textBuffer = utils.getTextBuffer();
			mode = READ_MODE.BLIND;
			
			/*primary data*/
			textTracker = new ArrayList<>();
			tokenId2MoSynAnaIds = new HashMap<>();
			speakerTimeTracker = new ArrayList<>();			
			
			/*misc*/
			annotations = new HashMap<>();
			timeslotId2Time = new HashMap<>();
			
			/*morphosyntax*/
			moSynAna2SynTargetId = utils.createIdMapper();
			targetSequence = new LinkedList<>();
			
			/*syntax*/
			synNodeId2AnaId = utils.createIdMapper();
			syntaxLinks = new HashMap<>();
			targetId2governorIds = new HashMap<>();
			synTargetId2SToken = new HashMap<>();
			synNodeId2synTargetId = utils.createIdMapper();
			synNodeId2SNode = new HashMap<>();
			syntaxLayer = SaltFactory.createSLayer();			
			syntaxLayer.setName(TYPE_SYNTAX);
			getDocument().getDocumentGraph().addLayer(syntaxLayer);
			
			/*reference*/
			referenceSpans = utils.createIdMapper();
			refInterpId2Inst = new HashMap<>();
			refInstId2AnaId = utils.createIdMapper();
			referenceLinks = new HashMap<>();						
		}
		
		private void debugMessage(Object... elements) {
			String[] elems = new String[elements.length];
			for (int i = 0; i < elements.length; i++) {
				elems[i] = elements[i] == null? "null" : elements[i].toString();
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
					String id = attributes.getValue(String.join(":", NS_XML, ATT_ID));
					String targetValue = attributes.getValue(ATT_TARGET);
					String anaValue = attributes.getValue(ATT_ANA);
					if (targetValue != null) {
						String tgt = targetValue.substring(1);
						if (!tokenId2MoSynAnaIds.containsKey(tgt)) {
							tokenId2MoSynAnaIds.put(tgt, new ArrayList<String>());
						}
						if (anaValue != null) {
							tokenId2MoSynAnaIds.get(tgt).add( anaValue.substring(1) );
							moSynAna2SynTargetId.put(anaValue.substring(1), id);
						}
						targetSequence.add(Pair.of(id, tgt));
					} else {
						targetSequence.add(Pair.of(id, targetValue));
					}
					
				}
				else if (READ_MODE.REFERENCES.equals(mode)) {
					referenceSpans.put(attributes.getValue(String.join(":", NS_XML, ATT_ID)), attributes.getValue(ATT_TARGET).substring(1));
				}
			}
			else if (TAG_STANDOFF.equals(localName)) {
				String standoffType = attributes.getValue(ATT_TYPE);
				mode = READ_MODE.getMode(standoffType);
			}
			else if (TAG_SYMBOL.equals(localName) || TAG_NUMERIC.equals(localName)) {
				if (TAG_F.equals(stack.peek()) && (READ_MODE.MORPHOSYNTAX.equals(mode) || READ_MODE.SYNTAX.equals(mode) || READ_MODE.REFERENCES.equals(mode))) {
					currentAnnotations.get(currentAnnotations.size() - 1).setValue(attributes.getValue(ATT_VALUE));
				}
			}
			else if (TAG_F.equals(localName)) {
				if (READ_MODE.MORPHOSYNTAX.equals(mode) || READ_MODE.SYNTAX.equals(mode) || READ_MODE.REFERENCES.equals(mode)) {
					String level = attributes.getValue(ATT_NAME);
					currentAnnotations.add(MutablePair.<String, String>of(level, null));
				}
			}
			else if (TAG_FS.equals(localName)) {
				if (READ_MODE.MORPHOSYNTAX.equals(mode) || READ_MODE.SYNTAX.equals(mode) || READ_MODE.REFERENCES.equals(mode)) {
					currentAnnotations = new ArrayList<Pair<String, String>>();
					String analysisId = attributes.getValue(String.join(":", NS_XML, ATT_ID));
					annotations.put(analysisId, currentAnnotations);
				}
			}
			else if (TAG_INTERP.equals(localName)) {
				if (READ_MODE.SYNTAX.equals(mode)) {		
					String id = attributes.getValue(String.join(":", NS_XML, ATT_ID));
					String instValue = attributes.getValue(ATT_INST);
					instValue = instValue == null || instValue.isEmpty()? null : instValue.substring(1);
					String anaValue = attributes.getValue(ATT_ANA);
					anaValue = anaValue == null || anaValue.isEmpty()? null : anaValue.substring(1); 
					synNodeId2synTargetId.put(id, instValue);					
					synNodeId2AnaId.put(id, anaValue);
				}
				else if (READ_MODE.REFERENCES.equals(mode)) {
					String id = attributes.getValue(String.join(":", NS_XML, ATT_ID));					
					String[] instances = attributes.getValue(ATT_INST).split(" ");
					String anaValue = attributes.getValue(ATT_ANA).substring(1);
					List<String> instanceList = new ArrayList<>();
					for (String instance : instances) {
						refInstId2AnaId.put(instance.substring(1), anaValue);
						instanceList.add(instance.substring(1));
					}
					refInterpId2Inst.put(id, instanceList);
				}
			}
			else if (TAG_LINK.equals(localName)) {
				Map<String, List<Pair<String, String>>> linkMap = null;
				if (READ_MODE.SYNTAX.equals(mode)) {
					linkMap = syntaxLinks;				
				}
				else if (READ_MODE.REFERENCES.equals(mode)) {
					linkMap = referenceLinks;
				}
				if (linkMap != null) {
					String[] targetInfo = attributes.getValue(ATT_TARGET).split(SPACE);
					String sourceId = targetInfo[1].substring(1);
					String targetId = targetInfo[0].substring(1);
					String type = attributes.getValue(ATT_TYPE);
					if (!linkMap.containsKey(sourceId) ) {
						linkMap.put(sourceId, new ArrayList<Pair<String, String>>());
					}
					linkMap.get(sourceId).add(Pair.of(targetId, type));
					if (READ_MODE.SYNTAX.equals(mode)) {
						String sourceSynTargetId = synNodeId2synTargetId.get(sourceId);
						String targetSynTargetId = synNodeId2synTargetId.get(targetId);
						if (!targetId2governorIds.containsKey(targetSynTargetId)) {
							targetId2governorIds.put(targetSynTargetId, new HashSet<String>());
						}
						targetId2governorIds.get(targetSynTargetId).add(sourceSynTargetId);
					}
				} else {
					logger.warn(WARN_UNKNOWN_LINKS);
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
			textTracker.add(utils.createTextSegment(tokenId, "", "", null));
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
				if (TAG_F.equals(stack.peek()) && (READ_MODE.MORPHOSYNTAX.equals(mode) || READ_MODE.REFERENCES.equals(mode) || READ_MODE.SYNTAX.equals(mode))) { //TODO untested for syntax, no test case
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
		
		/* (BURN) AFTER READING */
		
		private static final String SPACE = " ";		
		private static final String NORM = "norm";		
		private static final String DIPL = "dipl";		
		private static final String PAUSE = "pause";
		private static final String SYN = "pnorm";
		
				
		/**
		 * Heuristic method to track down token indexes that demand the insertion of an empty node first
		 * @return
		 */		
		private HashMap<Integer, String> getInsertionIndexes() {
			HashMap<Integer, String> retVal = new HashMap<>();
			Set<String> seenIds = new HashSet<>();
			String synTargetId;
			for (int i = 0; i < textTracker.size() && !targetSequence.isEmpty(); i++) {
				String id = textTracker.get(i).getId();
				if (targetSequence.peek().getValue() != null && targetSequence.peek().getValue().equals(id)) {
					Pair<String, String> p = targetSequence.poll();
					seenIds.add( p.getKey() );
					while (!targetSequence.isEmpty() && p.getValue() != null && p.getValue().equals(targetSequence.peek().getValue())) {						
						p = targetSequence.poll();
					}
				}
				else if (id != null && targetSequence.peek().getValue() == null && targetSequence.peek().getKey() != null) { //TODO third might be irrelevant
					synTargetId = targetSequence.poll().getKey();
					retVal.put(i, synTargetId);
					seenIds.add( synTargetId );
				}
				else if (id == null && targetSequence.peek().getValue() == null && targetSequence.peek().getKey() != null) {					
					Set<String> governingNodes = targetId2governorIds.get(targetSequence.peek().getKey());
					Set<String> intersec = new HashSet<>(governingNodes);
					intersec.retainAll(seenIds);
					synTargetId = targetSequence.poll().getKey();
					if (intersec.isEmpty()) { //is following
						retVal.put(i + 1, synTargetId);
					} else { //is preceding
						retVal.put(i, synTargetId);
					}
					seenIds.add( synTargetId );					
				}
			}
			return retVal;
		}
		
		private void buildGraph() {		
			SDocumentGraph docGraph = getDocument().getDocumentGraph();
			STimeline timeline = docGraph.createTimeline();
			{
				timeline.increasePointOfTime(textTracker.size());		
			}
			String speaker = null;
			int firstTokenIx = 0;
			int lastTokenIx;
			TextSegment tokenObject = null;
			long startsAt = 0L;
			long endedAt = 0L;
			
			HashMap<Integer, Integer[]> tokenIndex2timelineSlots = new HashMap<>();
			HashMap<String, ArrayList<Integer>> speaker2TokenIndexes = new HashMap<>();
			HashMap<String, Triple<StringBuilder, StringBuilder, StringBuilder>> speaker2Text = new HashMap<>();
			List<Integer[][]> tokenLimits = new ArrayList<>(); //shape (n, 4, 2) where n = number of tokens
						
			boolean overlap = false;
			int lastEndTimeslot = 0;
			HashMap<String, Integer> speaker2synStart = new HashMap<>();
			HashMap<Integer, String> insertEmptyFirst = getInsertionIndexes();
			for (Triple<String, Integer, Pair<String, String>> utteranceObject : speakerTimeTracker) {
				speaker = utteranceObject.getLeft();							
				lastTokenIx = utteranceObject.getMiddle();				
				speaker2synStart.put(speaker, 0);
				
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
					lastEndTimeslot -= 1;
				}
				
				int nNodes = 0;
				Integer[] interval = null;
				int tOffset = 0;
				for (int i = firstTokenIx; i <= lastTokenIx; i++) {
					if (insertEmptyFirst.containsKey(i)) {
						tOffset++;
					}
					Integer [][] limitTuple = new Integer[4][2];					
					tokenLimits.add(limitTuple);
					tokenIndexes.add(i);					
					tokenObject = textTracker.get(i);
					List<?> l = tokenId2MoSynAnaIds.get(tokenObject.getId());
					nNodes = l == null || l.isEmpty()? 1 : l.size();
					interval = new Integer[]{lastEndTimeslot + tOffset, lastEndTimeslot + nNodes + tOffset};
					tokenIndex2timelineSlots.put(i, interval);										
					if (tokenObject.getPause() == null) {
						//is text
						String diplT = tokenObject.getDipl();
						String normT = tokenObject.getNorm();
						if (!diplT.isEmpty()) {
							limitTuple[0][0] = diplText.length();
							limitTuple[0][1] = diplText.length() + diplT.length();
							diplText.append(diplT).append(SPACE);	
						}
						if (!normT.isEmpty()) {
							limitTuple[1][0] = normText.length();
							limitTuple[1][1] = normText.length() + normT.length();
							normText.append(normT).append(SPACE);
						}
					} else {
						//is pause
						String pauseT = tokenObject.getPause();
						limitTuple[2][0] = pauseText.length();
						limitTuple[2][1] = pauseText.length() + pauseT.length();
						pauseText.append(pauseT).append(SPACE);
					}					
					lastEndTimeslot += nNodes;
				}				
				
				firstTokenIx = lastTokenIx + 1;
				endedAt = timeslotId2Time.get(utteranceObject.getRight().getRight());
			}			
			ArrayList<SToken> diplTokens;
			ArrayList<SToken> normTokens;
			ArrayList<SToken> pauseTokens;
			ArrayList<SToken> synNodeTokens;
			
			HashMap<String, String> sTokId2speaker = new HashMap<>();
			HashMap<String, SToken> anaId2SynToken = new HashMap<>(); 
			for (Entry<String, Triple<StringBuilder, StringBuilder, StringBuilder>> e : speaker2Text.entrySet()) {				
				speaker = e.getKey();	
				StringBuilder diplText = e.getValue().getLeft();
				StringBuilder normText = e.getValue().getMiddle();
				StringBuilder pauseText = e.getValue().getRight(); 
				
				STextualDS diplDS = docGraph.createTextualDS(diplText.toString());
				diplDS.createMetaAnnotation(null, "speaker", speaker);
				STextualDS normDS = docGraph.createTextualDS(normText.toString());
				normDS.createMetaAnnotation(null, "speaker", speaker);
				STextualDS pauseDS = docGraph.createTextualDS(pauseText.toString());
				pauseDS.createMetaAnnotation(null, "speaker", speaker);
				String synText = StringUtils.repeat('#', tokenLimits.size()*2); //FIXME determine correct text size
				STextualDS synDS = docGraph.createTextualDS(synText);
				synDS.createMetaAnnotation(null, "speaker", speaker);
				
				ArrayList<Integer> tokenIndexes = speaker2TokenIndexes.get(speaker);				
				
				diplTokens = new ArrayList<>();
				normTokens = new ArrayList<>();
				pauseTokens = new ArrayList<>();
				synNodeTokens = new ArrayList<>();				
				
				for (Iterator<Integer> itIx = tokenIndexes.iterator(); itIx.hasNext();) {					
					int tokenIndex = itIx.next();
					int synStart = speaker2synStart.get(speaker);
					Integer[][] limits = tokenLimits.get(tokenIndex);
					Integer[] timeslot = tokenIndex2timelineSlots.get(tokenIndex);
					if (insertEmptyFirst.containsKey(tokenIndex)) {
						String synTargetId = insertEmptyFirst.get(tokenIndex);
						SToken synNode = createSynNode(synDS, synStart, ++synStart, timeslot[0] - 1);									
						//TODO add potential annotations
						anaId2SynToken.put(synTargetId, synNode);									
						synNodeTokens.add(synNode);
						synTargetId2SToken.put(synTargetId, synNode); 
					}
					SToken sTok = null;
					String tokId = textTracker.get(tokenIndex).getId();
					if (limits[2][0] == null) {
						// is text
						if (limits[0][0] != null) {
							//create dipl token
							sTok = docGraph.createToken(diplDS, limits[0][0], limits[0][1]);
							diplTokens.add(sTok);
							addTimelineRelation(sTok, timeslot[0], timeslot[1]);							
						}					
						if (limits[1][0] != null) {
							//create and store norm token
							sTok = docGraph.createToken(normDS, limits[1][0], limits[1][1]);
							normTokens.add(sTok);
							addTimelineRelation(sTok, timeslot[0], timeslot[1]);
							if (tokId != null) {
								debugMessage("Building syn nodes as text");
								List<String> anaIds = tokenId2MoSynAnaIds.get(tokId);
								for (int t = timeslot[0]; t < timeslot[1]; t++) {									
									SToken synNode = createSynNode(synDS, synStart, ++synStart, t);									
									String anaId = anaIds.get(t - timeslot[0]);
									if (anaId != null) {
										addAnnotations(speaker, synNode, anaId);																		
									}
									anaId2SynToken.put(anaId, synNode);									
									synNodeTokens.add(synNode);
									synTargetId2SToken.put(moSynAna2SynTargetId.get(anaId), synNode);
								}
								speaker2synStart.put(speaker, synStart);
							}							
							sTokId2speaker.put(sTok.getId(), speaker);
						}
					} else {
						//create pause token
						sTok = docGraph.createToken(pauseDS, limits[2][0], limits[2][1]);
						pauseTokens.add(sTok);
						addTimelineRelation(sTok, timeslot[0], timeslot[1]);
					}
				}
				String diplName = String.join(DELIMITER, speaker, DIPL);
				String normName = String.join(DELIMITER, speaker, NORM);
				String pauseName = String.join(DELIMITER, speaker, PAUSE);
				String synName = String.join(DELIMITER, speaker, SYN);
				
				diplDS.setName(diplName);
				normDS.setName(normName);
				pauseDS.setName(pauseName);
				synDS.setName(synName);
				addOrderRelations(diplTokens, diplName);
				addOrderRelations(normTokens, normName);				
				if (docGraph.getText(pauseDS).trim().isEmpty()) {
					docGraph.removeNode(pauseDS);
				} else {
					addOrderRelations(pauseTokens, pauseName);	
				}
				addOrderRelations(synNodeTokens, synName);				
			}			
			
			buildSyntax();
		}

		private SToken createSynNode(STextualDS syntacticDS, int start, int end, int timeslot) {			
			SToken sTok = getDocument().getDocumentGraph().createToken(syntacticDS, start, end);			
			addTimelineRelation(sTok, timeslot, timeslot + 1);			
			return sTok;
		}
		
		private void addAnnotations(String speaker, SToken sTok, String anaId) {
			if (anaId != null && sTok != null && speaker != null) {				
				List<Pair<String, String>> annos = annotations.get(anaId);
				SSpan span = getDocument().getDocumentGraph().createSpan(sTok);
				for (Pair<String, String> anno : annos) {
					String annoName = String.join(DELIMITER, speaker, anno.getKey());
					span.createAnnotation(null, annoName, anno.getValue());						
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
			if (timeline.getEnd() < end) {
				timeline.increasePointOfTime(end - timeline.getEnd());
			}
			STimelineRelation rel = null;
			rel = SaltFactory.createSTimelineRelation();
			rel.setSource(tok);
			rel.setTarget(timeline);
			rel.setStart(start);
			rel.setEnd(end);
			getDocument().getDocumentGraph().addRelation(rel);
		}
	}
	
	
}
