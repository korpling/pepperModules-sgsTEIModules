package org.corpus_tools.pepperModules.sgsTEIModules.builders.time;

public class TimeSlot {
	private int duration;
	protected TimeSlot(int duration) {
		this.duration = duration;
	}
	
	public int getDuration() {
		return duration;
	}
}
