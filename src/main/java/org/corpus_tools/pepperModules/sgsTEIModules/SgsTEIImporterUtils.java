package org.corpus_tools.pepperModules.sgsTEIModules;

import java.util.HashMap;
import java.util.Map.Entry;

public class SgsTEIImporterUtils implements SgsTEIDictionary{
	
	private TextBuffer textBufferInstance;
	
	protected TextBuffer getTextBuffer() {
		if (textBufferInstance == null) {
			textBufferInstance = new TextBuffer();
		}
		return textBufferInstance;
	}
	
	protected class TextBuffer{
		private StringBuilder[] text;
		protected TextBuffer() {
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
	
	public enum READ_MODE {
		TEXT, TRANSLITERATION, MORPHOSYNTAX, SYNTAX, REFERENCE, BLIND;		
		protected static READ_MODE getMode(String standoffType) {
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
	
	protected static void message(Object... elements) {
		String[] elems = new String[elements.length];
		for (int i = 0; i < elements.length; i++) {
			elems[i] = elements[i] == null? "null" : elements[i].toString();
		}
		System.out.println(String.join(" ", elems));
	}
}
