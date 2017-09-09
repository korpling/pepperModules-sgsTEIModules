package org.corpus_tools.pepperModules.sgsTEIModules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

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
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STimeline;
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
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			localName = localName.substring(localName.lastIndexOf(":") + 1);
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
			else if (TAG_SEG.equals(localName)) {
				LinguisticParent parent = parentStack.peek();
				LinguisticParent seg = new LinguisticParent(parent.getSpeaker(), -1, -1);
				parent.addElement(seg);
				parentStack.push(seg);
				id2Object.put(attributes.getValue(NS_XML, ATT_ID), seg);
			}
			else if (TAG_U.equals(localName)) {
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
			localName = localName.substring(localName.lastIndexOf(":") + 1);
			String stackTop = stack.pop();
			if (!localName.equals(stackTop)) {
				throw new PepperModuleDataException(SgsTEI2SaltMapper.this, ERR_MSG_STACK_INCONSISTENCY_EXCEPTION);
			}
			if (TAG_W.equals(localName) || TAG_PC.equals(localName)) {
				if (TAG_SEG.equals(stack.peek())) {
					currentT.setValue(textBuffer);
				}
				else if (TAG_CORR.equals(stack.peek())) {
					overlap.getRight().setValue(textBuffer);
				}
			}
			else if (TAG_SEG.equals(localName)) {
				parentStack.pop();
			}
			else if (TAG_SIC.equals(localName)) {
				overlap.getLeft().setValue(textBuffer);
			}
			else if (TAG_DESC.equals(localName) && TAG_VOCAL.equals(stack.peek())) {
				LinguisticParent parent = parentStack.peek();
				TokenLike t = new TokenLike(ElementType.DIPL, textBuffer);
				parent.addElement(t);				
			}
			else if (TAG_TEI.equals(localName)) {
				buildGraph();
			}
		}
		
		/**
		 * This method is called after the whole document has been read and the graph can be built 
		 * from the collected information.
		 */
		private void buildGraph() {
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
				while (!iteratorStack.isEmpty()) {
					e = iteratorStack.peek().next();
					ElementType etype = e.getElementType();
					if (ElementType.PARENT.equals(etype)) {
						iteratorStack.push(e.getElements().iterator());
					}
					else {
						if (ElementType.ALL.equals(etype) || ElementType.DIPL.equals(etype)) {
							dipl.append(e.getValue()).append(space);
						}
						if (ElementType.ALL.equals(etype) || ElementType.NORM.equals(etype)) {
							norm.append(e.getValue()).append(space);
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
			for (int i = 0; i < orderedTimes.length; i++) {
				SequenceElement e = start2utterance.get(orderedTimes[i]);
				Stack<Iterator<SequenceElement>> iteratorStack = new Stack<>();				
				iteratorStack.push(e.getElements().iterator());
				while (!iteratorStack.isEmpty()) {
					e = iteratorStack.peek().next();
					ElementType etype = e.getElementType();
					if (ElementType.PARENT.equals(etype)) {
						iteratorStack.push(e.getElements().iterator());
					}
					else {
						if (ElementType.ALL.equals(etype) || ElementType.DIPL.equals(etype)) {
							nv = d + e.getValue().length();
							docGraph.createToken(diplDS, d, nv);
							d = nv + 1;
						}
						if (ElementType.ALL.equals(etype) || ElementType.NORM.equals(etype)) {
							nv = n + e.getValue().length();
							docGraph.createToken(normDS, n, nv);
							n = nv + 1;
						}
						if (!iteratorStack.peek().hasNext()) {
							iteratorStack.pop();
						}
					}
				}
			}			
		}
	}
}
