/**
 * Copyright 2016 University of Cologne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */
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
