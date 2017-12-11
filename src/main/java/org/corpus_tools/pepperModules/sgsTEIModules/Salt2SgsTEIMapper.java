package org.corpus_tools.pepperModules.sgsTEIModules;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleDataException;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SOrderRelation;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SRelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Salt2SgsTEIMapper extends PepperMapperImpl implements SgsTEIDictionary{
	private static final Logger logger = LoggerFactory.getLogger(Salt2SgsTEIMapper.class);
	private static final String ERR_MSG_NO_DOCUMENT = "No document provided.";
	private static final String WARN_EMPTY_DOCUMENT = "Document is empty (no document graph).";
	
	private XMLStreamWriter xml;
	private String markableSegName;
	private String alternateSegName;
	
	public DOCUMENT_STATUS mapSDocument() {
		SDocument doc = getDocument();
		if (doc == null) {
			throw new PepperModuleDataException(this, ERR_MSG_NO_DOCUMENT);
		}
		SDocumentGraph docGraph = doc.getDocumentGraph();
		if (docGraph == null) {
			logger.warn(WARN_EMPTY_DOCUMENT);
			return DOCUMENT_STATUS.COMPLETED;
		}
		try {
			init();
		} catch (XMLStreamException | FactoryConfigurationError e) {
			logger.error(e.getMessage());
			throw new PepperModuleException();
		}
		try {
			startDocument();		
			mapReferences();
			mapSyntax();
			mapMorphosyntax();
			mapPrimaryData();			
			endDocument();
		} catch (XMLStreamException e) {
			throw new PepperModuleException(); //TODO ERR_MSG
		}
		return DOCUMENT_STATUS.COMPLETED;
	}
	
	private void endDocument() throws XMLStreamException {
		xml.writeEndElement(); //finish of TEI
		xml.writeEndDocument();
	}

	private void startDocument() throws XMLStreamException {
		xml.writeStartDocument();
		xml.writeNamespace(null, URI_NS_XML);
		xml.writeNamespace(NS_SO, URI_NS_SO);
		xml.writeStartElement(TAG_TEI);
	}
	
	private void init() throws XMLStreamException, FactoryConfigurationError {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		xml = XMLOutputFactory.newFactory().createXMLStreamWriter(outStream);	
		markableSegName = ((SgsTEIExporterProperties) getProperties()).getMainSegmentationName();
		alternateSegName = ((SgsTEIExporterProperties) getProperties()).getAlternateSegmentationName();
	}
	
	private void mapPrimaryData() throws XMLStreamException {
		xml.writeStartElement(TAG_TEXT);
		
		xml.writeEndElement();
	}
	
	private List<Pair<String, String>> getTokens() throws XMLStreamException{
		List<Pair<String, String>> tokens = new ArrayList<>();
		SDocumentGraph docGraph = getDocument().getDocumentGraph();
		STimeline timeline = docGraph.getTimeline();
		Map<Integer, Pair<SToken, SToken>> start2STokenPair = getTokensByTime(timeline);
		for (int t = 0; t < timeline.getEnd(); t++) {
			Pair<SToken, SToken> mainAlternate = start2STokenPair.get(t);
			if (mainAlternate != null && mainAlternate.getLeft() != null) {//must have left value, i. e. a main value
				SToken main = mainAlternate.getLeft();
				SToken alternate = mainAlternate.getRight();
				if (alternate == null) {
					// write w for main, alternate does not exist
					writeW(main);
				}
				else {
					// write w with add or completely different
					writeWwithAlternate(main, alternate);
				}
			}
		}
		return tokens;
	}
	
	private void writeWwithAlternate(SToken main, SToken alternate) {
		// TODO Auto-generated method stub
		
	}

	private void writeW(SToken sTok) throws XMLStreamException {
		xml.writeStartElement(TAG_W);
//		xml.writeAttribute(namespaceURI, localName, value);
		xml.writeEndElement();
	}
	
	private int getEnd(SToken sToken) {
		for (SRelation<?, ?> rel : sToken.getOutRelations()) {
			if (rel instanceof STimelineRelation) {
				return ((STimelineRelation) rel).getEnd();
			}
		}
		return -1;
	}
	
	private Map<Integer, Pair<SToken, SToken>> getTokensByTime(STimeline timeline) {
		Map<Integer, Pair<SToken, SToken>> retVal = new HashMap<>();
		for (SRelation<?, ?> rel : timeline.getInRelations()) {
			if (rel instanceof STimelineRelation) {
				int start = ((STimelineRelation) rel).getStart();
				if (!retVal.containsKey(start)) {
					retVal.put(start, MutablePair.<SToken, SToken>of(null, null));
				}
				SToken sTok = (SToken) rel.getSource();
				String level = getLevel(sTok);
				if (markableSegName.equals(level)) {
					((MutablePair<SToken, SToken>) retVal.get(start)).setLeft(sTok);
				}
				else if (alternateSegName != null && alternateSegName.equals(level)) {
					((MutablePair<SToken, SToken>) retVal.get(start)).setRight(sTok);
				}
			}
		}
		return retVal;
	}
	
	private Map<String, String> mapTokens(){
		Map<String, String> tokenId2Level = new HashMap<>();
		for (SToken sTok : getDocument().getDocumentGraph().getTokens()) {
			tokenId2Level.put(sTok.getId(), getLevel(sTok));			
		}
		return tokenId2Level;
	}
	
	private String getLevel(SToken token) {
		for (SRelation<?, ?> rel : token.getInRelations()) {
			if (rel instanceof SOrderRelation) {
				return rel.getType();
			}
		}
		return null;
	}
	
	private void mapMorphosyntax() {}
	
	private void mapSyntax() {}
	
	private void mapReferences() {}
}
