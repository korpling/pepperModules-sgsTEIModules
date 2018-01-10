package org.corpus_tools.pepperModules.sgsTEIModules.builders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleDataException;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SDominanceRelation;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SStructure;
import org.corpus_tools.salt.common.SStructuredNode;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STextualRelation;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SNode;
import org.omg.CosNaming.NamingContextExtPackage.AddressHelper;

public class GraphBuilder {
	private static final String F_ERR_ID_USED = "ID already in use: %s.";
	private static final String FUNC_NAME = "func";
	private static final String REF_TYPE_NAME = "type";
	private static final String ERR_NO_TEMPORAL_INFO = "No temporal information available.";
	private final PepperMapper mapper;
	private final SDocumentGraph graph;
	/** maps speaker specific segmentation name to sequence of token ids */
	private Map<String, Segmentation> segmentations;
	/** maps the file's analysis id to all annotations listed for this id */
	private Map<String, Set<SAnnotation>> annotations;
	/** maps node id (from file) to salt object */
	private Map<String, SNode> graphNodes;
	/** id provider */
	private IdProvider idProvider;
	/** maps token id to segmentation name for faster processing */
	private Map<String, String> tokenId2SegName;
	/** queue of building steps */
	private Map<BUILD_STEP, Collection<BuildingBrick>> buildQueues;
	/** step queue */
	private static final BUILD_STEP[] STEP_QUEUE = new BUILD_STEP[] {BUILD_STEP.TOKEN, BUILD_STEP.SYN_TOKEN, BUILD_STEP.ANNOTATION, BUILD_STEP.SYNTAX_NODE, BUILD_STEP.SYNTAX_REL, BUILD_STEP.REFERENCE_NODE, BUILD_STEP.REFERENCE_REL};
	/** steps */
	private enum BUILD_STEP {
		TOKEN, SYN_TOKEN, SYNTAX_NODE, SYNTAX_REL, REFERENCE_NODE, REFERENCE_REL, ANNOTATION
	}
	
	private final STimeline tl;
	
	public GraphBuilder(PepperMapper pepperMapper) {
		this.mapper = pepperMapper;
		this.graph = mapper.getDocument().getDocumentGraph();
		this.segmentations = new HashMap<>();
		this.annotations = new HashMap<>();
		this.graphNodes = new HashMap<>();
		this.buildQueues = new HashMap<>();
		for (BUILD_STEP step : STEP_QUEUE) {
			buildQueues.put(step, new ArrayList<BuildingBrick>());
		}
		IdValidator validator = new IdValidator() {			
			@Override
			public String validate(Set<String> ids, String id) {
				if (id == null) {
					//request for new id
					do {
						id = Double.toHexString( Math.random() ).substring(4, 13);
					} while (ids.contains(id));
				} else {
					if (ids.contains(id)) {
						//this should never be the case
						throw new PepperModuleException(mapper, String.format(F_ERR_ID_USED, id));
					}
				}
				ids.add(id);
				return id;
			}
		};
		this.idProvider = new IdProvider(validator);
		this.tokenId2SegName = new HashMap<>();
		this.tl = pepperMapper.getDocument().getDocumentGraph().createTimeline();
		this.tl.increasePointOfTime();
	}
	
	private STimeline getTimeline() {
		return tl;
	}
	
	public SDocumentGraph getGraph() {
		return graph;
	}
	
	public void registerReferringExpression(final String id, final String targetNodeId) {	
		new BuildingBrick(buildQueues.get(BUILD_STEP.REFERENCE_NODE)) {				
			@Override
			public void build(Object... args) {
				List<SToken> overlappedTokens = getGraph().getSortedTokenByText( getGraph().getOverlappedTokens( getNode(targetNodeId) ));
				SSpan span = getGraph().createSpan(overlappedTokens);
				registerNode(id, span);				
			}

			@Override
			public void immediate() {}
		};
	}
	
	public void registerDiscourseEntity(final String id, final String[] instanceIds, final String annotationId) {		
		new BuildingBrick(buildQueues.get(BUILD_STEP.REFERENCE_NODE)) {			
			@Override
			public void build(Object... args) {
				for (String instanceId : instanceIds) {
					addAnnotations(instanceId);
				}
			}

			@Override
			public void immediate() {}
		};
	}
	
	public void registerAnnotation(final String targetId, final String name, final String value, final boolean speakerSensitive) {
		new BuildingBrick(buildQueues.get(BUILD_STEP.ANNOTATION)) {			
			@Override
			public void immediate() {}			
			@Override
			public void build(Object... args) {
				SAnnotation anno = SaltFactory.createSAnnotation();
				anno.setName(speakerSensitive? getQName(getSpeakerByTokenId(targetId), name) : name);
				anno.setValue(value);
				if (!annotations.containsKey(targetId)) {
					annotations.put(targetId, new HashSet<SAnnotation>());
				}
				annotations.get(targetId).add(anno);
			}
		};
	}
	
	public Map<String, Set<SAnnotation>> getAnnotations() {
		return annotations;
	}
	
	public void registerSyntaxNode(final String id, final String instanceId) {
		new BuildingBrick(buildQueues.get(BUILD_STEP.SYNTAX_NODE)) {			
			@Override
			public void build(Object... args) {
				SStructure sStructure = null;
				if (instanceId != null) {
					SToken instance = (SToken) getGraph().getNode(instanceId);
					sStructure = getGraph().createStructure(instance);
					registerNode(id, sStructure);
				} else {
					sStructure = SaltFactory.createSStructure();
					registerNode(id, sStructure);
				}
				addAnnotations(id); 
			}	
			@Override
			public void immediate() {}
		};
	}
	
	public void registerSyntaxLink(final String id, final String type, final String sourceId, final String targetId) {		
		new BuildingBrick(buildQueues.get(BUILD_STEP.SYNTAX_REL)) {		
			@Override
			public void immediate() {}
			
			@Override
			public void build(Object... args) {
				SNode source = getNode(sourceId);				
				SNode target = getNode(targetId);
				getGraph().createRelation(source, target, SALT_TYPE.SDOMINANCE_RELATION, String.join("=", FUNC_NAME, type));
			}
		};
	}
	
	public void registerReferenceLink(final String id, final String type, final String sourceId, final String targetId) {
		new BuildingBrick(buildQueues.get(BUILD_STEP.REFERENCE_REL)) {			
			@Override
			public void build(Object... args) {
				SNode source = getNode(sourceId);
				SNode target = getNode(targetId);
				getGraph().createRelation(source, target, SALT_TYPE.SPOINTING_RELATION, String.join("=", REF_TYPE_NAME, type));
			}

			@Override
			public void immediate() {}
		};
	}
	
	protected void registerNode(String id, SNode sNode) {
		sNode.setGraph( getGraph() );
		graphNodes.put(id, sNode);
	}
	
	protected SNode getNode(String nodeId) {
		return graphNodes.get(nodeId);
	}
	
	public void setGlobalEvaluationMap(final Map<String, String> token2text) {
		for (Entry<String, Segmentation> e : getSegmentations().entrySet()) {
			registerEvaluationMap(e.getKey(), token2text);
		}
	}
	
	public void registerEvaluationMap(String speaker, String level, Map<?, String> evaluationMap) {
		registerEvaluationMap(getQName(speaker, level), evaluationMap);
	}
	
	public void registerEvaluationMap(String qName, final Map<?, String> evaluationMap) {		
		getSegmentations().get(qName).setEvaluator(new Segmentation.Evaluator() {				
			@Override
			public String evaluate(String tokenId) {
				return evaluationMap.get(tokenId);
			}
		});
	}
	
	private void registerSegmentation(String speaker, String level, String delimiter) {
		registerSegmentation(getQName(speaker, level), delimiter);		
	}
	
	private void registerSegmentation(String segmentationName, String delimiter) {
		Segmentation seg = new Segmentation(segmentationName, delimiter);
		getSegmentations().put(segmentationName, seg);
	}

	public String registerToken(String id, String speaker, String level) {
		final String tokenId = idProvider.validate(id);		
		final String segName = getQName(speaker, level);
		new BuildingBrick(buildQueues.get(BUILD_STEP.TOKEN)) {			
			@Override
			public void build(Object... args) {
				Segmentation segmentation = getSegmentations().get(segName);
				STextualDS ds = segmentation.getDS( getGraph() );
				int[] indices = segmentation.getIndices(tokenId);
				SToken sTok = segmentation.getSToken(tokenId);
				registerNode(tokenId, sTok);
				addTextualRelation(sTok, ds, indices[0], indices[1]);
			}

			@Override
			public void immediate() {
				addSegment(segName, tokenId);
			}
		};
		return tokenId;
	}
	
	protected void addSegment(String segName, String tokenId) {
		tokenId2SegName.put(tokenId, segName);
		if (!getSegmentations().containsKey(segName)) {
			registerSegmentation(segName, " ");
		}
		getSegmentations().get(segName).addSegment(tokenId);
	}

	private String getSegmentationByTokenId(String tokenId) {
		System.out.println(tokenId);
		return tokenId2SegName.get(tokenId);
	}
	
	private String getSpeakerByTokenId(String tokenId) {
		return getSegmentationByTokenId(tokenId).split("_")[0];
	}
	
	protected Map<String, Segmentation> getSegmentations() {
		return segmentations;
	}
	
	protected STextualRelation addTextualRelation(SToken sToken, STextualDS ds, int startIndex, int endIndex) {
		STextualRelation rel = SaltFactory.createSTextualRelation();		
		rel.setStart(startIndex);
		rel.setEnd(endIndex);
		rel.setSource(sToken);
		rel.setTarget(ds);
		rel.setGraph( getGraph() );
		return rel;
	}
	
	protected STimelineRelation addTimelineRelation(SToken sToken, int from, int to) {
		if (getTimeline().getEnd() < to) {
			getTimeline().increasePointOfTime(to - getTimeline().getEnd());
		}
		STimelineRelation rel = SaltFactory.createSTimelineRelation();
		rel.setStart(from);
		rel.setEnd(to);
		rel.setSource(sToken);
		rel.setTarget( getTimeline() );
		rel.setGraph( getGraph() );
		return rel;
	}
	
	/** 
	 * This method creates the timeline relations
	 * @param temporalSequence temporal information (sequence of map name -> tokenIds)
	 */
	protected void buildTime(List<Map<String, List<String>>> temporalSequence) {
		if (temporalSequence == null || temporalSequence.isEmpty()) {
			throw new PepperModuleDataException(this.mapper, ERR_NO_TEMPORAL_INFO);
		}
		int t = 0;
		for (Map<String, List<String>> timestep : temporalSequence) {
			int length = getLength(timestep);
			for (List<String> tokenIds : timestep.values()) {
				int step = length / tokenIds.size();
				int t_ = t;
				for (int i = 0; i < tokenIds.size(); i++) {
					String tokenId = tokenIds.get(i);
					SToken tok = (SToken) getNode(tokenId);
					addTimelineRelation(tok, t_, t_ + step);
					t_ += step;
				}				
			}
			t += length;
		}
	}
	
	/**
	 * This method applies element-wise multiplication to the collection's elements.
	 * @param c
	 * @return the product of all elements of c
	 */
	private int reduceProduct(Collection<Integer> c) {
		int r = 1;
		for (Integer i : c) {
			r *= i;
		}
		return r;
	}
	
	private int getLength(Map<String, List<String>> timestep) {
		Set<Integer> lengths = new HashSet<>();
		for (Entry<String, List<String>> e : timestep.entrySet()) {
			lengths.add(e.getValue().size());
		}
		return reduceProduct(lengths);
	}
	
	private void buildOrderRelations() {
		for (String name : segmentations.keySet()) {
			buildOrderRelations(name);
		}
	}

	private void buildOrderRelations(String segmentationName) {
		List<String> sequence = getSegmentations().get(segmentationName).getSequence();
		for (int i = 1; i < sequence.size(); i++) {
			addOrderRelation(sequence.get(i - 1), sequence.get(i), segmentationName);
		}
	}

	private void addOrderRelation(String fromId, String toId, String name) {
		SToken source = (SToken) getGraph().getNode(fromId);
		SToken target = (SToken) getGraph().getNode(toId);
		getGraph().createRelation(source, target, SALT_TYPE.SORDER_RELATION, null).setType(name);
	}
	
	public String getQName(String speaker, String level) {
		return String.join("_", speaker, level);
	}
	
	private void addAnnotations(String nodeId) {
		if (getAnnotations().containsKey(nodeId)) {
			SNode node = getNode(nodeId);
			for (SAnnotation a : getAnnotations().get(nodeId)) {
				addAnnotation(node, a);
			}
			getAnnotations().remove(nodeId);
		}
	}
	
	private void addAnnotation(SNode target, SAnnotation annotation) {
		if (target instanceof SToken) {
			getGraph().createSpan((SToken) target).addAnnotation(annotation);
		} else {
			target.addAnnotation(annotation);
		}
	}
	
	private void addRemainingAnnotations() {
		for (Entry<String, Set<SAnnotation>> e : getAnnotations().entrySet()) {
			SNode node = getNode( e.getKey() );
			if (node != null) {
				for (SAnnotation a : e.getValue()) {
					addAnnotation(node, a);
				}
			}
		}
	}

	public void build(List<Map<String, List<String>>> temporalSequence) {
		for (BUILD_STEP step : STEP_QUEUE) {
			for (BuildingBrick brick : buildQueues.get(step)) {
				brick.build();
			}
		}
		buildOrderRelations();
		buildTime(temporalSequence);
		addRemainingAnnotations();
	}
}