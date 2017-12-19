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
	
	protected TextSegment createTextSegment(String id, String dipl, String norm, String pause) {
		return new TextSegment(id, dipl, norm, pause);
	}
	
	public class TextSegment{		
		private String id;
		private String dipl;
		private String norm;
		private String pause;
		
		protected TextSegment(String id, String dipl, String norm, String pause) {
			this.id = id;
			this.dipl = dipl;
			this.norm = norm;
			this.pause = pause;
		}
		
		public String getId() {
			return this.id;
		}
		
		public void setId(String id) {
			this.id = id;
		}
		
		public String getDipl() {
			return this.dipl;
		}
		
		public void setDipl(String dipl) {
			this.dipl = dipl;
		}
		
		public String getNorm() {
			return this.norm;
		}
		
		public void setNorm(String norm) {
			this.norm = norm;
		}
		
		public String getPause() {
			return this.pause;
		}
		
		public void setPause(String pause) {
			this.pause = pause;
		}
		
		/** For debug purposes*/
		@Override
		public String toString() {
			return String.join(":", getId(), getDipl(), getNorm(), getPause());
		}
	}
	
	protected class TextBuffer{
		private StringBuilder text;		
		protected TextBuffer() {
			text = new StringBuilder();
		}
		
		public String clear() {
			String retVal = text.toString();
			text.delete(0, text.length());
			return retVal;
		}
		
		public void append(String text) {
			this.text.append(text);
		}
	}
	
	public enum READ_MODE {
		TEXT, MORPHOSYNTAX, SYNTAX, REFERENCE, BLIND;		
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
	
	/** This class is a simple extension of a String-to-String {@link HashMap} to improve readability. */
	public class IdMapper extends HashMap<String, String> {}

	public IdMapper createIdMapper() {
		// TODO Auto-generated method stub
		return new IdMapper();
	}
	
	public IdMapper reversedMapper(IdMapper mapper) {
		IdMapper newMapper = createIdMapper();
		for (Entry<String, String> e : mapper.entrySet()) {
			newMapper.put(e.getValue(), e.getKey());
		}
		return newMapper;
	}
	

	
	protected static void debugMessage(Object... elements) {
		String[] elems = new String[elements.length];
		for (int i = 0; i < elements.length; i++) {
			elems[i] = elements[i] == null? "null" : elements[i].toString();
		}
		System.out.println(String.join(" ", elems));
	}
}
