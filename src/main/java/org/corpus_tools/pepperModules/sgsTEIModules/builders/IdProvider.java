package org.corpus_tools.pepperModules.sgsTEIModules.builders;

import java.util.HashSet;
import java.util.Set;

public class IdProvider {
	private Set<String> ids;	
	private IdValidator validator;
	public IdProvider(IdValidator validator) {
		ids = new HashSet<>();
		this.validator = validator;
	}
	
	public String validate(String id) {
		return validator.validate(ids, id);
	}
}
