package org.corpus_tools.pepperModules.sgsTEIModules.builders.time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.corpus_tools.salt.common.STimeline;

public class TimeBuilder {
	private STimeline timeline;
	private List<Set<String>> sequence;
	private Map<String, Set<String>> tokenId2class;
	private Map<String, String> tokenId2SegmentationName;
	
	public TimeBuilder(STimeline timeline) {
		this.timeline = timeline;
		this.sequence = new ArrayList<>();
		this.tokenId2SegmentationName = new HashMap<>();
		this.tokenId2class = new HashMap<>();
	}
	
	public void put(String id, String segmentationName, String alignWithId) {
		tokenId2SegmentationName.put(id, segmentationName);
		Set<String> eqClass;
		if (alignWithId == null) {
			sequence.add(new HashSet<String>());
			eqClass = sequence.get(sequence.size() - 1); 
			eqClass.add(id);
			tokenId2class.put(id, eqClass);			
		} else {
			eqClass = tokenId2class.get(alignWithId);
			eqClass.add(id);
		}
	}	
}
