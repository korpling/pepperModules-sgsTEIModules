package org.corpus_tools.pepperModules.sgsTEIModules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SOrderRelation;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.util.DataSourceSequence;
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
		private ArrayList<MutableTriple<String, String, String>> textTracker;
		/** speaker and time tracker, each entry is (SPEAKER_NAME, index of last token in utterance, (START-TIME, END-TIME)) */
		private ArrayList<MutableTriple<String, Integer, Pair<String, String>>> speakerTimeTracker;
		/**mapping from markable-id to List of annotations*/
		private HashMap<String, List<SAnnotation>> morphosyntax;
		/**mapping from tokens to markables*/
		private HashMap<String, String> tokenId2markableId;
		/**variable keeping track of last markables annotation list, last annotation can simple be taken from end of the list*/
		private List<SAnnotation> currentAnnotations;
		
		
		
		public SgsTEIReader() {
			stack = new Stack<String>();
			textBuffer = "";
			mode = READ_MODE.BLIND;
			
			textTracker = new ArrayList<>();
			morphosyntax = new HashMap<>();
			tokenId2markableId = new HashMap<>();
			id2TimeMap = new HashMap<>();
		}
		
		private void debugMessage(String... elements) {
			System.out.println(String.join(" ", elements));
		}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			localName = qName.substring(qName.lastIndexOf(":") + 1);
			if (TAG_W.equals(localName)) {
				if (READ_MODE.TEXT.equals(mode)) {
					String tokenId = attributes.getValue(NS_XML, ATT_ID);
					newToken(tokenId);
				}
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
					tokenId2markableId.put(attributes.getValue(ATT_TARGET).substring(1), attributes.getValue(ATT_ANA).substring(1));
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
			}
			else if (TAG_F.equals(localName)) {
				if (READ_MODE.MORPHOSYNTAX.equals(mode)) {
					SAnnotation anno = SaltFactory.createSAnnotation();
					anno.setName(attributes.getValue(ATT_NAME));					
					currentAnnotations.add(anno);					
				}
			}
			else if (TAG_FS.equals(localName)) {
				if (READ_MODE.MORPHOSYNTAX.equals(mode)) {
					currentAnnotations = new ArrayList<SAnnotation>();
					String markableId = attributes.getValue(NS_XML, ATT_ID);
					morphosyntax.put(markableId, currentAnnotations);
				}
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
		
		private void newToken(String tokenId) {
			textTracker.add(MutableTriple.of(tokenId, "", ""));
		}
		
		private void readToken(String text, boolean dipl, boolean norm, boolean overwrite) {
			MutableTriple<String, String, String> lastTokenObject = textTracker.get(textTracker.size() - 1);
			if (dipl) {
				String newText = overwrite? text : lastTokenObject.getMiddle().concat(text);
				lastTokenObject.setMiddle(newText);
			}
			if (norm) {
				String newText = overwrite? text : lastTokenObject.getRight().concat(text);
				lastTokenObject.setRight(newText);
			}
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			localName = qName.substring(qName.lastIndexOf(":") + 1);			
			String stackTop = stack.pop();
			if (TAG_W.equals(localName)) {
				// read mode TEXT assumed				
				/* also set the set dipl, because for add-cases dipl is also taken from w */
				readToken(textBuffer, true, true, false);
			}
			else if (TAG_PC.equals(localName)) {
				// read mode TEXT assumed
				readToken(textBuffer, true, true, false);
			}
			else if (TAG_ADD.equals(localName)) {
				// read mode TEXT assumed				
				// here we DON'T add the add-text to middle, because it does not belong to dipl
				readToken(textBuffer, false, true, false);
			}
			else if (TAG_F.equals(localName)) {
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
		
		private static final String SPACE = " ";
		
		private static final String NORM = "norm";
		
		private static final String DIPL = "dipl";
		
		private static final String PAUSE = "pause";
		
		private void buildGraph() {
			SDocumentGraph docGraph = getDocument().getDocumentGraph();
			STimeline timeline = docGraph.createTimeline();
			// by iterating over speaker time tracker, we move through the text (ATTENTION: we are assuming, the text is given in chronological order)
			/*Textual Datasource, Text for DS, Index limits of tokens*/
			MutableTriple<STextualDS, StringBuilder, ArrayList<Pair<Integer, Integer>>> dipl = null;
			MutableTriple<STextualDS, StringBuilder, ArrayList<Pair<Integer, Integer>>> norm = null;
			MutableTriple<STextualDS, StringBuilder, ArrayList<Pair<Integer, Integer>>> pause = null;
			String speaker = null;
			int firstToken = 0;
			int lastToken;
			MutableTriple<String, String, String> tokenObject = null;
			Pair<Integer, Integer> limits = null;
			for (MutableTriple<String, Integer, Pair<String, String>> utteranceObject : speakerTimeTracker) {
				dipl = MutableTriple.of(null, new StringBuilder(), new ArrayList<Pair<Integer, Integer>>());
				norm = MutableTriple.of(null, new StringBuilder(), new ArrayList<Pair<Integer, Integer>>());
				pause = MutableTriple.of(null, new StringBuilder(), new ArrayList<Pair<Integer, Integer>>());
				speaker = utteranceObject.getLeft();
				lastToken = utteranceObject.getMiddle();
				
				/*collect texts and limits*/
				for (int i = firstToken; i <= lastToken; i++) {
					tokenObject = textTracker.get(i);
					if (!tokenObject.getLeft().isEmpty()) {
						if (!tokenObject.getMiddle().isEmpty()) {
							// has dipl
							dipl.getRight().add(Pair.of(dipl.getMiddle().length(), dipl.getMiddle().length() + tokenObject.getMiddle().length()));
							dipl.getMiddle().append(tokenObject.getMiddle()).append(SPACE);
						}
						if (!tokenObject.getRight().isEmpty()) {
							// has norm
							norm.getRight().add(Pair.of(norm.getMiddle().length(), norm.getMiddle().length() + tokenObject.getRight().length()));
							norm.getMiddle().append(tokenObject.getRight()).append(SPACE);
						}
					} else {
						pause.getRight().add(Pair.of(pause.getMiddle().length(), pause.getMiddle().length() + tokenObject.getMiddle().length()));
						pause.getMiddle().append(tokenObject.getMiddle()).append(SPACE);
					}
				}
				dipl.setLeft(docGraph.createTextualDS(dipl.getMiddle().toString().trim()));
				norm.setLeft(docGraph.createTextualDS(norm.getMiddle().toString().trim()));
				pause.setLeft(docGraph.createTextualDS(pause.getMiddle().toString().trim()));
				
				int d = 0, n = 0, p = 0;
				/*create token objects and add annotations*/
				for (int i = firstToken; i <= lastToken; i++) {
					tokenObject = textTracker.get(i);
					if (!tokenObject.getLeft().isEmpty()) {
						ArrayList<SToken> newTokens = new ArrayList<SToken>();
						SToken tok = null;
						// is textual
						if (!tokenObject.getMiddle().isEmpty()) {
							// has dipl
							limits = dipl.getRight().get(d++);
							tok = docGraph.createToken(dipl.getLeft(), limits.getLeft(), limits.getRight());
							newTokens.add(tok);
						}
						if (!tokenObject.getRight().isEmpty()) {
							// has norm
							limits = norm.getRight().get(n++);
							tok = docGraph.createToken(norm.getLeft(), limits.getLeft(), limits.getRight());
							newTokens.add(tok);
							{ //add annotations
								for (SAnnotation anno : morphosyntax.get(tokenId2markableId.get(tokenObject.getLeft()))) {
									anno.setName(String.join("_", speaker, anno.getName()));
									tok.addAnnotation(anno);
								}
							}
						}
						addTimelineRelations(newTokens);
						
					} else {
						// is pause
						limits = pause.getRight().get(p++);
						SToken tok = docGraph.createToken(pause.getLeft(), limits.getLeft(), limits.getRight());
						int start = timeline.getEnd();
						timeline.increasePointOfTime();
						addTimelineRelation(tok, timeline, start, timeline.getEnd());
					}
				}
				addOrderRelations(docGraph.getTokensBySequence((DataSourceSequence) dipl.getLeft()), String.join(DELIMITER, speaker, DIPL));
				addOrderRelations(docGraph.getTokensBySequence((DataSourceSequence) norm.getLeft()), String.join(DELIMITER, speaker, NORM));
				addOrderRelations(docGraph.getTokensBySequence((DataSourceSequence) pause.getLeft()), String.join(DELIMITER, speaker, PAUSE));
				
				
				firstToken = lastToken + 1;
			}
		}
		
		private static final String DELIMITER = "_";
		
		private void addOrderRelations(List<SToken> tokens, String name) {
			SDocumentGraph docGraph = getDocument().getDocumentGraph();
			SOrderRelation rel = null;
			for (int i = 0; i < tokens.size() - 1; i++) {
				rel = SaltFactory.createSOrderRelation();
				rel.setSource(tokens.get(i));
				rel.setTarget(tokens.get(i + 1));
				rel.setType(name); //TODO right method?
			}
		}
		
		private void addTimelineRelations(ArrayList<SToken> tokens) {
			STimeline timeline = getDocument().getDocumentGraph().getTimeline();
			int start = timeline.getEnd();
			timeline.increasePointOfTime();
			int end = timeline.getEnd();
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
