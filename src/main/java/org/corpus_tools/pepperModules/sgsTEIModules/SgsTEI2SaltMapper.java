package org.corpus_tools.pepperModules.sgsTEIModules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

import org.apache.commons.lang3.tuple.Pair;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.pepperModules.sgsTEIModules.SgsTEIImporterUtils.READ_MODE;
import org.corpus_tools.pepperModules.sgsTEIModules.SgsTEIImporterUtils.TextBuffer;
import org.corpus_tools.pepperModules.sgsTEIModules.builders.GraphBuilder;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.core.SLayer;
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
	
	protected SgsTEIImporterProperties getModuleProperties() {
		return new SgsTEIImporterProperties(); //FIXME
	}
		
	/**
	 * This sub class is the mapper's callback handler processing the input xml.
	 * @author klotzmaz
	 *
	 */
	private class SgsTEIReader extends DefaultHandler2{		

		private static final String PLACEHOLDER = ".";

		private final Logger logger = LoggerFactory.getLogger(SgsTEIReader.class);
		
		/** This is the element stack representing the hierarchy of elements to be closed. */
		private Stack<String> stack;
		/** This variable is keeping track of read characters */
		private final TextBuffer textBuffer;
		/** what are we currently reading? */
		private READ_MODE mode; 
		/** */
		private GraphBuilder builder;
		
		private SgsTEIImporterUtils utils;
		
		private Map<READ_MODE, SLayer> layers;
		
		private String annotationName;
		
		private String currentId;
		
		private String speaker;
		
		private Map<String, String> token2text;
		
		private Queue<Pair<String, String>> spanSeq;
		
		private List<Map<String, List<String>>> sequence;
		
		private Map<String, Collection<String>> comesWith;
		
		private Map<String, String> anaId2targetId;
		
		private int[] bufferMode;
		
		private boolean overlap;
						
		private final String NORM = getModuleProperties().getNormName();		
		private final String DIPL = getModuleProperties().getDiplName();
		private final String PAUSE = getModuleProperties().getPauseName();
		private final String SYN = getModuleProperties().getSynSegName();
		
		public SgsTEIReader() {
			/*internal*/
			utils = new SgsTEIImporterUtils();
			stack = new Stack<String>();
			textBuffer = utils.getTextBuffer();
			mode = READ_MODE.BLIND;
			layers = new HashMap<>();
			builder = new GraphBuilder(SgsTEI2SaltMapper.this);
			token2text = new HashMap<>();
			sequence = new ArrayList<>();
			comesWith = new HashMap<>();
			anaId2targetId = new HashMap<>();
			bufferMode = new int[] {0, 1};
			spanSeq = new LinkedList<>();
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
			else if (TAG_PC.equals(localName) && READ_MODE.TEXT.equals(mode)) {	
			}
			else if (TAG_U.equals(localName)) {
				speaker = attributes.getValue(ATT_WHO).substring(1);
			}
			else if (TAG_SPAN.equals(localName)) {
				String targetId = attributes.getValue(ATT_TARGET);
				targetId = targetId == null? null : targetId.substring(1);
				if (READ_MODE.REFERENCE.equals(mode)) {
//					builder.registerReferringExpression(attributes.getValue( String.join(":", NS_XML, ATT_ID) ), attributes.getValue(ATT_TARGET).substring(1));
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
			else if (TAG_SPANGRP.equals(localName) && READ_MODE.MORPHOSYNTAX.equals(mode)) {
			}
			else if (TAG_STANDOFF.equals(localName)) {
				mode = READ_MODE.getMode(attributes.getValue(ATT_TYPE));
			}
			else if (TAG_SYMBOL.equals(localName) || TAG_NUMERIC.equals(localName)) {
				if (TAG_F.equals(stack.peek())) {
					builder.registerAnnotation(anaId2targetId.get(currentId), annotationName, attributes.getValue(ATT_VALUE), READ_MODE.MORPHOSYNTAX.equals(mode));
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
//					builder.registerDiscourseEntity(id, attributes.getValue(ATT_INST).substring(1), anaId);
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
//					builder.registerReferenceLink(attributes.getValue(String.join(":", NS_XML, ATT_ID)), attributes.getValue(ATT_TYPE), targetSource[1].substring(1), targetSource[0].substring(1));
				}
			}
			else if (TAG_WHEN.equals(localName) && READ_MODE.TEXT.equals(mode)) {
			}
			else if (TAG_TEXT.equals(localName)) {
				mode = READ_MODE.TEXT;
				textBuffer.clear(0);
				textBuffer.clear(1);
				System.out.println(comesWith);
			}
			stack.push(localName);
		}
		
		/* warning: this method should always concatenate, since sometimes several calls are used for text-node (built in multiple steps) */
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (READ_MODE.TEXT.equals(mode) || READ_MODE.MORPHOSYNTAX.equals(mode)) {
				String next = (new String(Arrays.copyOfRange(ch, start, start + length))).trim();
				textBuffer.append(next, bufferMode);
			}
		}
		
		/**
		 * 
		 * @param id can be null
		 * @param speaker
		 * @param dipl
		 * @param norm
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
				registerToken(emptyId, speaker, SYN, PLACEHOLDER, timestep);
				sequence.add(timestep);
				timestep = new HashMap<>();
			}
			if (pause) {
				registerToken(null, speaker, PAUSE, value, timestep);
			} else {
				if (dipl) {
					registerToken(null, speaker, DIPL, textBuffer.clear(1), timestep);
				}
				if (norm) {
					registerToken(id, speaker, NORM, textBuffer.clear(0), timestep);
				}
				if (id != null && comesWith.containsKey(id)) {					
					String qName = builder.getQName(speaker, SYN);
					if (!timestep.containsKey(qName)) {
						timestep.put(qName, new ArrayList<String>());
					}
					List<String> synchronousIds = timestep.get(qName);
					for (String synTokenId : comesWith.get(id)) {
						token2text.put(builder.registerToken(synTokenId, speaker, SYN), PLACEHOLDER);
						synchronousIds.add(synTokenId);
					}
				}
			}
			sequence.add(timestep);
			overlap = false;			
		}
		
		/** 
		 * 
		 * @param id
		 * @return id of empty span to be created, else null
		 */
		private String checkForEmpty(String id) {
			Pair<String, String> spanIds = spanSeq.poll();
			if (spanIds.getRight() != null) {
				return null;
			} else {
				return spanIds.getLeft();
			}
		}
		
		private void registerToken(String id, String speaker, String level, String text, Map<String, List<String>> timestep) {
			String idd = builder.registerToken(id, speaker, level);
			token2text.put(idd, text);
			String qName = builder.getQName(speaker, level);
			if (!timestep.containsKey(qName)) {
				timestep.put(qName, new ArrayList<String>());
			}
			timestep.get(qName).add(idd);
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			localName = qName.substring(qName.lastIndexOf(":") + 1);		
			stack.pop();
			if (TAG_W.equals(localName) && READ_MODE.TEXT.equals(mode)) {
				//TODO differentiate
				tokenDetected(currentId, speaker, true, true, null);
			}
			else if (TAG_PC.equals(localName)) {
				tokenDetected(null, speaker, true, true, null);
			}
			else if (TAG_ADD.equals(localName)) {
				bufferMode = new int[] {0, 1};
			}
			else if (TAG_DESC.equals(localName) && TAG_VOCAL.equals(stack.peek())) {
				tokenDetected(null, speaker, true, false, null);
				bufferMode = new int[] {0, 1};
			}
			else if (TAG_SIC.equals(localName) && READ_MODE.TEXT.equals(mode)) {
				tokenDetected(null, speaker, true, false, null);
			}
			else if (TAG_F.equals(localName)) {
				annotationName = null;
			}
			else if (TAG_FS.equals(localName)) {
				currentId = null;
			}
			else if (TAG_STRING.equals(localName)) {
				if (TAG_F.equals(stack.peek())) {
					builder.registerAnnotation(anaId2targetId.get(currentId), annotationName, textBuffer.clear(0), READ_MODE.MORPHOSYNTAX.equals(mode));
				}
			}
			else if (TAG_U.equals(localName)) {
			}
			else if (TAG_TEI.equals(localName)) {
				builder.setGlobalEvaluationMap(token2text);
				builder.build(sequence);
			}
		}
	}	
}
