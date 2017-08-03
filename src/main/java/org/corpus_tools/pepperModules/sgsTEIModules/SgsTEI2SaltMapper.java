package org.corpus_tools.pepperModules.sgsTEIModules;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleDataException;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.core.SAnnotation;
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
	
	private enum SegmentationLayer {
		DIPL, NORM		
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
		/** Since tokens are read after all annotations, we need to map them to dummy values first. */
		private Map<String, List<SAnnotation>> diplAnnotations;
		/** Since tokens are read after all annotations, we need to map them to dummy values first. */
		private Map<String, List<SAnnotation>> normAnnotations;		
		/** This indicates the currently active segmentation layer */
		private SegmentationLayer currentSegmentation;
		
		
		public SgsTEIReader() {
			stack = new Stack<String>();
			diplAnnotations = new HashMap<String, List<SAnnotation>>();
			normAnnotations = new HashMap<String, List<SAnnotation>>();	
		}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			localName = localName.substring(localName.lastIndexOf(":") + 1);
			
			stack.push(localName);
		}
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			StringBuilder text = new StringBuilder();
			for (char c : ch) {
				text.append(c);
			}
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			localName = localName.substring(localName.lastIndexOf(":") + 1);
			String stackTop = stack.pop();
			if (!localName.equals(stackTop)) {
				throw new PepperModuleDataException(SgsTEI2SaltMapper.this, ERR_MSG_STACK_INCONSISTENCY_EXCEPTION);
			}
		}
	}
}
