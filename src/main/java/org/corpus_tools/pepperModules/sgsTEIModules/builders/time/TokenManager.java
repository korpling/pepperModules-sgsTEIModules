package org.corpus_tools.pepperModules.sgsTEIModules.builders.time;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;

public class TokenManager {	
	private static final String F_ERR_UNKNOWN_ID = "Unknown id: %s.";
	/* "regional" */
	private Map<String, int[]> tokenId2Indices;
	private Map<String, String> tokenId2tokenText;
	/* temporal */
	private List<Set<String>> sequence;
	private Map<String, Set<String>> tokenId2class;
	private Map<String, String> tokenId2SegmentationName;
	private Map<String, int[]> tokenId2timeslot;
	
	public TokenManager() {
		this.tokenId2Indices = new HashMap<>();
		this.sequence = new ArrayList<>();
		this.tokenId2SegmentationName = new HashMap<>();
		this.tokenId2class = new HashMap<>();
		this.tokenId2timeslot = new HashMap<>();
		this.tokenId2tokenText = new HashMap<>();
	}
	
	public void put(String id, String segmentationName, String alignWithId, String text) {	
		tokenId2SegmentationName.put(id, segmentationName);
		Set<String> eqClass;
		if (alignWithId == null) {
			sequence.add(new LinkedHashSet<String>());			
			eqClass = sequence.get(sequence.size() - 1); 
			eqClass.add(id);			
		} else {
			eqClass = tokenId2class.get(alignWithId);
			eqClass.add(id);
		}
		tokenId2class.put(id, eqClass);
		tokenId2tokenText.put(id, text);
	}
	
	public void putAfter(String id, String afterId, String segmentationName, String text) {//TODO unify
		tokenId2SegmentationName.put(id, segmentationName);
		Set<String> eqClass = new HashSet<>();
		eqClass.add(id);
		int ix = 0;
		while (!sequence.get(ix).contains(afterId) && ix < sequence.size()) {
			ix ++;
		}
		sequence.add(ix + 1, eqClass);
		tokenId2class.put(id, eqClass);	
		tokenId2tokenText.put(id, text);
	}
	
	public int[] getTimeslot(String tokenId) {
		if (!tokenId2timeslot.containsKey(tokenId)) {
			computeTimeslots();
		}
		return tokenId2timeslot.get(tokenId);
	}

	private void computeTimeslots() {
		int start = 0;
		for (int i = 0; i < sequence.size(); i++) {
			Set<String> tokenIds = sequence.get(i);
			int[] interval = new int[] {start, start + getTimeWidth(tokenIds)};
			for (String tokenId : tokenIds) {
				tokenId2timeslot.put(tokenId, interval);
			}
			start = interval[1];
		}
	}
	
	private int getTimeWidth(Set<String> tokenIds) {
		Set<String> levels = new HashSet<>();
		for (String id : tokenIds) {
			levels.add(tokenId2SegmentationName.get(id));
		}
		return 1 + tokenIds.size() - levels.size();
	}
	
	public int[] getIndices(String tokenId) {
		if (!tokenId2Indices.containsKey(tokenId)) {
			computeIndices();
		}
		return tokenId2Indices.get(tokenId);
	}

	private void computeIndices() {
		for (String name : tokenId2SegmentationName.values()) {
			int start = 0;
			for (String tokenId : getOrderedTokenIds(name)) {
				String text = tokenId2tokenText.get(tokenId);
				int[] indices = new int[] {start, start + text.length()};
				tokenId2Indices.put(tokenId, indices);
				start = indices[1];
			}
		}
	}
	
	public List<String> getOrderedTokenIds(String segmentationName) {
		List<String> tokens = new ArrayList<>();
		for (Set<String> tokenIds : sequence) {
			for (String id : tokenIds) {
				if (tokenId2SegmentationName.get(id).equals(segmentationName)) {
					tokens.add(id);
				}
			}
		}
		return tokens;
	}
	
	public String getText(String segmentationName) {
		List<String> tokens = new ArrayList<>();
		for (String id : getOrderedTokenIds(segmentationName)) {
			tokens.add(id);
		}
		return String.join(" ", tokens);
	}
	
	public Collection<String> getSegmentationNames() {
		Set<String> names = new HashSet<>(tokenId2SegmentationName.values());
		return names;
	}
	
	public String getSegementationName(String tokenId) {
		return tokenId2SegmentationName.get(tokenId);
	}
	
	public void setTokenText(String id, String text, boolean append) {
		if (!tokenId2tokenText.containsKey(id)) {
			throw new PepperModuleException(String.format(F_ERR_UNKNOWN_ID, id));
		}
		if (append) {
			String oldText = tokenId2tokenText.get(id);
			tokenId2tokenText.put(id, oldText == null? text : oldText.concat(text));
		} else {
			tokenId2tokenText.put(id, text);
		}
	}
	
	public boolean holdsToken(String id) {
		return tokenId2class.containsKey(id);
	}
	
	public String getSpeaker(String id) {
		return tokenId2SegmentationName.get(id).split("_")[0];
	}
}
