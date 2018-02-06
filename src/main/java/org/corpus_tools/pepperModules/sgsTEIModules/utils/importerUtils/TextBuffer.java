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
package org.corpus_tools.pepperModules.sgsTEIModules.utils.importerUtils;

public class TextBuffer{
	private StringBuilder[] text;
	public TextBuffer() {
		text = new StringBuilder[] {
			new StringBuilder(),
			new StringBuilder()
		};
	}
	
	public String clear(int b) {
		String retVal = text[b].toString();
		text[b].delete(0, text[b].length());
		return retVal;
	}
	
	public void append(String text, int[] b) {
		for (int i : b) {
			this.text[i].append(text);
		}
	}
}
