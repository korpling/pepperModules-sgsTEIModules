package org.corpus_tools.pepperModules.sgsTEIModules.builders.utils;

import java.util.function.Predicate;

public class InstanceOf<T> implements Predicate<T> {
	private Class<?> clazz;
	public InstanceOf(Class<?> clazz) {
		this.clazz = clazz;
	}
	
	@Override
	public boolean test(T t) {
		return clazz.isInstance(t);
	}
}
