package org.corpus_tools.pepperModules.sgsTEIModules;

import java.util.Collections;
import java.util.List;

public class TokenLike extends AbstractSequenceElement {
	private ElementType type;
	
	public TokenLike(ElementType type, String value) {
		super();
		this.type = type;
		setValue(value);
	}
	
	public TokenLike(ElementType type, String value, String id) {
		this(type, value);
		setId(id);
	}
	
	public ElementType getElementType() {
		return this.type;
	}

	@Override
	public List<SequenceElement> getElements() {
		return Collections.<SequenceElement>emptyList();
	}
}
