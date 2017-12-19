package org.corpus_tools.pepperModules.sgsTEIModules;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;

import org.apache.commons.lang3.tuple.MutablePair;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
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
		return (SgsTEIImporterProperties) getProperties();
	}
	
	/* (BURN) AFTER READING */
	
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
		/** */
		private GraphBuilder builder;
		
		private SgsTEIImporterUtils utils;
		
		private HashMap<READ_MODE, SLayer> layers;
		
		private String annotationName;
		
		private String annotationId;
				
		private static final String SPACE = " ";		
		private final String NORM = getModuleProperties().getNormName();		
		private final String DIPL = getModuleProperties().getDiplName();
		private final String PAUSE = getModuleProperties().getPauseName();
		private final String SYN = getModuleProperties().getSynSegName();
		private static final String DELIMITER = "_";
		
		public SgsTEIReader() {
			/*internal*/
			utils = new SgsTEIImporterUtils();
			stack = new Stack<String>();
			textBuffer = utils.getTextBuffer();
			mode = READ_MODE.BLIND;
			layers = new HashMap<>();
			builder = new GraphBuilder(SgsTEI2SaltMapper.this);
		}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			localName = qName.substring(qName.lastIndexOf(":") + 1);
			if (TAG_W.equals(localName)) {
			}
			else if (TAG_ADD.equals(localName) && READ_MODE.TEXT.equals(mode)) {
			}
			else if (TAG_PAUSE.equals(localName) && READ_MODE.TEXT.equals(mode)) {			
			}
			else if (TAG_PC.equals(localName) && READ_MODE.TEXT.equals(mode)) {		
			}
			else if (TAG_U.equals(localName)) {
			}
			else if (TAG_SPAN.equals(localName)) {
				if (READ_MODE.REFERENCE.equals(mode)) {
					builder.registerReferringExpression(attributes.getValue(String.join(":", NS_XML, ATT_ID)), attributes.getValue(ATT_TARGET).substring(1));
				}				
				else if (READ_MODE.MORPHOSYNTAX.equals(mode)) {
					
				}
			}
			else if (TAG_STANDOFF.equals(localName)) {
				mode = READ_MODE.getMode(attributes.getValue(ATT_TYPE));
			}
			else if (TAG_SYMBOL.equals(localName) || TAG_NUMERIC.equals(localName)) {
				if (TAG_F.equals(stack.peek())) {
					builder.registerAnnotation(annotationId, annotationName, attributes.getValue(ATT_VALUE));
				}
			}
			else if (TAG_F.equals(localName)) {
				annotationName = attributes.getValue(ATT_NAME);
			}
			else if (TAG_FS.equals(localName)) {
				annotationId = attributes.getValue(String.join(":", NS_XML, ATT_ID));
			}
			else if (TAG_INTERP.equals(localName)) {
				String id = attributes.getValue(String.join(":", NS_XML, ATT_ID));
				String anaId = attributes.getValue(ATT_ANA).substring(1);
				if (READ_MODE.REFERENCE.equals(mode)) {
					builder.registerDiscourseEntity(id, attributes.getValue(ATT_TARGET).substring(1), anaId);
				}
				else if (READ_MODE.SYNTAX.equals(mode)) {
					builder.registerSyntaxNode(id, attributes.getValue(ATT_INST).substring(1), anaId);
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
			else if (TAG_WHEN.equals(localName) && READ_MODE.TEXT.equals(mode)) {
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
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			localName = qName.substring(qName.lastIndexOf(":") + 1);			
			stack.pop();
			if (TAG_W.equals(localName)) {
			}
			else if (TAG_PC.equals(localName)) {
			}
			else if (TAG_ADD.equals(localName)) {
			}
			else if (TAG_DESC.equals(localName) && TAG_VOCAL.equals(stack.peek())) {
			}
			else if (TAG_SIC.equals(localName) && READ_MODE.TEXT.equals(mode)) {
			}
			else if (TAG_F.equals(localName)) {
				annotationName = null;
			}
			else if (TAG_FS.equals(localName)) {
				annotationId = null;
			}
			else if (TAG_STRING.equals(localName)) {
				if (TAG_F.equals(stack.peek())) {
					builder.registerAnnotation(annotationId, annotationName, textBuffer.clear());
				}
			}
			else if (TAG_U.equals(localName)) {
			}
			else if (TAG_TEI.equals(localName)) {			
				buildGraph();
			}
		}
		
		private void buildGraph() {		
		}
	}
	
	
}
