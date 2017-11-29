package org.corpus_tools.pepperModules.sgsTEIModules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleDataException;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SDominanceRelation;
import org.corpus_tools.salt.common.SOrderRelation;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SStructure;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.exceptions.SaltException;
import org.corpus_tools.salt.semantics.SCatAnnotation;
import org.corpus_tools.salt.util.ExportFilter;
import org.corpus_tools.salt.util.VisJsVisualizer;
import org.eclipse.emf.common.util.URI;
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
	
	private SgsTEIImporterProperties getModuleProperties() {
		return (SgsTEIImporterProperties) getProperties();
	}
	
	/**
	 * This sub class is the mapper's callback handler processing the input xml.
	 * @author klotzmaz
	 *
	 */
	private class SgsTEIReader extends DefaultHandler2 implements SgsTEIDictionary{
		
		private final Logger logger = LoggerFactory.getLogger(SgsTEIReader.class);
		
		private final String PAUSE_TEXT_VALUE = null;
		
		private static final String PAUSE_LAYER_NAME = "pause";
		
		private static final String ERR_MSG_STACK_INCONSISTENCY_EXCEPTION = "Opening and closing element do not match!";

		private static final String CAT_SEG = "clause";

		private static final String CAT_U = "u";
		/** This is the element stack representing the hierarchy of elements to be closed. */
		private Stack<String> stack;
		/** This variable is keeping track of read characters */
		private String textBuffer;
		/** This mapping represents the timeline provided with the TEI file to preserve order as given. */
		private HashMap<String, Long> internalOrder;
		
		/** This collects the utterances ordered by appearance, not by temporal annotation */
		private List<LinguisticParent> corpusData;
		/** This keeps track of the currently edited linguistic parent */
		private Stack<LinguisticParent> parentStack;
		/** Since the current token value will be known after the closing tag, we need to keep track of the object */
		private TokenLike currentT;
		/** This is used to record overlapping elements as in choice elements */
		private Pair<TokenLike, TokenLike> overlap;
		/** This variable keeps track of the currently read spanGrp */
		private String currentSpanGroupType;
		/** This variable stores the mapping from tokens to list of annotations given in spans for each spangroup type FIXME maybe there is only on type*/
		private HashMap<String, HashMap<String, List<String>>> group2AnnotationMapping;
		/** This variable collects the stand-off annotations */
		private HashMap<String, HashMap<String, String>> annoId2FreeAnnotations; 
		/** This variable stores the id of currently read annotation */
		private String currentAnnoId;
		/** This variable keeps track of the currently active annotation name in the f-tag's environment */
		private String currentAnnoLayer;
		/** This variable keeps track of the current standoff type (morphosyntax, syntax, references, ...)*/
		private String currentStandoffType;
		/** This collects pointers for dependency relations, gov:dep;label */
		private HashMap<String, Pair<String, String>> gov2depPointers;
		
		public SgsTEIReader() {
			stack = new Stack<String>();
			textBuffer = "";
			internalOrder = new HashMap<String, Long>();
			corpusData = new ArrayList<LinguisticParent>();
			group2AnnotationMapping = new HashMap<String, HashMap<String, List<String>>>();
			annoId2FreeAnnotations = new HashMap<String, HashMap<String, String>>();
			gov2depPointers = new HashMap<>();
		}
		
		private void debugMessage(String... elements) {
			System.out.println(String.join(" ", elements));
		}
		
		private boolean debugEnabled = false;
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			localName = qName.substring(qName.lastIndexOf(":") + 1);
			if (TAG_W.equals(localName) || TAG_PC.equals(localName)) {
				if (TAG_SEG.equals(stack.peek())) {
					currentT = new TokenLike(ElementType.ALL, null, attributes.getValue(String.join(":", NS_XML, ATT_ID)));
				}
				else if (TAG_CORR.equals(stack.peek())) {
					overlap.getRight().setId(attributes.getValue(String.join(":", NS_XML, ATT_ID)));
				}
			}
			else if (/*TYPE_MORPHOSYNTAX.equals(currentStandoffType) && */TAG_FS.equals(localName)) {
				currentAnnoId = attributes.getValue(String.join(":", NS_XML, ATT_ID));
				annoId2FreeAnnotations.put(currentAnnoId, new HashMap<String, String>());	
			}
			else if (TAG_F.equals(localName)) {
				currentAnnoLayer = attributes.getValue(ATT_NAME);			
			}
			else if (TAG_SYMBOL.equals(localName)) {
				if (TAG_F.equals(stack.peek())) {
					annoId2FreeAnnotations.get(currentAnnoId).put(currentAnnoLayer, attributes.getValue(ATT_VALUE));
				}
			}
			else if (TAG_PAUSE.equals(localName)) {
				LinguisticParent parent = parentStack.peek();
				TokenLike pause = new TokenLike(ElementType.DIPL, null);
				parent.addElement(pause);
				pause.addAnnotation(PAUSE_LAYER_NAME, attributes.getValue(ATT_TYPE));
			}
			else if (TAG_CHOICE.equals(localName)) {
				overlap = Pair.of(new TokenLike(ElementType.DIPL, null), new TokenLike(ElementType.NORM, null));
				overlap.getLeft().setOverlap(overlap.getRight());
				overlap.getRight().setOverlap(overlap.getLeft());
			}
			else if (TAG_ADD.equals(localName)) {
				overlap = Pair.of(new TokenLike(ElementType.DIPL, textBuffer), new TokenLike(ElementType.NORM, textBuffer, currentT.getId()));
				overlap.getLeft().setOverlap(overlap.getRight());
				overlap.getRight().setOverlap(overlap.getLeft());
				currentT = null;
			}
			else if (TAG_SEG.equals(localName)) {
				LinguisticParent parent = parentStack.peek();
				LinguisticParent seg = new LinguisticParent(parent.getSpeaker(), -1, -1);
				parent.addElement(seg);
				parentStack.push(seg);
				seg.setId(attributes.getValue(String.join(":", NS_XML, ATT_ID)));
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
				currentUtterance.setId(attributes.getValue(String.join(":", NS_XML, ATT_ID)));
			}
			else if (TAG_SPAN.equals(localName)) {
				if (!group2AnnotationMapping.containsKey(currentSpanGroupType)) {
					group2AnnotationMapping.put(currentSpanGroupType, new HashMap<String, List<String>>());
				}
				HashMap<String, List<String>> annoMapping = group2AnnotationMapping.get(currentSpanGroupType);				
				String ana = attributes.getValue(ATT_ANA);
				String target = attributes.getValue(ATT_TARGET);
				if (ana != null && target != null) {
					String a = ana.replace("#", "");
					String tgt = target.replace("#", "");
					if (!annoMapping.containsKey(tgt)) {
						annoMapping.put(tgt, new ArrayList<String>());
					}
					annoMapping.get(tgt).add(a);
				}
			}
			else if (TAG_SPANGRP.equals(localName)) {
				currentSpanGroupType = attributes.getValue(ATT_TYPE);
			}
			else if (TAG_STANDOFF.equals(localName)) {
				currentStandoffType = attributes.getValue(ATT_TYPE);
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
			else if (TAG_STRING.equals(localName)) {
				if (TAG_F.equals(stack.peek())) {
					annoId2FreeAnnotations.get(currentAnnoId).put(currentAnnoLayer, textBuffer);
				}
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
//				debugPrintCollectedData();
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
		
		private void addAnnotations(SequenceElement e, SToken token, String speaker, boolean addFreeAnnotations) {
			SAnnotation a = null;			
			for (Entry<String, String> akv : e.getAnnotations().entrySet()) {
				a = SaltFactory.createSAnnotation();
				a.setName(akv.getKey());
				a.setValue(akv.getValue());
				token.addAnnotation(a);
			}
			String tokenId = e.getId();
			if (addFreeAnnotations && tokenId != null) {
				for (Entry<String, HashMap<String, List<String>>> g2m : group2AnnotationMapping.entrySet()) {
					List<String> freeAnnotationIds = g2m.getValue().get(tokenId);
					if (freeAnnotationIds != null) {
						for (String annoId : freeAnnotationIds) {
							for (Entry<String, String> freeKVPair : annoId2FreeAnnotations.get(annoId).entrySet()) {
								a = SaltFactory.createSAnnotation();								
								a.setName(String.join("_", speaker, freeKVPair.getKey()));
								a.setValue(freeKVPair.getValue());
								token.addAnnotation(a);
							}
						}
					}
				}				
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
			for (int i = 0; i < corpusData.size(); i++) { //FIXME Dangerous, when people start exactly the same time;
				LinguisticParent lp = corpusData.get(i);
				start2utterance.put(lp.getStart(), lp);
				orderedTimes[i] = lp.getStart();
			}
			Arrays.sort(orderedTimes);
			// build document utterance by utterance
			/* collect and build base texts */
			StringBuilder dipl = null;
			StringBuilder norm = null;
			StringBuilder pause = null;
			HashMap<String, Triple<StringBuilder, StringBuilder, StringBuilder>> baseTexts = new HashMap<>();
			Triple<StringBuilder, StringBuilder, StringBuilder> basePair = null;
			String space = " ";
			String speaker = null;
			debugMessage("Collecting base texts ....");
			boolean[] overlap2previous = new boolean[orderedTimes.length];
			long previousEndTime = -1;
			for (int i = 0; i < orderedTimes.length; i++) {				
				SequenceElement e = start2utterance.get(orderedTimes[i]);
				speaker = ((LinguisticParent) e).getSpeaker().replace("#", "");
				basePair = baseTexts.get(speaker);
				debugMessage("prev:", Long.toString(previousEndTime), "new:", Long.toString(((LinguisticParent) e).getStart()));
				overlap2previous[i] = (previousEndTime > ((LinguisticParent) e).getStart()); //TODO introduce regulating property
				previousEndTime = ((LinguisticParent) e).getEnd();
				if (basePair == null) {
					basePair = Triple.of(new StringBuilder(), new StringBuilder(), new StringBuilder());
					baseTexts.put(speaker, basePair);
				}
				dipl = basePair.getLeft();
				pause = basePair.getMiddle();
				norm = basePair.getRight();
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
							else if (ElementType.DIPL.equals(etype)) {//TODO does it work?
								pause.append(e.getAnnotations().get(PAUSE_LAYER_NAME));
							}
						}
						if (!iteratorStack.peek().hasNext()) {
							iteratorStack.pop();
						}
					}
				}
			}
			/*final work on graph*/
			SDocumentGraph docGraph = getDocument().getDocumentGraph();
			STimeline timeline = docGraph.createTimeline();
			HashMap<String, Triple<STextualDS, STextualDS, STextualDS>> dataSources = new HashMap<>();
			HashMap<String, Triple<Integer, Integer, Integer>> positions = new HashMap<>();
			HashMap<String, Triple<ArrayList<SToken>, ArrayList<SToken>, ArrayList<SToken>>> tokenLists = new HashMap<>();
			for (Entry<String, Triple<StringBuilder, StringBuilder, StringBuilder>> entry : baseTexts.entrySet()) {
				speaker = entry.getKey();
				basePair = entry.getValue();
				dataSources.put(speaker, 
						Triple.of(	docGraph.createTextualDS(basePair.getLeft().toString().trim()),
									docGraph.createTextualDS(basePair.getMiddle().toString().trim()),
									docGraph.createTextualDS(basePair.getRight().toString().trim())
						)
				);
				positions.put(speaker, Triple.of(0, 0, 0));
				tokenLists.put(speaker, Triple.of(new ArrayList<SToken>(), new ArrayList<SToken>(), new ArrayList<SToken>()));
			}
			STextualDS diplDS = null;
			STextualDS normDS = null;		
			STextualDS pauseDS = null;
			int d = 0;
			int n = 0;
			int p = 0;
			int nv = 0;
			List<SToken> diplTokens = null;
			List<SToken> normTokens = null;		
			List<SToken> pauseTokens = null;
			debugMessage("Building graph ...");
			SingularEvent timeShift;			
			for (int i = 0; i < orderedTimes.length; i++) {
				timeShift = new SingularEvent(overlap2previous[i]);
				SequenceElement e = start2utterance.get(orderedTimes[i]);
				speaker = ((LinguisticParent) e).getSpeaker().replace("#", "");
				Triple<ArrayList<SToken>, ArrayList<SToken>, ArrayList<SToken>> tlPair = tokenLists.get(speaker);
				diplTokens = tlPair.getLeft();
				normTokens = tlPair.getRight();
				pauseTokens = tlPair.getMiddle();
				diplDS = dataSources.get(speaker).getLeft();
				normDS = dataSources.get(speaker).getRight();
				pauseDS = dataSources.get(speaker).getMiddle();
				d = positions.get(speaker).getLeft();
				n = positions.get(speaker).getRight();
				p = positions.get(speaker).getMiddle();
				Stack<Iterator<SequenceElement>> iteratorStack = new Stack<>();				
				iteratorStack.push(e.getElements().iterator());
				/*syntax*/
				Stack<SStructure> treeNodeStack = new Stack<>();
				SStructure child = SaltFactory.createSStructure();
				SCatAnnotation anno = SaltFactory.createSCatAnnotation();
				anno.setValue(CAT_U);
				child.addAnnotation(anno);
				treeNodeStack.push(child);
				SDominanceRelation domRel = null;
				Set<SDominanceRelation> domRels = new HashSet<SDominanceRelation>();
				/*end of syntax*/
				while (!iteratorStack.isEmpty() && iteratorStack.peek().hasNext()) { //FIXME get rid of second condition
					e = iteratorStack.peek().next();
					ElementType etype = e.getElementType();
					if (ElementType.PARENT.equals(etype)) {
						iteratorStack.push(e.getElements().iterator());
						{/*Syntax*/
							child = SaltFactory.createSStructure();
							anno = SaltFactory.createSCatAnnotation();
							anno.setValue(CAT_SEG);
							child.addAnnotation(anno);
							domRel = SaltFactory.createSDominanceRelation();
							domRel.setSource(treeNodeStack.peek());
							domRel.setTarget(child);
							domRels.add(domRel);
							treeNodeStack.push(child);
						}
					}
					else {
						if (ElementType.ALL.equals(etype) || ElementType.DIPL.equals(etype)) {
							if (e.getValue() != null) {
								nv = d + e.getValue().length();			
								SToken tok = docGraph.createToken(diplDS, d, nv);
								diplTokens.add(tok);								
								d = nv + 1;
								addTimelineRelation(timeline, tok, !timeShift.getValue());
								addAnnotations(e, tok, speaker, false);
								SequenceElement ov = ElementType.ALL.equals(etype)? e : e.getOverlap();
								if (ov != null && ov.getValue() != null) {
									nv = n + ov.getValue().length();
									tok = docGraph.createToken(normDS, n, nv);
									normTokens.add(tok);
									addTimelineRelation(timeline, tok, false);
									addAnnotations(ov, tok, speaker, true);
									n = nv + 1;
									{/*Syntax*/
										child = treeNodeStack.peek();
										domRel = SaltFactory.createSDominanceRelation();
										domRel.setSource(child);
										domRel.setTarget(tok);
										domRels.add(domRel);
									}
								}
								if (ov == null) {
									if (ElementType.DIPL.equals(etype)) {
										Iterator<SRelation> relations = normTokens.get(normTokens.size() - 1).getOutRelations().iterator();
										SRelation rel = null;
										while (!(rel instanceof STimelineRelation)) {
											rel = relations.next();
										}
										STimelineRelation tRel = (STimelineRelation) rel;
										tRel.setEnd(tRel.getEnd() + 1);
									}
									else if (ElementType.NORM.equals(etype)) {
										Iterator<SRelation> relations = diplTokens.get(normTokens.size() - 1).getOutRelations().iterator();
										SRelation rel = null;
										while (!(rel instanceof STimelineRelation)) {
											rel = relations.next();
										}
										STimelineRelation tRel = (STimelineRelation) rel;
										tRel.setEnd(tRel.getEnd() + 1);
									}
								}
							}
							else if (ElementType.DIPL.equals(etype)) {
								String value = e.getAnnotations().get(PAUSE_LAYER_NAME); 
								nv = p + value.length();
								SToken tok = docGraph.createToken(pauseDS, p, nv);
								pauseTokens.add(tok);
								addTimelineRelation(timeline, tok, !timeShift.getValue());
								p = nv + 1;
							}
						}
						if (!iteratorStack.peek().hasNext()) {
							iteratorStack.pop();
							{/*Syntax*/
								docGraph.addNode(treeNodeStack.pop());
							}
						}
					}
				}
				positions.put(speaker, Triple.of(d, p, n));
				{/*Syntax*/
					docGraph.addNode(treeNodeStack.pop());
					for (SDominanceRelation drl : domRels) {
						docGraph.addRelation(drl);						
					}	
				}				
			}
			for (Entry<String, Triple<ArrayList<SToken>, ArrayList<SToken>, ArrayList<SToken>>> entry : tokenLists.entrySet()) {
				speaker = entry.getKey();
				diplTokens = entry.getValue().getLeft();
				normTokens = entry.getValue().getRight();
				pauseTokens = entry.getValue().getMiddle();				
				SOrderRelation orderRel = null;
				List<List<SToken>> tokenSources = new ArrayList<>();
				tokenSources.add(diplTokens);
				tokenSources.add(normTokens);
				tokenSources.add(pauseTokens);				
				if (pauseTokens.size() < 2) { 
					/*add dummy token for pause layer to have at least one order relation*/
					pauseDS = dataSources.get(speaker).getMiddle();
					pauseTokens.add(docGraph.createToken(pauseDS, pauseDS.getEnd(), pauseDS.getEnd()));
				}
				String[] names = {speaker + "_dipl", speaker + "_norm", speaker + "_pause"};  //FIXME obtain from properties
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
			/*DEBUG*/
			ExportFilter exportFilter = new ExportFilter() {				
				@Override
				public boolean includeRelation(SRelation relation) {
					return (relation instanceof SDominanceRelation);
				}
				
				@Override
				public boolean includeNode(SNode node) {
					for (SRelation r : node.getInRelations()) {
						if (r instanceof SDominanceRelation) {
							return true;
						}
					}
					for (SRelation r : node.getOutRelations()) {
						if (r instanceof SDominanceRelation) {
							return true;
						}
					}
					return false;
				}
			};
			debugVis(exportFilter);		
			
			buildSyntax();
		}
		
		private void debugVis(ExportFilter exportFilter) {
			VisJsVisualizer vis;
			try {				
				vis = new VisJsVisualizer(getDocument(), exportFilter, null);				
				vis.visualize(URI.createFileURI("./vis/"));
			} catch (IOException | SaltException | XMLStreamException e) {
				e.printStackTrace();
			}			
		}
	}
	
	private class SingularEvent{
		private boolean value;
		
		public SingularEvent(boolean initial) {
			this.value = initial;
		}
		
		public boolean getValue() {
			boolean v = this.value;
			this.value = false;
			return v;
		}
	}
	
	private void buildSyntax() {
		/*introduce artificial tree and text*/
		if (true) {
			return;
		}
		STextualDS ds = getDocument().getDocumentGraph().createTextualDS("c était qqn qqn qui s int bc aux autres aux gens en général");
		List<SToken> testTokens = ds.tokenize();
		SOrderRelation oRel = null;
		STimeline timeline = getDocument().getDocumentGraph().getTimeline();
		STimelineRelation tRel = SaltFactory.createSTimelineRelation();
		tRel.setSource(testTokens.get(0));
		tRel.setTarget(timeline);		
		tRel.setStart(timeline.getEnd());
		timeline.increasePointOfTime();
		tRel.setEnd(timeline.getEnd());
		String name = "test";
				
		for (int i=1; i < testTokens.size(); i++) {
			oRel = SaltFactory.createSOrderRelation();
			oRel.setSource(testTokens.get(i - 1));
			oRel.setTarget(testTokens.get(i));
			oRel.setName(name);
			
			tRel.setSource(testTokens.get(i));
			tRel.setTarget(timeline);		
			tRel.setStart(timeline.getEnd());
			timeline.increasePointOfTime();
			tRel.setEnd(timeline.getEnd());
		}
		
		
		String cat = "cat";
		List<SStructure> structures = new ArrayList<>();
		SDocumentGraph graph = getDocument().getDocumentGraph();
		
		/*0*/
		SStructure structure = graph.createStructure(testTokens.get(0));
		structure.createAnnotation(null, cat, "PRN");		
		structures.add(structure);
		
		structure = graph.createStructure(testTokens.get(4));
		structure.createAnnotation(null, cat, "PRN");		
		structures.add(structure);
		
		structure = graph.createStructure(testTokens.get(5));
		structure.createAnnotation(null, cat, "PRN");		
		structures.add(structure);
		
		structure = graph.createStructure(testTokens.get(7));
		structure.createAnnotation(null, cat, "ADV");		
		structures.add(structure);
		
		structure = graph.createStructure(testTokens.get(8));
		structure.createAnnotation(null, cat, "D");		
		structures.add(structure);
		
		structure = graph.createStructure(testTokens.get(10));
		structure.createAnnotation(null, cat, "D");		
		structures.add(structure);
		
		structure = graph.createStructure(testTokens.get(12), testTokens.get(13));
		structure.createAnnotation(null, cat, "ADV");		
		structures.add(structure);
		
		/*1*/		
		structure = graph.createStructure(testTokens.get(9), structures.get(4));
		structure.createAnnotation(null, cat, "Q");		
		structures.add(structure);
		
		structure = graph.createStructure(structures.get(5), testTokens.get(11), structures.get(6));
		structure.createAnnotation(null, cat, "N");		
		structures.add(structure);
		
		/*2*/		
		structure = graph.createStructure(testTokens.get(10), structure);
		structure.createAnnotation(null, cat, "P");		
		structures.add(structure);
		
		/*3*/		
		structure = graph.createStructure(structure);
		structure.createAnnotation(null, cat, "COORD");		
		structures.add(structure);
		
		/*4*/		
		structure = graph.createStructure(testTokens.get(8), structures.get(7), structure);
		structure.createAnnotation(null, cat, "P");		
		structures.add(structure);
		
		/*5*/		
		structure = graph.createStructure(structures.get(1), structures.get(2), testTokens.get(6), structure);
		structure.createAnnotation(null, cat, "V");		
		structures.add(structure);
		
		/*6*/
		structure = graph.createStructure(testTokens.get(3), structure);
		structure.createAnnotation(null, cat, "PRN");		
		structures.add(structure);
		
		/*7*/
		structure = graph.createStructure(structures.get(0), testTokens.get(1), structure);
		structure.createAnnotation(null, cat, "V");		
		structures.add(structure);
		
		/*8*/
		structure = graph.createStructure(structure);
		structure.createAnnotation(null, cat, "ROOT");		
		structures.add(structure);
	}
}
