package org.corpus_tools.pepperModules.sgsTEIModules.builders;

public interface TokenDescriptor {
	public String getId();
	public int[] getTimes();
	public int[] getTextLimits();
	public String getValue();
}
