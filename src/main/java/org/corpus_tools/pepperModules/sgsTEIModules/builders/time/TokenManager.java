package org.corpus_tools.pepperModules.sgsTEIModules.builders.time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class TokenManager {	
	/* "regional" */
	private Map<String, List<String>> segmentationName2tokenIds;
	private Map<String, int[]> tokenId2Indices;
	private Map<String, String> tokenId2tokenText;
	/* temporal */
	private List<Set<String>> sequence;
	private Map<String, Set<String>> tokenId2class;
	private Map<String, String> tokenId2SegmentationName;
	private Map<String, int[]> tokenId2timeslot;
	
	public TokenManager() {
		this.segmentationName2tokenIds = new HashMap<>();
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
			sequence.add(new HashSet<String>());
			eqClass = sequence.get(sequence.size() - 1); 
			eqClass.add(id);
			tokenId2class.put(id, eqClass);			
		} else {
			eqClass = tokenId2class.get(alignWithId);
			eqClass.add(id);
		}
		tokenId2tokenText.put(id, text);
		if (!segmentationName2tokenIds.containsKey(segmentationName)) {
			segmentationName2tokenIds.put(segmentationName, new ArrayList<String>());
		}
		segmentationName2tokenIds.get(segmentationName).add(id);
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
		List<String> tokenIdSequence = segmentationName2tokenIds.get(segmentationName); 
		tokenIdSequence.add(tokenIdSequence.indexOf(afterId) + 1, id);
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
		for (Entry<String, List<String>> e : segmentationName2tokenIds.entrySet()) {
			List<String> tokenIds = e.getValue();
			int start = 0;
			for (String tokenId : tokenIds) {
				String text = tokenId2tokenText.get(tokenId);
				int[] indices = new int[] {start, start + text.length()};
				tokenId2Indices.put(tokenId, indices);
				start += 1 + text.length();
			}
		}
	}
	
	public String getText(String segmentationName) {
		List<String> tokenIds = segmentationName2tokenIds.get(segmentationName);
		if (tokenIds == null) {
			return null;
		}
		String[] tokenTexts = new String[tokenIds.size()];
		for (int i = 0; i < tokenTexts.length; i++) {
			tokenTexts[i] = tokenId2tokenText.get( tokenIds.get(i) );
		}
		return String.join(" ", tokenTexts);
	}
	
	public Set<String> getSegmentationNames() {
		return segmentationName2tokenIds.keySet();
	}
	
	public String getSegementationName(String tokenId) {
		return tokenId2SegmentationName.get(tokenId);
	}
	
	public void setTokenText(String id, String text, boolean append) {
		if (append) {
			String oldText = tokenId2tokenText.get(id);
			tokenId2tokenText.put(id, oldText == null? text : oldText.concat(text));
		} else {
			tokenId2tokenText.put(id, text);
		}
	}
}
