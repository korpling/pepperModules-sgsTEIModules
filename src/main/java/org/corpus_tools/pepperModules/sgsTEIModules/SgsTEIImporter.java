package org.corpus_tools.pepperModules.sgsTEIModules;

import java.util.ArrayList;
import java.util.List;

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.common.PepperConfiguration;
import org.corpus_tools.pepper.impl.PepperImporterImpl;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.PepperImporter;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.PepperModule;
import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleNotReadyException;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SPointingRelation;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SStructure;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.graph.Identifier;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a dummy implementation of a {@link PepperImporter}, which can be used
 * as a template to create your own module from. The current implementation
 * creates a corpus-structure looking like this:
 * 
 * <pre>
 *       c1
 *    /      \
 *   c2      c3
 *  /  \    /  \
 * d1  d2  d3  d4
 * </pre>
 * 
 * For each document d1, d2, d3 and d4 the same document-structure is created.
 * The document-structure contains the following structure and annotations:
 * <ol>
 * <li>primary data</li>
 * <li>tokenization</li>
 * <li>part-of-speech annotation for tokenization</li>
 * <li>information structure annotation via spans</li>
 * <li>anaphoric relation via pointing relation</li>
 * <li>syntactic annotations</li>
 * </ol>
 * This dummy implementation is supposed to give you an impression, of how
 * Pepper works and how you can create your own implementation along that dummy.
 * It further shows some basics of creating a simple Salt model. <br/>
 * <strong>This code contains a lot of TODO's. Please have a look at them and
 * adapt the code for your needs </strong> At least, a list of not used but
 * helpful methods:
 * <ul>
 * <li>the salt model to fill can be accessed via {@link #getSaltProject()}</li>
 * <li>customization properties can be accessed via {@link #getProperties()}
 * </li>
 * <li>a place where resources of this bundle are, can be accessed via
 * {@link #getResources()}</li>
 * </ul>
 * If this is the first time, you are implementing a Pepper module, we strongly
 * recommend, to take a look into the 'Developer's Guide for Pepper modules',
 * you will find on
 * <a href="http://corpus-tools.org/pepper/">http://corpus-tools.org/pepper</a>.
 * 
 * @author Martin Klotz
 */
@Component(name = "sgsTEIImporterComponent", factory = "PepperImporterComponentFactory")
public class SgsTEIImporter extends PepperImporterImpl implements PepperImporter{
	/** this is a logger, for recording messages during program process, like debug messages**/
	private static final Logger logger = LoggerFactory.getLogger(SgsTEIImporter.class);

	/**
	 * <strong>OVERRIDE THIS METHOD FOR CUSTOMIZATION</strong> <br/>
	 * A constructor for your module. Set the coordinates, with which your
	 * module shall be registered. The coordinates (modules name, version and
	 * supported formats) are a kind of a fingerprint, which should make your
	 * module unique.
	 */
	public SgsTEIImporter() {
		super();
		setName("sgsTEIImporter");
		// TODO change suppliers e-mail address
		setSupplierContact(URI.createURI(PepperConfiguration.EMAIL));
		// TODO change suppliers homepage
		setSupplierHomepage(URI.createURI(PepperConfiguration.HOMEPAGE));
		//TODO add a description of what your module is supposed to do
		setDesc("This is a dummy importer and imports a static corpus containing one super-corpus, two sub-corpora and four documents. Each document contains a primary text, a tokenization, part-of-speech annotations,information structure annotations, syntactic annotations and anaphoric relations.");
		// TODO change "sample" with format name and 1.0 with format version to
		// support
		addSupportedFormat("sample", "1.0", null);
		// TODO change the endings in endings of files you want to import, see
		// also predefined endings beginning with 'ENDING_'
		getDocumentEndings().add("xml");
	}
	

	/**
	 * <strong>OVERRIDE THIS METHOD FOR CUSTOMIZATION</strong> <br/>
	 * This method is called by the pepper framework to import the
	 * corpus-structure for the passed {@link SCorpusGraph} object. In Pepper
	 * each import step gets an own {@link SCorpusGraph} to work on. This graph
	 * has to be filled with {@link SCorpus} and {@link SDocument} objects
	 * representing the corpus-structure of the corpus to be imported. <br/>
	 * In many cases, the corpus-structure can be retrieved from the
	 * file-structure of the source files. Therefore Pepper provides a default
	 * mechanism to map the file-structure to corpus-structure. This default
	 * mechanism can be configured. To adapt the default behavior to your needs,
	 * we recommend, to take a look into the 'Developer's Guide for Pepper
	 * modules', you will find on
	 * <a href="https://u.hu-berlin.de/saltnpepper/">https
	 * ://u.hu-berlin.de/saltnpepper/</a>. <br/>
	 * Just to show the creation of a corpus-structure for our sample purpose,
	 * we here create a simple corpus-structure manually. The simple contains a
	 * root-corpus <i>c1</i> having two sub-corpora <i>c2</i> and <i>c3</i>.
	 * Each sub-corpus contains two documents <i>d1</i> and <i>d2</i> for
	 * <i>d3</i> and <i>d4</i> and <i>c1</i> for <i>c3</i>.
	 * 
	 * <pre>
	 *       c1
	 *    /      \
	 *   c2      c3
	 *  /  \    /  \
	 * d1  d2  d3  d4
	 * </pre>
	 * 
	 * The URIs of the corpora and documents would be:
	 * <ul>
	 * <li>salt:/c1</li>
	 * <li>salt:/c1/c2</li>
	 * <li>salt:/c1/c2/d1</li>
	 * <li>salt:/c1/c2/d2</li>
	 * <li>salt:/c1/c3</li>
	 * <li>salt:/c1/c3/d3</li>
	 * <li>salt:/c1/c3/d4</li>
	 * </ul>
	 * 
	 * @param corpusGraph
	 *            the CorpusGraph object, which has to be filled.
	 */
	@Override
	public void importCorpusStructure(SCorpusGraph sCorpusGraph) throws PepperModuleException {
		
	}

	/**
	 * <strong>OVERRIDE THIS METHOD FOR CUSTOMIZATION</strong> <br/>
	 * This method creates a customized {@link PepperMapper} object and returns
	 * it. You can here do some additional initialisations. Thinks like setting
	 * the {@link Identifier} of the {@link SDocument} or {@link SCorpus} object
	 * and the {@link URI} resource is done by the framework (or more in detail
	 * in method {@link #start()}).<br/>
	 * The parameter <code>Identifier</code>, if a {@link PepperMapper} object
	 * should be created in case of the object to map is either an
	 * {@link SDocument} object or an {@link SCorpus} object of the mapper
	 * should be initialized differently. <br/>
	 * Just to show how the creation of such a mapper works, we here create a
	 * sample mapper of type {@link sgsTEIMapper}, which only produces a fixed
	 * document-structure in method {@link sgsTEIMapper#mapSDocument()} and
	 * enhances the corpora for further meta-annotations in the method
	 * {@link sgsTEIMapper#mapSCorpus()}. <br/>
	 * If your mapper needs to have set variables, this is the place to do it.
	 * 
	 * @param Identifier
	 *            {@link Identifier} of the {@link SCorpus} or {@link SDocument}
	 *            to be processed.
	 * @return {@link PepperMapper} object to do the mapping task for object
	 *         connected to given {@link Identifier}
	 */
	public PepperMapper createPepperMapper(Identifier Identifier) {
		SgsTEI2SaltMapper mapper = new SgsTEI2SaltMapper();
		/**
		 * TODO Set the exact resource, which should be processed by the created
		 * mapper object, if the default mechanism of importCorpusStructure()
		 * was used, the resource could be retrieved by
		 * getIdentifier2ResourceTable().get(Identifier), just uncomment this
		 * line
		 */
		// mapper.setResourceURI(getIdentifier2ResourceTable().get(Identifier));
		return (mapper);
	}
}
