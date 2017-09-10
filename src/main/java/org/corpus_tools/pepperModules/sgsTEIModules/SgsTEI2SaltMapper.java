package org.corpus_tools.pepperModules.sgsTEIModules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleDataException;
import org.corpus_tools.pepperModules.sgsTEIModules.lib.ElementType;
import org.corpus_tools.pepperModules.sgsTEIModules.lib.LinguisticParent;
import org.corpus_tools.pepperModules.sgsTEIModules.lib.SequenceElement;
import org.corpus_tools.pepperModules.sgsTEIModules.lib.TokenLike;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SOrderRelation;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

public class SgsTEI2SaltMapper extends PepperMapperImpl{
	
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
	
	/**
	 * This sub class is the mapper's callback handler processing the input xml.
	 * @author klotzmaz
	 *
	 */
	private class SgsTEIReader extends DefaultHandler2 implements SgsTEIDictionary {
		
		private final Logger logger = LoggerFactory.getLogger(SgsTEIReader.class);
		
		private static final String ERR_MSG_STACK_INCONSISTENCY_EXCEPTION = "Opening and closing element do not match!";
		/** This is the element stack representing the hierarchy of elements to be closed. */
		private Stack<String> stack;
		/** This variable is keeping track of read characters */
		private String textBuffer;
		/** This mapping represents the timeline provided with the TEI file to preserve order as given. */
		private HashMap<String, Long> internalOrder;
		/** This represents the current time frame the utterance takes place in. */
		private long[] currentFrame;
		
		/** This collects the utterances ordered by appearance, not by temporal annotation */
		private List<LinguisticParent> corpusData;
		/** This keeps track of the currently edited linguistic parent */
		private Stack<LinguisticParent> parentStack;
		/** This is a global mapping of Ids to internal objects */
		private HashMap<String, SequenceElement> id2Object;
		/** Since the current token value will be known after the closing tag, we need to keep track of the object */
		private TokenLike currentT;
		/** This is used to record overlapping elements as in choice elements */
		private Pair<TokenLike, TokenLike> overlap;
		
		public SgsTEIReader() {
			stack = new Stack<String>();
			textBuffer = "";
			internalOrder = new HashMap<String, Long>();
			currentFrame = new long[2];
			corpusData = new ArrayList<LinguisticParent>();
			id2Object = new HashMap<String, SequenceElement>();
		}
		
		private void debugMessage(String... elements) {
			System.out.println(String.join(" ", elements));
		}
		
		private boolean debugEnabled = false;
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			localName = qName.substring(qName.lastIndexOf(":") + 1);
			if (debugEnabled && !stack.isEmpty()) {
				debugMessage("OPENING", localName, stack.peek(), parentStack != null && !parentStack.isEmpty()? parentStack.peek().toString() : "");
			}
			if (TAG_W.equals(localName) || TAG_PC.equals(localName)) {
				if (TAG_SEG.equals(stack.peek())) {
					currentT = new TokenLike(ElementType.ALL, null);					
					id2Object.put(attributes.getValue(NS_XML, ATT_ID), currentT);
				}
				else if (TAG_CORR.equals(stack.peek())) {
					id2Object.put(attributes.getValue(NS_XML, ATT_ID), overlap.getRight());
				}
			}
			else if (TAG_PAUSE.equals(localName)) {
				LinguisticParent parent = parentStack.peek();
				TokenLike pause = new TokenLike(ElementType.DIPL, null);
				parent.addElement(pause);
				pause.addAnnotation(ATT_TYPE, attributes.getValue(ATT_TYPE));
			}
			else if (TAG_CHOICE.equals(localName)) {
				overlap = Pair.of(new TokenLike(ElementType.DIPL, null), new TokenLike(ElementType.NORM, null));
				overlap.getLeft().setOverlap(overlap.getRight());
				overlap.getRight().setOverlap(overlap.getLeft());
			}
			else if (TAG_ADD.equals(localName)) {
				overlap = Pair.of(new TokenLike(ElementType.DIPL, textBuffer), new TokenLike(ElementType.NORM, textBuffer));
				overlap.getLeft().setOverlap(overlap.getRight());
				overlap.getRight().setOverlap(overlap.getLeft());
				currentT = null;
			}
			else if (TAG_SEG.equals(localName)) {
				LinguisticParent parent = parentStack.peek();
				LinguisticParent seg = new LinguisticParent(parent.getSpeaker(), -1, -1);
				parent.addElement(seg);
				parentStack.push(seg);
				id2Object.put(attributes.getValue(NS_XML, ATT_ID), seg);
			}
			else if (TAG_U.equals(localName)) {
				debugEnabled = true;
				String speaker = attributes.getValue(ATT_WHO);
				long start = internalOrder.get(attributes.getValue(ATT_START).substring(1));
				long end = internalOrder.get(attributes.getValue(ATT_END).substring(1));	
				LinguisticParent currentUtterance = new LinguisticParent(speaker, start, end);
				corpusData.add(currentUtterance);
				parentStack = new Stack<LinguisticParent>();
				parentStack.push(currentUtterance);
				id2Object.put(attributes.getValue(NS_XML, ATT_ID), currentUtterance);
			}
			else if (TAG_WHEN.equals(localName) && TAG_TIMELINE.equals(stack.peek())) {
				String timeValue = attributes.getValue(ATT_ABSOLUTE).replaceAll("\\.|:", "");
				internalOrder.put(attributes.getValue(String.join(":", NS_XML, ATT_ID)), Long.parseLong(timeValue));
			}
			stack.push(localName);
		}
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			textBuffer = new String(Arrays.copyOfRange(ch, start, start + length)).trim(); //TODO figure out if there are cases where trim should not be used			
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			localName = qName.substring(qName.lastIndexOf(":") + 1);			
			String stackTop = stack.pop();
			if (debugEnabled && !stack.isEmpty()) {
				debugMessage("CLOSURE", localName, stack.peek(), parentStack != null && !parentStack.isEmpty()? parentStack.peek().toString() : "");				
			}
			if (!localName.equals(stackTop)) {
				throw new PepperModuleDataException(SgsTEI2SaltMapper.this, ERR_MSG_STACK_INCONSISTENCY_EXCEPTION);
			}
			if (TAG_W.equals(localName) || TAG_PC.equals(localName)) {
				if (TAG_SEG.equals(stack.peek())) {
					if (overlap != null) {
						TokenLike dipl = overlap.getLeft();
						TokenLike norm = overlap.getRight();
						postassignTextValue(dipl, dipl.getValue().concat(textBuffer));
						postassignTextValue(norm, norm.getValue().concat(textBuffer));
						parentStack.peek().addElement(dipl);
						overlap = null;
					} else {
						postassignTextValue(currentT, textBuffer);
						parentStack.peek().addElement(currentT);
					}					
				}
				else if (TAG_CORR.equals(stack.peek())) {
					postassignTextValue(overlap.getRight(), textBuffer);
				}
			}
			else if (TAG_SEG.equals(localName)) {
				parentStack.pop();
			}
			else if (TAG_SIC.equals(localName)) {
				postassignTextValue(overlap.getLeft(), textBuffer);
			}
			else if (TAG_ADD.equals(localName)) {
				TokenLike norm = overlap.getRight();
				postassignTextValue(norm, norm.getValue().concat(textBuffer));
				textBuffer = "";
			}
			else if (TAG_CHOICE.equals(localName)) {
				parentStack.peek().addElement(overlap.getLeft());
				overlap = null;
			}
			else if (TAG_DESC.equals(localName) && TAG_VOCAL.equals(stack.peek())) {
				LinguisticParent parent = parentStack.peek();
				TokenLike t = new TokenLike(ElementType.DIPL, textBuffer);
				parent.addElement(t);				
			}
			else if (TAG_TEI.equals(localName)) {
				debugPrintCollectedData();
				buildGraph();
			}
		}
		
		private void postassignTextValue(TokenLike tokenLike, String value) {
			tokenLike.setValue(value);
		}
		
		private void debugPrintCollectedData() {
			for (LinguisticParent u : corpusData) {
				debugMessage("utterance", u.toString());
				debugMessage(debugRecPrint(u, 0));
			}
		}
		
		private String debugRecPrint(SequenceElement elem, int indent) {
			ElementType etype = elem.getElementType();
			if (ElementType.PARENT.equals(etype)) {
				String[] representations = new String[elem.getElements().size()];
				for (int i = 0; i < representations.length; i++) {
					representations[i] = debugRecPrint(elem.getElements().get(i), indent + 1);
				}
				return String.join(System.lineSeparator(), representations);
			} else {
				return String.join(" ", StringUtils.repeat(' ', indent), elem.toString());
			}
		}
		
		private void addTimelineRelation(STimeline timeline, SToken source, boolean increase) {
			if (increase) {
				timeline.increasePointOfTime();
			}
			int pointInTime = timeline.getEnd();
			STimelineRelation timeRel = SaltFactory.createSTimelineRelation();
			timeRel.setSource(source);
			timeRel.setTarget(timeline);
			timeRel.setStart(pointInTime - 1);
			timeRel.setEnd(pointInTime);
			getDocument().getDocumentGraph().addRelation(timeRel);
		}
		
		/**
		 * This method is called after the whole document has been read and the graph can be built 
		 * from the collected information.
		 */
		private void buildGraph() {
			debugMessage("Building graph ...");
			/* first obtain order */
			HashMap<Long, LinguisticParent> start2utterance = new HashMap<Long, LinguisticParent>();
			long[] orderedTimes = new long[corpusData.size()];
			for (int i = 0; i < corpusData.size(); i++) { //FIXME Dangerous, when people start exactly the same time
				LinguisticParent lp = corpusData.get(i);
				start2utterance.put(lp.getStart(), lp);
				orderedTimes[i] = lp.getStart();
			}
			Arrays.sort(orderedTimes);
			// build document utterance by utterance
			/* collect and build base texts */
			StringBuilder dipl = new StringBuilder();
			StringBuilder norm = new StringBuilder();
			String space = " ";
			for (int i = 0; i < orderedTimes.length; i++) {
				SequenceElement e = start2utterance.get(orderedTimes[i]);
				Stack<Iterator<SequenceElement>> iteratorStack = new Stack<>();				
				iteratorStack.push(e.getElements().iterator());
				while (!iteratorStack.isEmpty() && iteratorStack.peek().hasNext()) { // FIXME the second condition should not be necessary
					e = iteratorStack.peek().next();
					ElementType etype = e.getElementType();
					if (ElementType.PARENT.equals(etype)) {
						iteratorStack.push(e.getElements().iterator());
					}
					else {
						if (ElementType.ALL.equals(etype) || ElementType.DIPL.equals(etype)) {
							if (e.getValue() != null) {
								dipl.append(e.getValue()).append(space);
								SequenceElement ov = ElementType.ALL.equals(etype)? e : e.getOverlap();
								if (ov != null && ov.getValue() != null) {
									norm.append(ov.getValue()).append(space);
								}
							}
						}
						if (!iteratorStack.peek().hasNext()) {
							iteratorStack.pop();
						}
					}
				}
			}
			SDocumentGraph docGraph = getDocument().getDocumentGraph();
			STimeline timeline = docGraph.createTimeline();
			STextualDS diplDS = docGraph.createTextualDS(dipl.toString().trim());
			STextualDS normDS = docGraph.createTextualDS(norm.toString().trim());			
			int d = 0;
			int n = 0;
			int nv = 0;
			List<SToken> diplTokens = new ArrayList<SToken>();
			List<SToken> normTokens = new ArrayList<SToken>();
			for (int i = 0; i < orderedTimes.length; i++) {
				SequenceElement e = start2utterance.get(orderedTimes[i]);
				Stack<Iterator<SequenceElement>> iteratorStack = new Stack<>();				
				iteratorStack.push(e.getElements().iterator());
				while (!iteratorStack.isEmpty() && iteratorStack.peek().hasNext()) { //FIXME get rid of second condition
					e = iteratorStack.peek().next();
					ElementType etype = e.getElementType();
					if (ElementType.PARENT.equals(etype)) {
						iteratorStack.push(e.getElements().iterator());
					}
					else {
						if (ElementType.ALL.equals(etype) || ElementType.DIPL.equals(etype)) {
							if (e.getValue() != null) {
								nv = d + e.getValue().length();			
								SToken tok = docGraph.createToken(diplDS, d, nv);
								diplTokens.add(tok);
								d = nv + 1;
								addTimelineRelation(timeline, tok, true);
								SequenceElement ov = ElementType.ALL.equals(etype)? e : e.getOverlap();
								if (ov != null && ov.getValue() != null) {
									nv = n + ov.getValue().length();
									tok = docGraph.createToken(normDS, n, nv);
									normTokens.add(tok);
									addTimelineRelation(timeline, tok, false);
									n = nv + 1;										
								}
							}
						}
						if (!iteratorStack.peek().hasNext()) {
							iteratorStack.pop();
						}
					}
				}
			}			
			SOrderRelation orderRel = null;
			List<List<SToken>> tokenSources = new ArrayList<>();
			tokenSources.add(diplTokens);
			tokenSources.add(normTokens);
			String[] names = {((SgsTEIImporterProperties) getProperties()).getDiplName(), ((SgsTEIImporterProperties) getProperties()).getNormName()};
			for (int j = 0; j < tokenSources.size(); j++) {
				List<SToken> tokenSource = tokenSources.get(j);
				String name = names[j];
				for (int i = 1; i < tokenSource.size(); i++) {
					orderRel = SaltFactory.createSOrderRelation();
					orderRel.setSource(tokenSource.get(i-1));
					orderRel.setTarget(tokenSource.get(i));
					orderRel.setType(name);
					docGraph.addRelation(orderRel);
				}
			}		
		}
	}
}
