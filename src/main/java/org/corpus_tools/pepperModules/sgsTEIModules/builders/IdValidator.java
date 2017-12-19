package org.corpus_tools.pepperModules.sgsTEIModules.builders;

import java.util.Set;

public interface IdValidator {
	public String validate(Set<String> ids, String id);
}
