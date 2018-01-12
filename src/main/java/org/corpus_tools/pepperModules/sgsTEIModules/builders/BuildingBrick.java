package org.corpus_tools.pepperModules.sgsTEIModules.builders;

import java.util.Collection;

/**
 * This class represents a sub step in the graph building queue.
 * @author klotzmaz
 *
 */
public abstract class BuildingBrick {
	public abstract void build();
	public BuildingBrick(Collection<BuildingBrick> stack) {	stack.add(this); }
}
