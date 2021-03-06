//Dstl (c) Crown Copyright 2015
package uk.gov.dstl.baleen.annotators.misc;

import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import uk.gov.dstl.baleen.exceptions.BaleenException;
import uk.gov.dstl.baleen.types.semantic.Entity;
import uk.gov.dstl.baleen.uima.BaleenAnnotator;
import uk.gov.dstl.baleen.uima.utils.TypeUtils;

/**
 * Annotate the entire document as a single entity
 * 
 * 
 * @baleen.javadoc
 */
public class FullDocument extends BaleenAnnotator {
	private Class<? extends Entity> et = null;

	/**
	 * The type to annotate the whole document with
	 * 
	 * @baleen.config uk.gov.dstl.baleen.types.semantic.Entity
	 */
	public static final String PARAM_TYPE = "type";
	@ConfigurationParameter(name = PARAM_TYPE, defaultValue="uk.gov.dstl.baleen.types.semantic.Entity")
	private String type;
	
	@Override
	public void doInitialize(UimaContext aContext) throws ResourceInitializationException {
		try{
			et = TypeUtils.getEntityClass(type, JCasFactory.createJCas());
		}catch(UIMAException | BaleenException e){
			throw new ResourceInitializationException(e);
		}
	}
	
	@Override
	public void doProcess(JCas jCas) throws AnalysisEngineProcessException {
		String text = jCas.getDocumentText();

		if(text == null){
			getMonitor().info("Didn't annotate the document as it contained no content");
			return;
		}
				
		try {
			Entity ret = (Entity) et.getConstructor(JCas.class).newInstance(jCas);
			
			ret.setBegin(0);
			ret.setEnd(text.length());
			ret.setConfidence(1.0f);
				
			addToJCasIndex(ret);
			
			getMonitor().info("Annotated full document as {}", type);
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
	}
}
