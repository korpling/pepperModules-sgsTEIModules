package org.corpus_tools.pepperModules.sgsTEIModules.utils.importerUtils;

import java.util.ArrayList;
import java.util.List;

public class IdBuffer {
	private List<String> ids;
	protected IdBuffer() {
		ids = new ArrayList<>();
	}
	
	public List<String> clear() {
		List<String> retVal = new ArrayList<>(ids);
		ids.clear();
		return retVal;
	}
	
	public void append(String id) {
		ids.add(id);
	}
	
	public boolean isEmpty() {
		return ids.isEmpty();
	}
}
