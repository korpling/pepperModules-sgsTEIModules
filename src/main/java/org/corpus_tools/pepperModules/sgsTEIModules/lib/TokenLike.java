package org.corpus_tools.pepperModules.sgsTEIModules.lib;

import java.util.Collections;
import java.util.List;

public class TokenLike extends AbstractSequenceElement {
	private ElementType type;
	
	public TokenLike(ElementType type, String value) {
		this.type = type;
	}
	
	public ElementType getElementType() {
		return this.type;
	}

	@Override
	public List<SequenceElement> getElements() {
		return Collections.<SequenceElement>emptyList();
	}
}
