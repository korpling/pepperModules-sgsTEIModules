package org.corpus_tools.pepperModules.sgsTEIModules.builders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleDataException;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SDominanceRelation;
import org.corpus_tools.salt.common.SPointingRelation;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SStructure;
import org.corpus_tools.salt.common.SStructuredNode;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;

public class GraphBuilder {
	private static final String F_ERR_NODE_NOT_REGISTERED = "Syntactic node is undefined: %s.";
	private static final String FUNC_NAME = "func";
	private static final String REF_TYPE_NAME = "type";
	private PepperMapper mapper;
	private SDocumentGraph graph;
	private Map<String, Segmentation> segmentations;
	/** maps the file's analysis id to all annotations listed for this id */
	private Map<String, List<SAnnotation>> annotations;
	/** maps node id (from file) to salt object */
	private Map<String, SNode> graphNodes;
	/** maps relation id (from file) to salt object */
	private Map<String, SRelation<?, ?>> graphRelations;
	/** queue of todos to be done later, since objects occur later */
	private Stack<Finisher> unfinished;
	
	public GraphBuilder(PepperMapper mapper) {
		this.mapper = mapper;
		this.graph = mapper.getDocument().getDocumentGraph();
		this.segmentations = new HashMap<>();
		this.annotations = new HashMap<>();
		this.graphNodes = new HashMap<>();
		this.graphRelations = new HashMap<>();
		this.unfinished = new Stack<Finisher>();
	}
	
	public SDocumentGraph getGraph() {
		return graph;
	}
	
	public void addSegment(String id, String segmentationName, String text, int offset, int timeSpan) {
		if (!segmentations.containsKey(segmentationName)) {
			segmentations.put(segmentationName, new Segmentation(graph.getTimeline(), segmentationName, " "));
		}
		segmentations.get(segmentationName).addElement(id, text, offset, timeSpan);
	}
	
	public void registerReferringExpression(String id, String targetNodeId) {		
		final String spanId = id;
		final String targetId = targetNodeId;
		Finisher finisher = new Finisher() {				
			@Override
			public void build() {
				List<SToken> overlappedTokens = getGraph().getSortedTokenByText( getGraph().getOverlappedTokens( getNode(targetId) ));
				SSpan span = getGraph().createSpan(overlappedTokens);
				registerNode(spanId, span);				
			}
		};
		unfinished.push(finisher);
	}
	
	public void registerDiscourseEntity(String id, String targetNodeId, String annotationId) {		
		final String nodeId = targetNodeId;
		final String anaId = annotationId;
		Finisher finisher = new Finisher() {			
			@Override
			public void build() {
				SSpan span = (SSpan) getNode(nodeId);
				for (SAnnotation anno : getAnnotions(anaId)) {
					span.addAnnotation(anno);
				}
				getGraph().addNode(span);
			}
		};
		unfinished.push(finisher);
	}
	
	public void registerAnnotation(String anaId, String name, String value) {
		SAnnotation anno = SaltFactory.createSAnnotation();
		anno.setName(name);
		anno.setValue(value);
		if (!annotations.containsKey(anaId)) {
			annotations.put(anaId, new ArrayList<SAnnotation>());
		}
		annotations.get(anaId).add(anno);
	}
	
	public void registerSyntaxNode(String id, String instanceId, String analysisId) {
		SStructure sStructure = SaltFactory.createSStructure();
		sStructure.setId(id);
		registerNode(id, sStructure);
		sStructure.createProcessingAnnotation(null, "id", analysisId);
		final String inst = instanceId;
		final String sId = id;
		final String anaId = analysisId;
		Finisher finisher = new Finisher() {			
			@Override
			public void build() {
				SStructure struct = (SStructure) getNode(sId);
				getGraph().addNode(struct);
				for (SAnnotation anno : getAnnotions(anaId)) {
					struct.addAnnotation(anno);
				}
				if (inst != null) {
					SToken instance = (SToken) getNode(inst);
					getGraph().createRelation(struct, instance, SALT_TYPE.SDOMINANCE_RELATION, null);
				}
			}
		};
		unfinished.add(finisher);
	}
	
	public void registerSyntaxLink(String id, String type, String sourceId, String targetId) {
		SNode source = getNode(sourceId);
		SNode target = getNode(targetId);
		if (source == null) {
			throw new PepperModuleDataException(mapper, String.format(F_ERR_NODE_NOT_REGISTERED, sourceId));
		} 
		else if (target == null) {
			throw new PepperModuleDataException(mapper, String.format(F_ERR_NODE_NOT_REGISTERED, targetId));
		}
		SDominanceRelation rel = SaltFactory.createSDominanceRelation();
		rel.setId(id);
		rel.setSource((SStructure) source);
		rel.setTarget((SStructuredNode) target);
		rel.createAnnotation(null, FUNC_NAME, type);
		registerRelation(id, rel);
	}
	
	public void registerReferenceLink(String id, String type, String sourceId, String targetId) {
		final String typeValue = type;
		final String srcId = sourceId;
		final String tgtId = targetId;
		Finisher finisher = new Finisher() {			
			@Override
			public void build() {
				SNode source = getNode(srcId);
				SNode target = getNode(tgtId);
				getGraph().createRelation(source, target, SALT_TYPE.SPOINTING_RELATION, String.join("=", REF_TYPE_NAME, typeValue));
			}
		};
		unfinished.push(finisher);
	}
	
	protected List<SAnnotation> getAnnotions(String anaId) {
		return annotations.get(anaId);
	}
	
	protected void registerNode(String id, SNode sNode) {
		graphNodes.put(id, sNode);
	}
	
	protected SNode getNode(String nodeId) {
		return graphNodes.get(nodeId);
	}
	
	private void registerRelation(String id, SRelation<?, ?> sRelation) {
		graphRelations.put(id, sRelation);
	}
	
	public void build() {
		SDocumentGraph graph = getGraph();
		while (!unfinished.isEmpty()) {
			Finisher top = unfinished.pop();
			top.build();
		}
	}
}
