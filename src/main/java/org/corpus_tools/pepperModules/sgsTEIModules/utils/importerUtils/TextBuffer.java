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
