package edu.upf.taln.uima.clean.twitter_clean;
import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolSegmenter;

import de.tudarmstadt.ukp.dkpro.core.arktools.ArktweetTokenizer;
import de.tudarmstadt.ukp.dkpro.core.castransformation.ApplyChangesAnnotator;
import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiReader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;



import org.apache.uima.analysis_engine.AnalysisEngineDescription;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;

import org.apache.uima.fit.factory.AggregateBuilder;

import org.apache.uima.fit.pipeline.SimplePipeline;

import java.io.File;


import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;


public class TwitterCleaner 
{
   public static final String TARGET_VIEW = "TargetView";
   public static void main( String[] args )
    {
    	try {
	    File output = new File("src/test/resources");
        File inputFile = new File("src/test/resources/input2.txt");
        // inputFile = new File("src/test/resources/input.xmi");
	    // inputFile = new File("src/test/resources/input2.txt.xmi");
        File dumpFile = new File(output, "output2.txt");
        String pipelineFilePath = new File(output, "pipeline2.xml").getPath();

        CollectionReaderDescription reader;
			  reader = createReaderDescription(TextReader.class,
			        TextReader.PARAM_SOURCE_LOCATION, inputFile, TextReader.PARAM_LANGUAGE, "en");
			 // reader = createReaderDescription(XmiReader.class,
			 //       XmiReader.PARAM_SOURCE_LOCATION, inputFile, XmiReader.PARAM_LANGUAGE, "en");

        AnalysisEngineDescription tokClean = createEngineDescription(CleanTokens.class);

        AnalysisEngineDescription applyChanges = createEngineDescription(
                ApplyChangesAnnotator.class);
        
        // Token, Sentence. 
        AnalysisEngineDescription tokSent =createEngineDescription(ArktweetTokenizer.class);
		//AnalysisEngineDescription tokSent =createEngineDescription(WhitespaceTokenizer.class);
        //AnalysisEngineDescription tokSent =createEngineDescription(LanguageToolSegmenter.class);
        // Token, Sentence
        AnalysisEngineDescription tokSent2 =createEngineDescription(LanguageToolSegmenter.class);
        // Constituent, POS

        //xmi Writter
        AnalysisEngineDescription xmiWriter = createEngineDescription(XmiWriter.class,
                XmiWriter.PARAM_TARGET_LOCATION, output,
                XmiWriter.PARAM_OVERWRITE,true);


        AggregateBuilder builder = new AggregateBuilder();
        builder.add(tokSent);
        // tweet clean
        builder.add(tokClean); // Removing some lines to make sure to confuse the backmapper
        //apply changes from defeault sofa to target view
        builder.add(applyChanges, ApplyChangesAnnotator.VIEW_TARGET, TARGET_VIEW,
                ApplyChangesAnnotator.VIEW_SOURCE, CAS.NAME_DEFAULT_SOFA);
        builder.add(tokSent2, CAS.NAME_DEFAULT_SOFA, TARGET_VIEW); // Removing some lines to make sure to confuse the backmapper
        //builder.add(backMapper);
        //builder.add(xmiWriter, CAS.NAME_DEFAULT_SOFA, TARGET_VIEW);
        builder.add(xmiWriter, CAS.NAME_DEFAULT_SOFA, TARGET_VIEW);
        // builder.add(xmiWriter);
        AnalysisEngineDescription pipeline = builder.createAggregateDescription();

  
        SimplePipeline.runPipeline(reader, pipeline);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}       
    }
}
