package org.corpus_tools.pepperModules.sgsTEIModules.lib.importerLib;

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
