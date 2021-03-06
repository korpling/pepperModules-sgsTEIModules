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
package org.corpus_tools.pepperModules.sgsTEIModules.lib;

public interface SgsTEIDictionary {
	/* Name spaces */
	public static final String NS_SO = "so";
	public static final String NS_XML = "xml";	
	/* Tags */
	public static final String TAG_ADD = "add";
	public static final String TAG_CHOICE = "choice";
	public static final String TAG_CORR = "corr";
	public static final String TAG_DESC = "desc";
	public static final String TAG_F = "f";
	public static final String TAG_FS = "fs";
	public static final String TAG_INTERP = "interp";
	public static final String TAG_LINK = "link";
	public static final String TAG_LISTPERSON = "listPerson";
	public static final String TAG_NUMERIC = "numeric";
	public static final String TAG_PAUSE = "pause";
	public static final String TAG_PC = "pc";
	public static final String TAG_PERSON = "person";
	public static final String TAG_REG = "reg";
	public static final String TAG_SEG = "seg";
	public static final String TAG_SIC = "sic";
	public static final String TAG_SHIFT = "shift";
	public static final String TAG_SPAN = "span";
	public static final String TAG_SPANGRP = "spanGrp";
	public static final String TAG_STANDOFF = "standOff";
	public static final String TAG_STRING = "string";
	public static final String TAG_SYMBOL = "symbol";
	public static final String TAG_TEI = "TEI";
	public static final String TAG_TEXT = "text";
	public static final String TAG_TIMELINE = "timeline";
	public static final String TAG_TITLE = "title";
	public static final String TAG_U = "u";
	public static final String TAG_VOCAL = "vocal";
	public static final String TAG_W = "w";
	public static final String TAG_WHEN = "when";
	/* Attributes */
	public static final String ATT_ABSOLUTE = "absolute";
	public static final String ATT_ANA = "ana";
	public static final String ATT_DURATION = "duration";
	public static final String ATT_END = "end";
	public static final String ATT_FEATURE = "feature";
	public static final String ATT_ID = "id";
	public static final String ATT_INST = "inst";
	public static final String ATT_LANG = "lang";
	public static final String ATT_NAME = "name";
	public static final String ATT_NEW = "new";
	public static final String ATT_START = "start";
	public static final String ATT_TARGET = "target";
	public static final String ATT_TRANS = "trans";
	public static final String ATT_TYPE = "type";
	public static final String ATT_VALUE = "value";
	public static final String ATT_WHO = "who";
	/* Attribute values */
	public static final String TYPE_MORPHOSYNTAX = "morphosyntax";
	public static final String TYPE_REFERENCE = "reference";
	public static final String TYPE_SYNTAX = "syntax";
	public static final String TYPE_WORDFORM = "wordForm";
	public static final String NEW_NORMAL = "normal";
	/* Namespace uris */
	public static final String URI_NS_XML = "http://www.tei-c.org/ns/1.0";
	public static final String URI_NS_SO = "http://standoff.proposal";
	/* features */	
	public static final String[] FEAT_NAMES = {"tempo",};
	public static final String[][] FEAT_VALUES = {{"normal", "slow", "fast"},};
	public static final int[] FEAT_DEFAULT_INDICES = {0,}; 	
}
