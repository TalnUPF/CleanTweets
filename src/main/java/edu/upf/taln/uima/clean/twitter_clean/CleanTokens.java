

package edu.upf.taln.uima.clean.twitter_clean;

import static org.apache.uima.fit.util.JCasUtil.select;
import com.vdurmont.emoji.EmojiParser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URL;
import java.net.MalformedURLException;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.transform.type.SofaChangeAnnotation;

/**
 * 
 * @author Joan Codina
 * Processes a tweet and removes/replaces some elements
 * it has a STATUS with several status.
 * Each token can produce different actions depending on the token and the status:
 * 	- Skip leave the token as it is
 * 	- change status
 *  - remove token
 *  - replace token
 *  - clean token
 *  tokens are classified by a regular expressions  in 
 *  - hashtags (to be processed and maybe replaced by a set of tokens) 
 *  - Users (depending on status are removed or modified (@ is removed)
 *  - URL (most cases are removed)
 *  - RT, start of retweet
 *  - EMOJIs (to be replaced)
 *  States:
 *  - initial state: tweet begin
 *  - rt State: the tweet starts with RT till ":" is found
 *  - text_state: regular text of the tweet
 *  - end_state: the tweet reaches the end... elements like "Via" or 
 */
public class CleanTokens
    extends JCasAnnotator_ImplBase
{
    
	public enum Status {
	    INITIAL,RT,TEXT,END
	};
	
	public enum TokType {
	    WORD,USER,HASHTAG,URL,RT,EMOJI,COLON,DOTS
	}
	
	public class TokInfo{
		public TokType tokType;
		public String replacement;
		public Token token;
		public TokInfo(TokType tokType, String replacement,Token token) {
			this.tokType = tokType;
			this.replacement = replacement;
			this.token=token;
		}
		
		
	}

    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        super.initialize(context);
    };

    @Override
    public void process(JCas aJCas)
        throws AnalysisEngineProcessException
    {
        List<Token> toRemove = new ArrayList<>();
        List<Token> tokens = new ArrayList<>(select(aJCas, Token.class));
        int size=tokens.size();
        Status status=Status.INITIAL;
        int rt=0;int elem=0;
        for (Token token : tokens) {
            if (++elem==size) status=Status.END; //last element;
        	TokInfo info= detectTokenType(token);
        	System.out.println(elem +"--" + size + "-" +status + "---" + info.token.getCoveredText() + " --- " + info.replacement +
        			" ----" + info.tokType );
        	switch (status) {
        	case INITIAL:
        		switch (info.tokType){
        		case WORD:
        			status=Status.TEXT;
        			break;
         		case COLON:
        		case USER:
        			remove(info,aJCas);
        			break;
        		case EMOJI:
        		case HASHTAG:
        			replace(info,aJCas);
       			    status=Status.TEXT;
        			break;
        		case DOTS:
        		case URL:
        			remove(info,aJCas);  
        			status=Status.TEXT;
        			break;
        		case RT:
        			remove(info,aJCas);
          			status=Status.RT; rt++;
        			break;
         		}
        		break;
        	case RT:
        		switch (info.tokType){
        		case WORD:
        			break;
        		case URL:
        		case DOTS:
        		case USER:
        			remove(info,aJCas);
        			break;
         		case EMOJI:
        		case HASHTAG:
        			replace(info,aJCas);
        			break;
         		case RT:
           			remove(info,aJCas);
          			rt++;
        			break;
         		case COLON:
           			remove(info,aJCas);
          			if (--rt==0) status=Status.TEXT;
        			break;
       		}
          	case TEXT:
        		switch (info.tokType){
        		case COLON:
        		case DOTS:
        		case WORD:
        			break;
        		case USER:
        		case EMOJI:
        		case HASHTAG:
        			replace(info,aJCas);
        			break;
        		case URL:
        		case RT:
        			remove(info,aJCas);
        			break;
        		}
          	case END:
        		switch (info.tokType){
        		case WORD:
        			break;
        		case DOTS:
         		case RT:
        		case USER:
       		    case URL:
         		case COLON:
         			remove(info,aJCas);
        			break;
      		    case EMOJI:
        		case HASHTAG:
        			replace(info,aJCas);
        			break;
        		}
        	}
        }

        for (Token token : toRemove) {
            token.removeFromIndexes(aJCas);
        }
    }

	private void replace(TokInfo info, JCas jCas) {
		// TODO Auto-generated method stub
	       SofaChangeAnnotation replace = new SofaChangeAnnotation(jCas);
	        replace.setOperation("replace");
	        replace.setBegin(info.token.getBegin());
	        replace.setEnd(info.token.getEnd());
	        replace.setValue(info.replacement);
	        replace.addToIndexes();
				
	}

	private void remove(TokInfo info, JCas jCas) {
		// TODO Auto-generated method stub
        SofaChangeAnnotation delete = new SofaChangeAnnotation(jCas);
        delete.setOperation("delete");
        delete.setBegin(info.token.getBegin());
        delete.setEnd(info.token.getEnd());
        delete.addToIndexes();
		
	}

	private TokInfo detectTokenType(Token token) {
		String text= token.getCoveredText();
		try{       	
		if (text.equalsIgnoreCase(":")) return new TokInfo(TokType.COLON,":",token);
		if (text.equalsIgnoreCase("RT")) return new TokInfo(TokType.RT,"RT",token);
		if (text.equalsIgnoreCase("...") || text.equalsIgnoreCase("…")) return new TokInfo(TokType.DOTS,"...",token);
		if (text.startsWith("@") && text.length()>1) return new TokInfo(TokType.USER,text.substring(1,2).toUpperCase()+text.substring(2),token);
		if (text.startsWith("#") && text.length()>1) { 
			String result= StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(text.substring(1)),' ');
			return new TokInfo(TokType.HASHTAG,result,token);
		}
		}catch (Exception e) {
	           System.out.println("--"+text+"--");
	           e.printStackTrace();
	           throw e;
	        } 
		try {
            new URL(text);
 			return new TokInfo(TokType.URL,text,token);
        } catch (MalformedURLException e) {
           // it is not an  url , so we continue
        }
		// emojis 
		String parsed=EmojiParser.parseToAliases(text);
		if (!parsed.equals(text)) {
			// then emojis have been replaced by :emoji:
			//now just replace ":" by " "
			parsed=parsed.replace(":", " ");
			return new TokInfo(TokType.EMOJI,parsed,token);
		}
		return new TokInfo(TokType.WORD,text,token);
	}
}
