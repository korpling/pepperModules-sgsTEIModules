package org.corpus_tools.pepperModules.sgsTEIModules.builders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;

import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleDataException;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SOrderRelation;
import org.corpus_tools.salt.common.SStructure;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STextualRelation;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * This class is given all the necessary information to then build the graph.
 * It stores all the several build steps given as {@link BUILD_STEP}s and {@link BuildingBrick}s
 * and executes them in proper order. 
 * @author klotzmaz
 *
 */
public class GraphBuilder {
	private static final Logger logger = LoggerFactory.getLogger(GraphBuilder.class);
	private static final String F_ERR_ID_USED = "ID already in use: %s.";
	private static final String FUNC_NAME = "func";
	private static final String REF_TYPE_NAME = "type";
	private static final String ERR_NO_TEMPORAL_INFO = "No temporal information available.";
	private final PepperMapper mapper;
	private final SDocumentGraph graph;
	/** maps speaker specific segmentation name to sequence of token ids */
	private Map<String, Segmentation> segmentations;
	/** maps node id (from file) to salt object */
	private Multimap<String, SNode> graphNodes;
	/** id provider */
	private IdProvider idProvider;
	/** maps token id to segmentation name for faster processing */
	private Map<String, String> tokenId2SegName;
	/** queue of building steps */
	private Map<BUILD_STEP, Collection<BuildingBrick>> buildQueues;
	/** step queue */
	private static final BUILD_STEP[] BUILD_QUEUE = new BUILD_STEP[] {
			BUILD_STEP.TOKEN,
			BUILD_STEP.TIME,
			BUILD_STEP.ORDER,
			BUILD_STEP.UTTERANCES,
			BUILD_STEP.SYNTAX_NODE, 
			BUILD_STEP.SYNTAX_REL,
			BUILD_STEP.REFERENCE_REFEX,
			BUILD_STEP.REFERENCE_DE, 
			BUILD_STEP.REFERENCE_REL, 
			BUILD_STEP.FURTHER_SPANS,			 
			BUILD_STEP.ANNOTATION
	};
	protected static final String BRIDGING_RELATION = "bridging";
	protected static final String F_WARN_NODE_DOES_NOT_EXIST = "Node %s does not exist, because it was either not mentioned, not built or registered with a wrong id.";
	private static final String ANNO_NAME_GIVEN = "given";
	/** steps */
	private enum BUILD_STEP {
		TOKEN, SYNTAX_NODE, SYNTAX_REL, REFERENCE_REFEX, REFERENCE_DE, REFERENCE_REL, FURTHER_SPANS, UTTERANCES, TIME, ANNOTATION, ORDER
	}
	/** the graph's timeline */
	private final STimeline tl;
	
	public GraphBuilder(PepperMapper pepperMapper) {
		this.mapper = pepperMapper;
		this.graph = mapper.getDocument().getDocumentGraph();
		this.segmentations = new HashMap<>();
		this.graphNodes = HashMultimap.<String, SNode>create();
		this.buildQueues = new HashMap<>();
		for (BUILD_STEP step : BUILD_QUEUE) {
			buildQueues.put(step, new ArrayList<BuildingBrick>());
		}
		new BuildingBrick(buildQueues.get(BUILD_STEP.ORDER)) {			
			@Override
			public void build() {
				buildOrderRelations();
			}
		};
		IdValidator validator = new IdValidator() {			
			@Override
			public String validate(Set<String> ids, String id) {
				if (id == null) {
					//request for new id
					do {
						id = generate();
					} while (ids.contains(id));
				} else {
					if (ids.contains(id)) {
						//this should never be the case
						String errorMessage = String.format(F_ERR_ID_USED, id);
						logger.error(errorMessage, new PepperModuleException(mapper, errorMessage));
					}
				}
				ids.add(id);
				return id;
			}
			
			private String generate() {
				return Double.toHexString( Math.random() ).substring(4, 13) + Double.toHexString( Math.random() ).substring(4, 13);
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
				List<SToken> overlappedTokens = getGraph().getSortedTokenByText( getGraph().getOverlappedTokens( getNode(targetNodeId) ));
				overlappedTokens = getFullSequence(overlappedTokens.get(0), overlappedTokens.get(overlappedTokens.size() - 1));
				registerNode(id, getGraph().createSpan( overlappedTokens ));
			}
		};
	}
	
	private List<SToken> getFullSequence(SToken startToken, SToken endToken) {
		List<SToken> fullSequence = new ArrayList<>();
		SToken tok = startToken;
		Predicate<SRelation> p = new Predicate<SRelation>() {			
			@Override
			public boolean test(SRelation t) {
				return t instanceof SOrderRelation;
			}
		};
		while (tok != endToken) {
			fullSequence.add(tok);
			tok = (SToken) tok.getOutRelations().stream().filter(p).findFirst().get().getTarget();
		}
		fullSequence.add(endToken);
		return fullSequence;
	}
	
	/**
	 * With this method a discourse entity can be registered and enqueued in the build process.
	 * @param id
	 * @param instanceIds
	 */
	public void registerDiscourseEntity(final String id, final String[] instanceIds) {
		new BuildingBrick(buildQueues.get(BUILD_STEP.REFERENCE_DE)) {			
			@Override
			public void build() {
				/* current solution: first (mentioned) instance will be used by reflink, the others will be connected via p-rels*/
				for (int i = 0; i < instanceIds.length; i++) {
					if (i > 0) {
						addCorefRel(instanceIds[i], instanceIds[i - 1]); //FIXME NOTE: points backward to first mention, actually to most recent mention was preferred
						addDistanceAnnotation(instanceIds[i - 1], instanceIds[i]);
					}
					registerNode(id, getNode(instanceIds[i]) );					
				}
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
		getNode(mentionId).createAnnotation(null, ANNO_NAME_GIVEN, Integer.toString(val));		
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
	public void registerAnnotation(final String targetId, final String name, final String value) {
		new BuildingBrick(buildQueues.get(BUILD_STEP.ANNOTATION)) {		
			@Override
			public void build() {
				String annoName = name;
				Collection<SNode> targetNodes = getNodes(targetId);
				if (targetNodes != null) {
					for (SNode targetNode : targetNodes) {
						targetNode.createAnnotation(null, annoName, value);
					}
				} else {
					logger.warn(String.format(F_WARN_NODE_DOES_NOT_EXIST, targetId));
				}
			}
		};
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
				getGraph().createRelation(source, target, SALT_TYPE.SPOINTING_RELATION, String.join("=", REF_TYPE_NAME, type));//.setType(BRIDGING_RELATION);				
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
	protected Collection<SNode> getNodes(String nodeId) {
		return graphNodes.get(nodeId);
	}
	
	protected SNode getNode(String nodeId) {
		Collection<SNode> nodes = graphNodes.get(nodeId);
		return nodes == null? null : nodes.stream().findFirst().get();
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
				registerNode(spanId, getGraph().createSpan(tokens));
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
	 * Returns the maximal number of tokens on a single level for one timestep.
	 * @param timestep
	 * @return
	 */
	private int getLength(Map<String, List<String>> timestep) {
		Set<Integer> lengths = new HashSet<>();
		for (Entry<String, List<String>> e : timestep.entrySet()) {
			lengths.add( e.getValue().size() );
		}
		return lengths.stream().reduce(MULTIPLY).get();
	}	
	
	private static final BinaryOperator<Integer> MULTIPLY = new BinaryOperator<Integer>() {
		@Override
		public Integer apply(Integer t, Integer u) {
			return t * u;
		}
	};
	
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
		for (BUILD_STEP step : BUILD_QUEUE) {			
			for (BuildingBrick brick : buildQueues.get(step)) {
				brick.build();
			}
		}
	}
}