package org.corpus_tools.pepperModules.sgsTEIModules.builders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleDataException;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.pepperModules.sgsTEIModules.builders.time.TokenManager;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SDominanceRelation;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SStructure;
import org.corpus_tools.salt.common.SStructuredNode;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STextualRelation;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;

public class GraphBuilder {
	private static final String F_ERR_ID_USED = "ID already in use: %s.";
	private static final String F_ERR_NODE_NOT_REGISTERED = "Syntactic node is undefined: %s. This might be caused by multiple use of the id or an insufficient id validation mechanism.";
	private static final String FUNC_NAME = "func";
	private static final String REF_TYPE_NAME = "type";
	private static final String ERR_SPEAKER_IS_NULL = "Speaker must not be null.";
	private final PepperMapper mapper;
	private SDocumentGraph graph;
	/** maps speaker specific segmentation name to segmentation */
	private Map<String, Segmentation> segmentations;
	/** maps the file's analysis id to all annotations listed for this id */
	private Map<String, List<SAnnotation>> annotations;
	/** maps node id (from file) to salt object */
	private Map<String, SNode> graphNodes;
	/** maps relation id (from file) to salt object */
	private Map<String, SRelation<?, ?>> graphRelations;
	/** queue of todos to be done later, since objects occur later */
	private Stack<Finisher> unfinished;
	/** id provider */
	private IdProvider idProvider;
	/** time builder module */
	private TokenManager tokenManager;
	/** map segmentation name to textual ds */
	private Map<String, STextualDS> textualDSs;	
	
	public GraphBuilder(PepperMapper pepperMapper) {
		this.mapper = pepperMapper;
		this.graph = mapper.getDocument().getDocumentGraph();
		this.segmentations = new HashMap<>();
		this.annotations = new HashMap<>();
		this.graphNodes = new HashMap<>();
		this.graphRelations = new HashMap<>();
		this.unfinished = new Stack<Finisher>();
		IdValidator validator = new IdValidator() {			
			@Override
			public String validate(Set<String> ids, String id) {
				if (id == null) {
					//request for new id
					do {
						id = Double.toHexString( Math.random() ).substring(4, 13);
					} while (ids.contains(id));
				} else {
					//put id
					if (ids.contains(id)) {
						//this should never be the case
						throw new PepperModuleException(mapper, String.format(F_ERR_ID_USED, id));
					}
					ids.add(id);
				}
				return id;
			}
		};
		this.idProvider = new IdProvider(validator);
		this.tokenManager = new TokenManager();
		this.textualDSs = new HashMap<>();
		/* init */
		getGraph().createTimeline().increasePointOfTime(1000); //FIXME
	}
	
	public SDocumentGraph getGraph() {
		return graph;
	}
	
	public void registerReferringExpression(String id, String targetNodeId) {		
		final String spanId = id;
		final String targetId = targetNodeId;
		Finisher finisher = new Finisher() {				
			@Override
			public void build(Object... args) {
				List<SToken> overlappedTokens = getGraph().getSortedTokenByText( getGraph().getOverlappedTokens( getNode(targetId) ));
				SSpan span = getGraph().createSpan(overlappedTokens);
				registerNode(spanId, span);				
			}
		};
//		unfinished.push(finisher);
	}
	
	public void registerDiscourseEntity(String id, String targetNodeId, String annotationId) {		
		final String nodeId = targetNodeId;
		final String anaId = annotationId;
		Finisher finisher = new Finisher() {			
			@Override
			public void build(Object... args) {
				SSpan span = (SSpan) getNode(nodeId);
				for (SAnnotation anno : getAnnotions(anaId)) {
					span.addAnnotation(anno);
				}
				getGraph().addNode(span);
			}
		};
//		unfinished.push(finisher);
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
			public void build(Object... args) {
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
//		unfinished.add(finisher);
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
//		registerRelation(id, rel);
	}
	
	public void registerReferenceLink(String id, String type, String sourceId, String targetId) {
		final String typeValue = type;
		final String srcId = sourceId;
		final String tgtId = targetId;
		Finisher finisher = new Finisher() {			
			@Override
			public void build(Object... args) {
				SNode source = getNode(srcId);
				SNode target = getNode(tgtId);
				getGraph().createRelation(source, target, SALT_TYPE.SPOINTING_RELATION, String.join("=", REF_TYPE_NAME, typeValue));
			}
		};
//		unfinished.push(finisher);
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
	
	public void setTokenText(String speaker, String id, String text, boolean append) {
		tokenManager.setTokenText(id, text, append);
	}
	
	public void registerToken(final String speaker, final String segmentationName, String id, final String text, final String alignWithId) {
		if (speaker == null) {
			throw new PepperModuleException(ERR_SPEAKER_IS_NULL);
		}
		final String tokenId = idProvider.validate(id);				
		Finisher finisher = new Finisher() {			
			@Override
			public void build(Object... args) {
				int[] textInterval = getTokenLimits(tokenId);
				int[] timeInterval = getTokenTimes(tokenId);
				STextualDS ds = getTextualDS(tokenId);
				SToken sToken = SaltFactory.createSToken();
				sToken.setId(tokenId);
				getGraph().addNode(sToken);
				createTextualRelation(sToken, ds, textInterval[0], textInterval[1]);
				createTimelineRelation(sToken, timeInterval[0], timeInterval[1]);
				registerNode(tokenId, sToken);
			}
		};
		unfinished.push(finisher);
		getManager().put(tokenId, String.join("_", speaker, segmentationName), alignWithId, text);
		
	}
	
	protected TokenManager getManager() {
		return tokenManager;
	}

	public String registerTokenAfter(final String speaker, final String segmentationName, String id, final String afterTokenId, final String text) {
		if (speaker == null) {
			throw new PepperModuleException(ERR_SPEAKER_IS_NULL);
		}
		final String tokenId = idProvider.validate(id);
		Finisher finisher = new Finisher() {			
			@Override
			public void build(Object... args) { // FIXME unify, see registerToken (double code :( )
				int[] textInterval = getTokenLimits(tokenId);
				int[] timeInterval = getTokenTimes(tokenId);
				STextualDS ds = getTextualDS(tokenId);
				SToken sToken = SaltFactory.createSToken();
				sToken.setId(tokenId);
				getGraph().addNode(sToken);
				getGraph().addRelation( createTextualRelation(sToken, ds, textInterval[0], textInterval[1]) );
				getGraph().addRelation( createTimelineRelation(sToken, timeInterval[0], timeInterval[1]) );
				registerNode(tokenId, sToken);
			}
		};
		unfinished.push(finisher);
		getManager().putAfter(tokenId, afterTokenId, String.join("_", speaker, segmentationName), text);
		return tokenId;
	}
	
	protected STextualRelation createTextualRelation(SToken sToken, STextualDS ds, int startIndex, int endIndex) {
		STextualRelation rel = SaltFactory.createSTextualRelation();		
		rel.setStart(startIndex);
		rel.setEnd(endIndex);
		rel.setSource(sToken);
		rel.setTarget(ds);
		return rel;
	}
	
	protected STimelineRelation createTimelineRelation(SToken sToken, int from, int to) {
		STimelineRelation rel = SaltFactory.createSTimelineRelation();
		rel.setStart(from);
		rel.setEnd(to);
		rel.setSource(sToken);
		rel.setTarget( getGraph().getTimeline() );
		return rel;
	}
	
	protected int[] getTokenLimits(String tokenId) {
		return tokenManager.getIndices(tokenId);
	}
	
	protected int[] getTokenTimes(String tokenId) {
		return tokenManager.getTimeslot(tokenId);
	}	
	
	private STextualDS getTextualDS(String tokenId) {
		String segName = tokenManager.getSegementationName(tokenId);
		if (!textualDSs.containsKey(segName)) {
			textualDSs.put(segName, buildDS(segName));
		}
		return textualDSs.get(segName);
	}
	
	private STextualDS buildDS(String segmentationName) {
		STextualDS ds = getGraph().createTextualDS( tokenManager.getText(segmentationName) );
		ds.setName(segmentationName);
		return ds;
	}
	
	public boolean hasToken(String id) {
		return tokenManager.holdsToken(id);
	}

	public void build() {
		while (!unfinished.isEmpty()) {
			unfinished.pop().build();			
		}
		buildOrderRelations();
	}
	
	private void buildOrderRelations() {
		for (String name : tokenManager.getSegmentationNames()) {
			System.out.println(name);
			buildOrderRelations(name);
		}
	}

	private void buildOrderRelations(String name) {
		List<String> tokenIds = tokenManager.getOrderedTokenIds(name);
		for (int i = 1; i < tokenIds.size(); i++) {
			addOrderRelation(tokenIds.get(i - 1), tokenIds.get(i), name);
		}
	}

	private void addOrderRelation(String fromId, String toId, String name) {
		SToken source = (SToken) getGraph().getNode(fromId);
		SToken target = (SToken) getGraph().getNode(toId);
		getGraph().createRelation(source, target, SALT_TYPE.SORDER_RELATION, null).setType(name);
	}
}
