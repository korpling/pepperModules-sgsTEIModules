package org.corpus_tools.pepperModules.sgsTEIModules;

/**
 * PARENT is used for elements like u and seg, which can have children
 * NORM, DIPL, ALL assign a target level to token-like elements, such
 * as w, vocal, ...
 * @author klotzmaz
 *
 */
public enum ElementType {
	PARENT, NORM, DIPL, ALL
}
