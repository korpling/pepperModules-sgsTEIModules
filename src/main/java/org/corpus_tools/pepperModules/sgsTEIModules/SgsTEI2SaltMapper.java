package org.corpus_tools.pepperModules.sgsTEIModules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;

import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
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
		private String textBuffer;
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
			textBuffer = "";
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
					syntaxLayer.addNode(node);
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
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			textBuffer = new String(Arrays.copyOfRange(ch, start, start + length)).trim(); //TODO figure out if there are cases where trim should not be used			
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
			String text = textBuffer;
			if (dipl) {
				String newText = overwrite? text : lastTokenObject.getDipl().concat(text);
				lastTokenObject.setDipl(newText);
			}
			if (norm) {
				String newText = overwrite? text : lastTokenObject.getNorm().concat(text);
				lastTokenObject.setNorm(newText);
			}
			textBuffer = "";			
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			localName = qName.substring(qName.lastIndexOf(":") + 1);			
			String stackTop = stack.pop();
			if (TAG_W.equals(localName)) {
				// read mode TEXT assumed				
				/* also set the set dipl, because for add-cases dipl is also taken from w */
				boolean isCorrection = TAG_CORR.equals(stackTop);
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
			else if (TAG_DESC.equals(localName) && TAG_VOCAL.equals(stackTop)) {
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
					currentAnnotations.get(currentAnnotations.size() - 1).setValue(textBuffer);
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
			timeline.increasePointOfTime();
			// by iterating over speaker time tracker, we move through the text (ATTENTION: we are assuming, the text is given in chronological order)
			/*Textual Datasource, Text for DS, Index limits of tokens*/
			MutableTriple<STextualDS, StringBuilder, ArrayList<Pair<Integer, Integer>>> dipl = null;
			MutableTriple<STextualDS, StringBuilder, ArrayList<Pair<Integer, Integer>>> norm = null;
			MutableTriple<STextualDS, StringBuilder, ArrayList<Pair<Integer, Integer>>> pause = null;
			String speaker = null;
			int firstToken = 0;
			int lastToken;
			TextSegment tokenObject = null;
			Pair<Integer, Integer> limits = null;
			HashMap<String, SToken> tokenId2SToken = new HashMap<>();
			long startsAt = 0L;
			long endedAt = 0L;
			boolean overlap = false;
			for (MutableTriple<String, Integer, Pair<String, String>> utteranceObject : speakerTimeTracker) {
				speaker = utteranceObject.getLeft();				
				dipl = MutableTriple.of(null, new StringBuilder(), new ArrayList<Pair<Integer, Integer>>());
				norm = MutableTriple.of(null, new StringBuilder(), new ArrayList<Pair<Integer, Integer>>());
				pause = MutableTriple.of(null, new StringBuilder(), new ArrayList<Pair<Integer, Integer>>());				
				lastToken = utteranceObject.getMiddle();
				
				startsAt = timeslotId2Time.get(utteranceObject.getRight().getLeft());
				overlap = startsAt < endedAt;
				
				/*collect texts and limits*/				
				for (int i = firstToken; i <= lastToken; i++) {
					tokenObject = textTracker.get(i);
					if (tokenObject.getPause() == null) { // text or pause?
						if (!tokenObject.getDipl().isEmpty()) {
							// has dipl
							dipl.getRight().add(Pair.of(dipl.getMiddle().length(), dipl.getMiddle().length() + tokenObject.getDipl().length()));
							dipl.getMiddle().append(tokenObject.getDipl()).append(SPACE);
						}
						if (!tokenObject.getNorm().isEmpty()) {
							// has norm
							norm.getRight().add(Pair.of(norm.getMiddle().length(), norm.getMiddle().length() + tokenObject.getNorm().length()));
							norm.getMiddle().append(tokenObject.getNorm()).append(SPACE);
						}
					} else {
						pause.getRight().add(Pair.of(pause.getMiddle().length(), pause.getMiddle().length() + tokenObject.getPause().length()));
						pause.getMiddle().append(tokenObject.getPause()).append(SPACE);
					}
				}
				//get DSs
				Triple<STextualDS, STextualDS, STextualDS> dsTriple = speaker2DSMap.get(speaker);
				if (dsTriple == null) {
					dsTriple = Triple.of(docGraph.createTextualDS(""), 
						 	  docGraph.createTextualDS(norm.getMiddle().toString().trim()), 
							  docGraph.createTextualDS(pause.getMiddle().toString().trim())
					);
					speaker2DSMap.put(speaker, dsTriple);					
				}				
				dipl.setLeft(dsTriple.getLeft());
				norm.setLeft(dsTriple.getMiddle());
				pause.setLeft(dsTriple.getRight());
				
				//token counters 
				int d = 0;
				int n = 0;
				int p = 0;
				
				//obtain token length offsets
				int offset_d = dipl.getLeft().getText().length() + 1;
				int offset_n = norm.getLeft().getText().length()+ 1;
				int offset_p = pause.getLeft().getText().length() + 1;
				
				//enlarge text				
				dipl.getLeft().setText(dipl.getLeft().getText().concat(dipl.getMiddle().toString()));			
				norm.getLeft().setText(norm.getLeft().getText().concat(norm.getMiddle().toString()));
				pause.getLeft().setText(pause.getLeft().getText().concat(pause.getMiddle().toString()));								
				
				/*token lists : (dipl, norm, pause)*/
				Triple<ArrayList<SToken>, ArrayList<SToken>, ArrayList<SToken>> tokenLists = 
						Triple.of(new ArrayList<SToken>(), 
								new ArrayList<SToken>(), 
								new ArrayList<SToken>());
				
				/*create token objects and add annotations*/				
				for (int i = firstToken; i <= lastToken; i++) {
					tokenObject = textTracker.get(i);
					
					if (tokenObject.getPause() == null) {
						ArrayList<SToken> newTokens = new ArrayList<SToken>();
						SToken tok = null;
						// is textual
						if (!tokenObject.getDipl().isEmpty()) {
							// has dipl
							limits = dipl.getRight().get(d++);
							tok = docGraph.createToken(dipl.getLeft(), limits.getLeft() + offset_d, limits.getRight() + offset_d);
							newTokens.add(tok);
							tokenLists.getLeft().add(tok);
						}
						if (!tokenObject.getNorm().isEmpty()) {
							// has norm
							limits = norm.getRight().get(n++);
							tok = docGraph.createToken(norm.getLeft(), limits.getLeft() + offset_n, limits.getRight() + offset_n);
							newTokens.add(tok);
							{ //add annotations
								String markableId = tokenId2markableAnaId.get(tokenObject.getId());
								if (morphosyntax.containsKey(markableId)) {								
									for (SAnnotation anno : morphosyntax.get(markableId)) {
										anno.setName(String.join("_", speaker, anno.getName()));
										tok.addAnnotation(anno);
									}
								}
							}
							tokenLists.getMiddle().add(tok);
							tokenId2SToken.put(tokenObject.getId(), tok); //only norm tokens need ids
						}
						addTimelineRelations(newTokens, docGraph.getTimeline(), !overlap);
						overlap = false;						
					} else {
						// is pause (which never overlaps)
						limits = pause.getRight().get(p++);
						SToken tok = docGraph.createToken(pause.getLeft(), limits.getLeft() + offset_p, limits.getRight() + offset_p);
						int start = timeline.getEnd();
						timeline.increasePointOfTime();
						addTimelineRelation(tok, timeline, start, timeline.getEnd());
						tokenLists.getRight().add(tok);
					}
				}				
				addOrderRelations(tokenLists.getLeft(), String.join(DELIMITER, speaker, DIPL));
				addOrderRelations(tokenLists.getMiddle(), String.join(DELIMITER, speaker, NORM));
				addOrderRelations(tokenLists.getRight(), String.join(DELIMITER, speaker, PAUSE));
								
				firstToken = lastToken + 1;
				endedAt = timeslotId2Time.get(utteranceObject.getRight().getRight());
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
					domRel = SaltFactory.createSDominanceRelation();
					domRel.setSource(node);
					domRel.setTarget(target);
					SAnnotation anno = SaltFactory.createSAnnotation();
					anno.setName(ATT_TYPE);
					anno.setValue(type);
					domRel.addAnnotation(anno);
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
				}
			}
			docGraph.addLayer(syntaxLayer);
		}		
		
		private static final String DELIMITER = "_";
				
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
		
		private void addTimelineRelations(ArrayList<SToken> tokens, STimeline timeline, boolean increase) {
			int end = timeline.getEnd();
			int start = end;
			if (increase) {
				timeline.increasePointOfTime();
				end += 1;
			} else {
				start -= 1;
			}
			for (SToken tok : tokens) {
				addTimelineRelation(tok, timeline, start, end);
			}
		}
		
		private void addTimelineRelation(SToken tok, STimeline timeline, int start, int end) {
			STimelineRelation rel = null;
			rel = SaltFactory.createSTimelineRelation();
			rel.setSource(tok);
			rel.setTarget(timeline);
			rel.setStart(start);
			rel.setEnd(end);
			getDocument().getDocumentGraph().addRelation(rel);
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
