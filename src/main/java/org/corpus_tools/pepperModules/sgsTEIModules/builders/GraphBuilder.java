package org.corpus_tools.pepperModules.sgsTEIModules.builders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.SystemUtils;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleDataException;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SStructure;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STextualRelation;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;

/**
 * This class is given all the necessary information to then build the graph.
 * It stores all the several build steps given as {@link BUILD_STEP}s and {@link BuildingBrick}s
 * and executes them in proper order. 
 * @author klotzmaz
 *
 */
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
	private static final BUILD_STEP[] STEP_QUEUE = new BUILD_STEP[] {
			BUILD_STEP.TOKEN,
			BUILD_STEP.TIME,
			BUILD_STEP.UTTERANCES,
			BUILD_STEP.SYNTAX_NODE, 
			BUILD_STEP.SYNTAX_REL, 
			BUILD_STEP.REFERENCE_REFEX, 
			BUILD_STEP.ANNOTATION, 
			BUILD_STEP.REFERENCE_DE, 
			BUILD_STEP.REFERENCE_REL, 
			BUILD_STEP.FURTHER_SPANS
	};
	protected static final String BRIDGING_RELATION = "bridging";
	/** steps */
	private enum BUILD_STEP {
		TOKEN, SYNTAX_NODE, SYNTAX_REL, REFERENCE_REFEX, REFERENCE_DE, REFERENCE_REL, ANNOTATION, FURTHER_SPANS, UTTERANCES, TIME
	}
	/** the graphs timeline */
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
	
	/**
	 * 
	 * @return the graphs {@link STimeline} object.
	 */
	private STimeline getTimeline() {
		return tl;
	}
	
	/** 
	 * 
	 * @return the document graph.
	 */
	public SDocumentGraph getGraph() {
		return graph;
	}
	
	/**
	 * With this method a referring expression can be registered and enqueued in the build process.
	 * @param id
	 * @param targetNodeId
	 */
	public void registerReferringExpression(final String id, final String targetNodeId) {	
		new BuildingBrick(buildQueues.get(BUILD_STEP.REFERENCE_REFEX)) {				
			@Override
			public void build() {
				/* re-register syntactic node with new id */
				List<SToken> overlappedTokens = getGraph().getOverlappedTokens( getNode(targetNodeId));
				registerNode(id, getGraph().createSpan(overlappedTokens) );	
			}
		};
	}
	
	/**
	 * With this method a discourse entity can be registered and enqueued in the build process.
	 * @param id
	 * @param instanceIds
	 */
	public void registerDiscourseEntity(final String id, final String[] instanceIds) {
//		System.out.println("Requesting DE: " + id + "=" + String.join(",", instanceIds));
		new BuildingBrick(buildQueues.get(BUILD_STEP.REFERENCE_DE)) {			
			@Override
			public void build() {
//				System.out.println("Building DE: " + id + "=" + String.join(",", instanceIds));
				/* current solution: first (mentioned) instance will be used by reflink, the others will be connected via p-rels*/
				for (int i = 0; i < instanceIds.length; i++) {
					SNode instance = getNode(instanceIds[i]);
					for (SAnnotation anno : getAnnotations().get(id)) {
						addAnnotation(instance, anno);
//						System.out.println("TYPE " + instance.getId() + " " + getGraph().getText(instance) + " is given annotation " + String.join("=", anno.getQName(), anno.getValue_STEXT()) + " (" + getGraph().containsNode(instance.getId()));
					} 
					if (i > 0) {
						addCorefRel(instanceIds[i], instanceIds[i - 1]); //NOTE: points backward to first mention
						addDistanceAnnotation(instanceIds[i - 1], instanceIds[i]);
					}
				}				
				getAnnotations().remove(id);				
				registerNode(id, getNode(instanceIds[0]));				
			}
		};
	}
	
	/**
	 * With this method a distance annotation for discourse entities will be computed and added to the graph
	 * as {@link SAnnotation} object.
	 * @param lastMentionId
	 * @param mentionId
	 */
	protected void addDistanceAnnotation(String lastMentionId, String mentionId) {
		List<SToken> overlappedTokens = getGraph().getSortedTokenByText( getGraph().getOverlappedTokens( getNode(lastMentionId)));
		SToken lastMention = overlappedTokens.get(overlappedTokens.size() - 1);
		SToken mention = getGraph().getSortedTokenByText( getGraph().getOverlappedTokens( getNode(mentionId))).get(0);
		int val = getSegmentations().get( getSegmentationByTokenId(mention.getId()) ).getDistance(lastMention.getId(), mention.getId());		 
		addAnnotation(mention, "given", Integer.toString(val));
	}
	
	/**
	 * This adds a coreference relation between discourse entities representing the same entity.
	 * @param fromId
	 * @param toId
	 */
	protected void addCorefRel(String fromId, String toId) {
		getGraph().createRelation(getNode(fromId), getNode(toId), SALT_TYPE.SPOINTING_RELATION, null).setType("coreference");
	}
	
	/**
	 * With this method an annotation object can be registered and enqueued in the build process.
	 * @param targetId
	 * @param name
	 * @param value
	 * @param speakerSensitive
	 */
	public void registerAnnotation(final String targetId, final String name, final String value, final boolean speakerSensitive) {
		new BuildingBrick(buildQueues.get(BUILD_STEP.ANNOTATION)) {		
			@Override
			public void build() {
				SAnnotation anno = SaltFactory.createSAnnotation();
				String lookupId = targetId;
				SNode lookupNode = getNode(lookupId);
				if (speakerSensitive && !(lookupNode instanceof SToken)) {
					lookupId = getGraph().getOverlappedTokens(lookupNode).get(0).getId();
				}
				anno.setName(speakerSensitive? getQName(getSpeakerByTokenId(lookupId), name) : name);
				anno.setValue(value);
				if (!annotations.containsKey(targetId)) {
					annotations.put(targetId, new HashSet<SAnnotation>());
				}
				annotations.get(targetId).add(anno);
			}
		};
	}
	
	/**
	 * 
	 * @return all (so far) collected {@link SAnnotation} objects. The returned object maps target ids to annotations.
	 */
	public Map<String, Set<SAnnotation>> getAnnotations() {
		return annotations;
	}
	
	/**
	 * With this method a syntax node (not a syntactical token!) can be registered and enqueued in the build process.
	 * @param id
	 * @param instanceId
	 */
	public void registerSyntaxNode(final String id, final String instanceId) {
		new BuildingBrick(buildQueues.get(BUILD_STEP.SYNTAX_NODE)) {			
			@Override
			public void build() {
				SStructure sStructure = null;
				if (instanceId != null) {
					SToken instance = (SToken) getGraph().getNode(instanceId);
					sStructure = getGraph().createStructure(instance);
					registerNode(id, sStructure);
				} else {
					sStructure = SaltFactory.createSStructure();
					registerNode(id, sStructure);
				}
			}
		};
	}
	
	/**
	 * With this method a syntax link can be registered and enqueued in the build process.
	 * @param id
	 * @param type
	 * @param sourceId
	 * @param targetId
	 */
	public void registerSyntaxLink(final String id, final String type, final String sourceId, final String targetId) {		
		new BuildingBrick(buildQueues.get(BUILD_STEP.SYNTAX_REL)) {
			@Override
			public void build() {
				SNode source = getNode(sourceId);				
				SNode target = getNode(targetId);
				getGraph().createRelation(source, target, SALT_TYPE.SDOMINANCE_RELATION, String.join("=", FUNC_NAME, type));
			}
		};
	}
	
	/**
	 * With this method a reference link can be registered and enqueued in the build process.
	 * @param id
	 * @param type
	 * @param sourceId
	 * @param targetId
	 */
	public void registerReferenceLink(final String id, final String type, final String sourceId, final String targetId) {
		new BuildingBrick(buildQueues.get(BUILD_STEP.REFERENCE_REL)) {			
			@Override
			public void build() {
				SNode source = getNode(sourceId);
				SNode target = getNode(targetId);
				getGraph().createRelation(source, target, SALT_TYPE.SPOINTING_RELATION, String.join("=", REF_TYPE_NAME, type)).setType(BRIDGING_RELATION);;				
			}
		};
	}
	
	/**
	 * This method stores created nodes to have them available for access in the later process.
	 * The node will always be added to the graph for safety reasons.
	 * @param id
	 * @param sNode
	 */
	protected void registerNode(String id, SNode sNode) {
		sNode.setGraph( getGraph() );
		graphNodes.put(id, sNode);
	}
	
	/**
	 *  Returns an {@link SNode} object (if that graph node has already been registered with the given nodeId).
	 * @param nodeId
	 * @return
	 */
	protected SNode getNode(String nodeId) {
		return graphNodes.get(nodeId);
	}
	
	/**
	 * This method sets a global evaluation map tokenId -> text value for all segmentations.
	 * @param token2text
	 */
	public void setGlobalEvaluationMap(final Map<String, String> token2text) {
		for (Entry<String, Segmentation> e : getSegmentations().entrySet()) {
			registerEvaluationMap(e.getKey(), token2text);
		}
	}
	
	/**
	 * This method allows to set a specific evaluation map for a single segmentation.
	 * @param speaker
	 * @param level
	 * @param evaluationMap
	 */
	public void registerEvaluationMap(String speaker, String level, Map<?, String> evaluationMap) {
		registerEvaluationMap(getQName(speaker, level), evaluationMap);
	}
	
	/**
	 * This method allows to set a specific evaluation map for a single segmentation.
	 * @param qName
	 * @param evaluationMap
	 */
	public void registerEvaluationMap(String qName, final Map<?, String> evaluationMap) {		
		getSegmentations().get(qName).setEvaluator(new Segmentation.Evaluator() {				
			@Override
			public String evaluate(String tokenId) {
				return evaluationMap.get(tokenId);
			}
		});
	}
	
	/**
	 *  This method is used to register new segmentations when their first token is added to the graph builder.
	 * @param segmentationName
	 * @param delimiter
	 */
	private void registerSegmentation(String segmentationName, String delimiter) {
		Segmentation seg = new Segmentation(segmentationName, delimiter);
		getSegmentations().put(segmentationName, seg);
	}
	
	/**
	 * This method allows to register a span over tokens.
	 * @param id
	 * @param tokenIds
	 * @return the span id (usable for later annotation and processing)
	 */
	public String registerSpan(String id, List<String> tokenIds) {
		final String spanId = idProvider.validate(id);
		final List<String> idList = new ArrayList<>(tokenIds);
		new BuildingBrick(buildQueues.get(BUILD_STEP.FURTHER_SPANS)) {			
			@Override
			public void build() {
				List<SToken> tokens = new ArrayList<>();
				for (String tId : idList) {
					tokens.add( (SToken) getNode(tId) );
				}
				SSpan span = getGraph().createSpan(tokens);
				registerNode(spanId, span);
				addAnnotations(spanId);
			}
		};
		return spanId;
	}
	
	/**
	 * This method registers an utterance token. Utterance tokens are tokens, that overlap all the tokens belonging to the utterance.
	 * @param id
	 * @param tokenIds
	 * @param speaker
	 * @param level
	 * @return
	 */
	public String registerUtterance(String id, final List<String> tokenIds, final String speaker, final String level) {
		final String utteranceId = idProvider.validate(id);
		new BuildingBrick(buildQueues.get(BUILD_STEP.UTTERANCES)) {			
			@Override
			public void build() {
				Segmentation segmentation = getSegmentations().get( getQName(speaker, level) );
				int[] indices = segmentation.getIndices(utteranceId);
				SToken utteranceToken = segmentation.getSToken(utteranceId);
				registerNode(utteranceId, utteranceToken);
				STextualDS ds = segmentation.getDS( getGraph() );
				addTextualRelation(utteranceToken, ds, indices[0], indices[1]);
				{
					/* build time for these tokenizations separately */
					addTimelineRelation(utteranceToken, getStartEndTime( tokenIds.get(0) )[0], getStartEndTime( tokenIds.get(tokenIds.size() - 1) )[1]);
				}
			}
		};
		addSegment(getQName(speaker, level), utteranceId);
		return utteranceId;
	}
	
	/**
	 * Returns the start and end time of a token
	 * @param tokenId
	 * @return
	 */
	private int[] getStartEndTime(String tokenId) {
		SToken sTok = (SToken) getNode(tokenId);
		for (SRelation<?, ?> rel : sTok.getOutRelations()) {
			if (rel instanceof STimelineRelation) {				
				return new int[] {((STimelineRelation) rel).getStart(), ((STimelineRelation) rel).getEnd()};
			}
		}
		return null;
	}
	
	/**
	 * This method is used to register token objects.
	 * @param id
	 * @param speaker
	 * @param level
	 * @return
	 */
	public String registerToken(String id, String speaker, String level) {
		final String tokenId = idProvider.validate(id);		
		final String segName = getQName(speaker, level);
		new BuildingBrick(buildQueues.get(BUILD_STEP.TOKEN)) {			
			@Override
			public void build() {
				Segmentation segmentation = getSegmentations().get(segName);
				STextualDS ds = segmentation.getDS( getGraph() );
				int[] indices = segmentation.getIndices(tokenId);
				SToken sTok = segmentation.getSToken(tokenId);
				registerNode(tokenId, sTok);
				addTextualRelation(sTok, ds, indices[0], indices[1]);
			}
		};
		addSegment(segName, tokenId);
		return tokenId;
	}
	
	/**
	 * Register a segment to its segmentation.
	 * @param segName
	 * @param tokenId
	 */
	protected void addSegment(String segName, String tokenId) {
		tokenId2SegName.put(tokenId, segName);
		if (!getSegmentations().containsKey(segName)) {
			registerSegmentation(segName, " ");
		}
		getSegmentations().get(segName).addSegment(tokenId);
	}
	
	/**
	 * Return the segmentation a given tokenId belongs to.
	 * @param tokenId
	 * @return
	 */
	private String getSegmentationByTokenId(String tokenId) {
		return tokenId2SegName.get(tokenId);
	}
	
	/**
	 * Return the speaker of a given token Id.
	 * WARNING: This method sticks to a certain standard of naming and should be instead implemented more dynamically.
	 * @param tokenId
	 * @return
	 */
	private String getSpeakerByTokenId(String tokenId) {
		return getSegmentationByTokenId(tokenId).split("_")[0];
	}
	
	/**
	 * Returns all known segmentations.
	 * @return
	 */
	protected Map<String, Segmentation> getSegmentations() {
		return segmentations;
	}
	
	/**
	 * creates an {@link STextualRelation} between a token and its {@link STextualDS}.
	 * @param sToken
	 * @param ds
	 * @param startIndex
	 * @param endIndex
	 * @return
	 */
	protected STextualRelation addTextualRelation(SToken sToken, STextualDS ds, int startIndex, int endIndex) {
		STextualRelation rel = SaltFactory.createSTextualRelation();		
		rel.setStart(startIndex);
		rel.setEnd(endIndex);
		rel.setSource(sToken);
		rel.setTarget(ds);
		rel.setGraph( getGraph() );
		return rel;
	}
	
	/**
	 * Creates an {@link STimelineRelation} between a token and the graphs {@link STimeline}.
	 * @param sToken
	 * @param from
	 * @param to
	 * @return
	 */
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
	
	/**
	 * Returns the maximal number of tokens on a single level for one timestep.
	 * @param timestep
	 * @return
	 */
	private int getLength(Map<String, List<String>> timestep) {
		Set<Integer> lengths = new HashSet<>();
		for (Entry<String, List<String>> e : timestep.entrySet()) {
			lengths.add( e.getValue().size() );
		}
		return reduceProduct(lengths);
	}
	
	/**
	 * Build order relations for each segmentation.
	 */
	private void buildOrderRelations() {
		for (String name : segmentations.keySet()) {
			buildOrderRelations(name);
		}
	}
	
	/**
	 * Build order relations for a specific segmentation.
	 * @param segmentationName
	 */
	private void buildOrderRelations(String segmentationName) {
		List<String> sequence = getSegmentations().get(segmentationName).getSequence();
		for (int i = 1; i < sequence.size(); i++) {
			addOrderRelation(sequence.get(i - 1), sequence.get(i), segmentationName);
		}
	}
	
	/**
	 * Add order relation between two tokens.
	 * @param fromId
	 * @param toId
	 * @param name
	 */
	private void addOrderRelation(String fromId, String toId, String name) {
		SToken source = (SToken) getGraph().getNode(fromId);
		SToken target = (SToken) getGraph().getNode(toId);
		getGraph().createRelation(source, target, SALT_TYPE.SORDER_RELATION, null).setType(name);
	}
	
	/**
	 * Build a qName for a token layer.
	 * @param speaker
	 * @param level
	 * @return
	 */
	public String getQName(String speaker, String level) {
		return String.join("_", speaker, level);
	}
	
	/**
	 * Add all annotations of a node given by its id.
	 * @param nodeId
	 */
	private void addAnnotations(String nodeId) {
		if (getAnnotations().containsKey(nodeId)) {
			SNode node = getNode(nodeId);
			for (SAnnotation a : getAnnotations().get(nodeId)) {
				addAnnotation(node, a);
			}
			getAnnotations().remove(nodeId);
		}
	}
	
	/**
	 * Add annotation to target node.
	 * @param target
	 * @param annotation
	 */
	private void addAnnotation(SNode target, SAnnotation annotation) {
		if (target instanceof SToken) {
			getGraph().createSpan((SToken) target).addAnnotation(annotation);
		} else {
			target.addAnnotation(annotation);
		}
	}
	
	/**
	 * Add {@link SAnnotation} (key, value) to {@link SNode} object.
	 * @param target
	 * @param name
	 * @param value
	 */
	private void addAnnotation(SNode target, String name, String value) {
		SAnnotation annotation = SaltFactory.createSAnnotation();
		annotation.setName(name);
		annotation.setValue(value);
		addAnnotation(target, annotation);
	}
	
	/**
	 * Add all remaining annotations not added during the build process.
	 */
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

	/**
	 * Main build call. Executes all collected build steps.
	 * @param temporalSequence
	 */
	public void build(final List<Map<String, List<String>>> temporalSequence) {
		new BuildingBrick(buildQueues.get(BUILD_STEP.TIME)) {			
			@Override
			public void build() {
				buildTime(temporalSequence);
			}
		};
		for (BUILD_STEP step : STEP_QUEUE) {
			for (BuildingBrick brick : buildQueues.get(step)) {
				brick.build();
			}
		}
		buildOrderRelations();
		addRemainingAnnotations();
		List<String> nodes = new ArrayList<>();
		for (SNode node : getGraph().getNodes()) {
			nodes.add( node.getId() );
		}
//		System.out.println(String.join(SystemUtils.LINE_SEPARATOR, nodes));
	}
}