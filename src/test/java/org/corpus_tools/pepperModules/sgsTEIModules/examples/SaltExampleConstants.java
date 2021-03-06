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
package org.corpus_tools.pepperModules.sgsTEIModules.examples;

public interface SaltExampleConstants {
	/* names */
	public static final String SPEAKER_JER = "JER";
	public static final String SPEAKER_S92 = "S92";
	public static final String DIPL = "dipl";
	public static final String NORM = "norm";
	public static final String PAUSE = "pause";
	public static final String SYN = "syn";
	public static final String UTT = "utterance";
	/* texts */
	public static final String DIPL_S92 = "ben , je viens à propos du , j' imagine que tu sais , du du cadavre qu' on a retrouvé au quatrième ètage .";
	public static final String NORM_S92 = "ben , je viens à propos du , j' imagine que tu sais , du du cadavre qu' on a retrouvé au quatrième étage .";
	public static final String PAUSE_S92 = "short";
	public static final String SYN_S92 = "ben je viens {à~propos~de} {le} j' imagine que tu sais {de} {le} {de} {le} cadavre qu' on a retrouvé {à} {le} quatrième étage";
	public static final String UTT_S92 = "u-92-d1e1251";
	public static final String DIPL_JER = "c' était quel quelqu'un qui s' inéressit mm beaucoup aux autes , ux gens en général .";
	public static final String NORM_JER = "c' était quelqu'un quelqu'un qui s' intéressait beaucoup aux autres , aux gens en général .";
	public static final String SYN_JER = "c' était quelqu'un quelqu'un qui s' intéressait beaucoup {à} {le} autres ∅ {à} {le} gens en général";
	public static final String UTT_JER = "u-92-d1e1992";
	//NOTE: there is no pause layer for JER. This way we can check whether the module is capable of dealing with the absence of pauses for one speaker properly.
	/* indices (start_index, end_index, start_time, end_time) */
	public static final int[][] DIPL_S92_INDICES = {
			{0, 3, 0, 1},//ben 
			{4, 5, 1, 2},//,
			{6, 8, 2, 3},//je 
			{9, 14, 3, 4},//viens 
			{15, 26, 4, 6},//à propos du 
			{27, 28, 6, 7},//, 
			{29, 31, 7, 8},//j' 
			{32, 39, 8, 9},//imagine 
			{40, 43, 9, 10},//que 
			{44, 46, 10, 11},//tu 
			{47, 51, 11, 12},//sais 
			{52, 53, 12, 13},//, 
			{54, 56, 13, 15},//du 
			{57, 59, 16, 18},//du 
			{60, 67, 18, 19},//cadavre 
			{68, 71, 19, 20},//qu' 
			{72, 74, 20, 21},//on 
			{75, 76, 21, 22},//a 
			{77, 85, 22, 23},//retrouvé 
			{86, 88, 23, 25},//au 
			{89, 98, 25, 26},//quatrième 
			{99, 104, 26, 27},//ètage 
			{105, 106, 27, 28}//.			
	};
	public static final int[][] NORM_S92_INDICES = DIPL_S92_INDICES;
	public static final int[][] PAUSE_S92_INDICES = {
			{0, 5, 15, 16}//short	
	};
	public static final int[][] SYN_S92_INDICES = {
			{0, 3, 0, 1},//ben
			{4, 6, 2, 3},//je
			{7, 12, 3, 4},//viens
			{13, 26, 4, 5},//{à~propos~de}
			{27, 31, 5, 6},//{le}
			{32, 34, 7, 8},//j'
			{35, 42, 8, 9},//imagine
			{43, 46, 9, 10},//que
			{47, 49, 10, 11},//tu
			{50, 54, 11, 12},//sais
			{55, 59, 13, 14},//{de}
			{60, 64, 14, 15},//{le}
			{65, 69, 16, 17},//{de}
			{70, 74, 17, 18},//{le}
			{75, 82, 18, 19},//cadavre
			{83, 86, 19, 20},//qu'
			{87, 89, 20, 21},//on
			{90, 91, 21, 22},//a
			{92, 100, 22, 23},//retrouvé
			{101, 104, 23, 24},//{à}
			{105, 109, 24, 25},//{le}
			{110, 119, 25, 26},//quatrième
			{120, 125, 26, 27}//étage	
	};
	public static final int[][] UTT_S92_INDICES = {
			{0, UTT_S92.length(), 0, 27}	
	};
	public static final int[][] DIPL_JER_INDICES = {
			{0, 2, 28, 29},//c'
			{3, 8, 29, 30},//était
			{9, 13, 30, 31},//quel
			{14, 23, 31, 32},//quelqu'un
			{24, 27, 32, 33},//qui
			{28, 30, 33, 34},//s'
			{31, 40, 34, 35},//inéressit
			{41, 43, 35, 36},//mm
			{44, 52, 36, 37},//beaucoup
			{53, 56, 37, 39},//aux
			{57, 62, 39, 40},//autes
			{63, 64, 41, 42},//,
			{65, 67, 42, 44},//ux
			{68, 72, 44, 45},//gens
			{73, 83, 45, 46},//en~général
			{84, 85, 46, 47}//.
	};
	public static final int[][] NORM_JER_INDICES = {
			{0, 2, 28, 29},//c'
			{3, 8, 29, 30},//était
			{9, 18, 30, 31},//quelqu'un
			{19, 28, 31, 32},//quelqu'un
			{29, 32, 32, 33},//qui
			{33, 35, 33, 34},//s'
			{36, 47, 34, 35},//intéressait
			{48, 56, 36, 37},//beaucoup
			{57, 60, 37, 39},//aux
			{61, 67, 39, 40},//autres
			{68, 69, 41, 42},//,
			{70, 73, 42, 44},//aux
			{74, 78, 44, 45},//gens
			{79, 89, 45, 46},//en~général
			{90, 91, 46, 47}//.
	};
	public static final int[][] SYN_JER_INDICES = {
			{0, 2, 28, 29},//c'
			{3, 8, 29, 30},//était
			{9, 18, 30, 31},//quelqu'un
			{19, 28, 31, 32},//quelqu'un
			{29, 32, 32, 33},//qui
			{33, 35, 33, 34},//s'
			{36, 47, 34, 35},//intéressait
			{48, 56, 36, 37},//beaucoup
			{57, 60, 37, 38},//{à}
			{61, 65, 38, 39},//{le}
			{66, 72, 39, 40},//autres
			{73, 74, 40, 41},//∅
			{75, 78, 42, 43},//{à}
			{79, 83, 43, 44},//{le}
			{84, 88, 44, 45},//gens
			{89, 99, 45, 46}//en~géneral			
	};
	public static final int[][] UTT_JER_INDICES = {
			{0, UTT_JER.length(), 28, 46}
	};
	/* morphological annotations */
	public static final String[] MORPH_NAMES = {"lemma", "pos"};
	public static final String[][] MORPH_S92 = {
			{"ben", "INT"},
			{"je", "PRO:cls"},
			{"venir", "VER:pres"},
			{"à~propos~de", "PRP"},
			{"le", "DET:def"},
			{"je", "PRO:cls"},
			{"imaginer", "VER:pres"},
			{"que", "KON"},
			{"tu", "PRO:cls"},
			{"savoir", "VER:pres"},
			{"de", "PRP"},
			{"le", "DET:def"},
			{"de", "PRP"},
			{"le", "DET:def"},
			{"cadavre", "NOM"},
			{"que", "PRO:rel"},
			{"on", "PRO:cls"},
			{"avoir", "AUX:pres"},
			{"retrouver", "VER:pper"},
			{"à", "PRP"},
			{"le", "DET:def"},
			{"quatrième", "ADJ"},
			{"étage", "NOM"}
	};
	public static final String[][] MORPH_JER = {
			{"ce", "PRO:cls"},
			{"être", "VER:impf"},
			{"quel", "DET:int"},
			{"quelqu'un", "PRO:ind"},
			{"qui", "PRO:rel"},
			{"se", "PRO:clo"},
			{"s'intéresser", "VER:impf"},
			{"beaucoup", "ADV"},
			{"à", "PRP"},
			{"le", "DET:def"},
			{"autre", "QUA"},
			null,//∅ has no annotation
			{"à", "PRP"},
			{"le", "DET:def"},
			{"gens", "NOM"},
			{"en~général", "ADV"}
	};
	public static final String[] UTT_NAMES = {
		"who", "transliteration" 	
	};
	public static final String[][] UTT_ANNO_S92 = {
		{"S92", null}
	};
	public static final String[][] UTT_ANNO_JER = {
		{"JER", "latching"}	
	};
	/* span annotations */
	/** each span is given as pair (from, to) whereas <i>from</i> and <i>to</i> are anchors in the timeline*/
	public static final int[][][] SPANS = {
			{{0, 1}, {1, 4}, {4, 6}, {6, 23}, {23, 25}, {25, 35}, {35, 37},	{37, 47}}, //tempo			
	};
	public static final String[] SPAN_NAMES = {
		"tempo"	
	};
	public static final String[][] SPAN_VALUES = {
			{"slow", "normal", "slow", "normal", "slow", "normal", "slow", "normal"}
	};
	/* syntax */
	/** each entry in the array represents one terminal syntactical token 
	 * (start-time, end-time, post-order of dominating head)
	 * */
	public static final int[][] SPELLOUT_JER = {
			{2, 3, 3},//je				
			{3, 4, 5},//viens			
			{4, 5, 8},//{à~propos~de}	
			{5, 6, 11},//{le}			
			{18, 19, 13},//cadavre		
			{19, 20, 16},//qu'			
			{20, 21, 18},//on			
			{21, 22, 20},//a			
			{22, 23, 22},//retrouvé		
			{23, 24, 25},//{à}			
			{24, 25, 28},//{le}			
			{25, 26, 30},//quatrième	
			{26, 27, 32}//étage		
	};
	/** each entry in the array represents one terminal syntactical token 
	 * (start-time, end-time, post-order of dominating head)
	 * */
	public static final int[][] SPELLOUT_S92 = {
			{28, 29, 3},//c'			
			{29, 30, 5},//était		
			{31, 32, 8},//quelqu'un	
			{32, 33, 11},//qui			
			{33, 34, 13},//s'			
			{34, 35, 15},//intéressait	
			{36, 37, 17},//beaucoup		
			{37, 38, 20},//{à}			
			{38, 39, 22},//{le}			
			{39, 40, 24},//autres		
			{40, 41, 39},//∅			
			{42, 43, 29},//{à}			
			{43, 44, 32},//{le}			
			{44, 45, 34},//gens			
			{45, 46, 36}//en~géneral	
	};
	/** each SStructure is represented by its pre- and postorder */
	public static final int[][] TREE_JER = {
			{0, 39},
			{1, 38},
			{2, 3},
			{4, 5},
			{6, 37},
			{7, 8},
			{9, 36},
			{10, 11},
			{12, 13},
			{14, 35},
			{15, 16},
			{17, 18},
			{19, 20},
			{21, 22},
			{23, 34},
			{24, 25},
			{26, 33},
			{27, 28},
			{29, 30},
			{31, 32}
	};
	public static final String[] TREE_LABELS_JER = {
		":ROOT",
		"root:VP",
		"suj:PRN",
		"head:V",
		"mod:PP",
		"head:P",
		"obj:NP",
		"det:D",
		"head:N",
		"mod_rel:VP",
		"obj:PRN",
		"suj:PRN",
		"aux_tps:V",
		"head:V",
		"mod:PP",
		"head:P",
		"obj:NP",
		"det:D",
		"mod:A",
		"head:N"
	};
	public static final int[][] TREE_S92 = {
			{0, 45},
			{1, 44},
			{2, 3},
			{4, 5},
			{6, 43},
			{7, 8},
			{9, 42},
			{10, 11},
			{12, 13},
			{14, 15},
			{16, 17},
			{18, 41},
			{19, 20},
			{21, 22},
			{23, 24},
			{25, 40},
			{26, 39},
			{27, 38},
			{28, 29},
			{30, 37},
			{31, 32},
			{33, 34},
			{35, 36}
	};
	public static final String[] TREE_LABELS_S92 = {
		":ROOT",
		"root:VP",
		"suj:PRN",
		"head:V",
		"ats:PRNP",
		"head:PRN",
		"mod_rel:VP",
		"suj:PRN",
		"aff:PRN",
		"head:V",
		"mod:ADV",
		"p_obj:PP",
		"head:P",
		"det:D",
		"obj:Q",
		"coord:COORDP",
		"head:COORD",
		"dep_coord:PP",
		"head:P",
		"obj:NP",
		"det:D",
		"head:N",
		"mod:ADV"
	};
	public static final int[][][] TREES = {TREE_S92, TREE_JER};
	public static final String[][] TREE_LABELS = {TREE_LABELS_S92, TREE_LABELS_JER};
	public static final int[][][] SPELLOUT = {SPELLOUT_S92, SPELLOUT_JER};
	/* reference */
	/** each entry is a tuple of time coordinates */
	public static final int[][] REFEX_SPANS = {
			{5, 27}, //cadavre ... 4e étage
			{28, 29}, //c'
			{37, 46}, //{à} {le} autres, {à} {le} gens en général
			{20, 21} //on
	};
	/** Each entry is an array of indices of the REFEX_SPANS array */
	public static final int[][] DISCOURSE_ENTITIES = {
			{0, 1},
			{2},
			{3}
	};
	public static final String[] DISCOURSE_ANNO_NAMES = {
			"label", "animacy", "specificity", "naturalGender", "cardinality"
	};
	public static final String[][] DISCOURSE_ANNO_VALUES = {
			{"VICTIM", "human", "specific", "male", "1"},
			{"PEOPLE", "human", "nonspecific", null, null},
			{"WE", "human", "nonspecific", null, null}
	};
	public static final String[][] DISCOURSE_GIVEN = {
			{"1",}, null, null	
	};
	/** each entry is a tuple (source-index, target-index) where indices are indices in 
	 * the discourse entity array 
	 */
	public static final int[][] REF_LINKS = {
			{11, 34}	
	};
	public static final String[] REF_LINK_TYPES = {
			"subset"
	};
}