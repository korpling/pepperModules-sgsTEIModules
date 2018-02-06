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

import org.corpus_tools.pepperModules.sgsTEIModules.lib.SgsTEIDictionary;

public enum READ_MODE implements SgsTEIDictionary{
	TEXT, TRANSLITERATION, MORPHOSYNTAX, SYNTAX, REFERENCE, BLIND;		
	public static READ_MODE getMode(String standoffType) {
		if (TYPE_SYNTAX.equalsIgnoreCase(standoffType)) {
			return READ_MODE.SYNTAX;
		}
		else if (TYPE_MORPHOSYNTAX.equalsIgnoreCase(standoffType)) {
			return READ_MODE.MORPHOSYNTAX;
		}
		else if (TYPE_REFERENCE.equals(standoffType)) {
			return READ_MODE.REFERENCE;
		} 
		else if (TAG_TEXT.equalsIgnoreCase(standoffType)) {
			return READ_MODE.TEXT;
		} else {
			return READ_MODE.BLIND;
		}
	}
}
