package org.corpus_tools.pepperModules.sgsTEIModules.builders;

import java.util.Collection;

public abstract class BuildingBrick {
	public abstract void build(Object... args);
	public abstract void immediate();
	public BuildingBrick(Collection<BuildingBrick> stack) {
		stack.add(this);
		immediate();
	}
}
