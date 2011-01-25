package cs224n.decoder;

import cs224n.util.*;
import cs224n.wordaligner.*;
import cs224n.langmodel.*;

import java.util.Set;
import java.util.List;


/**
 * Abstract class for the implementation of Decoders that use
 * a language model and a word alignment model to generate
 * translations of sentences.
 *
 * @author Sushant Prakash
 */

public abstract class Decoder {

  /* only the top N_MOST_LIKELY translations of a word will ever be considered. */
  public static final int N_MOST_LIKELY = 10;

  protected LanguageModel  langmodel;
  protected WordAligner wordaligner;
  protected WordAligner reverse_wordaligner;
  protected double lmWeight, transWeight, lengthWeight;
  protected CounterMap<String,String> mostLikelyTargetGivenSource;

  public Decoder(LanguageModel langmodel, WordAligner wordaligner, WordAligner reverse_wordaligner, double lmWeight, double transWeight, double lengthWeight){
    SetLanguageModel(langmodel);
    SetWordAlignmentModel(wordaligner);
    SetReverseWordAlignmentModel(reverse_wordaligner);
    mostLikelyTargetGivenSource = GetMostLikely();
    this.lmWeight = lmWeight;
    this.transWeight = transWeight;
    this.lengthWeight = lengthWeight;
  }

  public Decoder(LanguageModel langmodel, WordAligner wordaligner, WordAligner reverse_wordaligner){
    this(langmodel, wordaligner, reverse_wordaligner, 2.0, 1.0, 1.1);
  }

  public void SetLanguageModel(LanguageModel lm){
    langmodel = lm;
  }

  public void SetWordAlignmentModel(WordAligner wa){
    wordaligner = wa;
  }

  public void SetReverseWordAlignmentModel(WordAligner wa){
    reverse_wordaligner = wa;
  }

  
  /*
   * For each source word s, this method goes through all target words
   * t, keeping the top N_MOST_LIKELY with values p(s|t).
   */
  private CounterMap<String,String> GetMostLikely(){
    
    CounterMap<String,String> probSourceGivenTarget = wordaligner.getProbSourceGivenTarget();
    Set<String> sourceWords = probSourceGivenTarget.keySet();
    
    CounterMap<String,String> mostLikelyTargetGivenSource = new CounterMap<String,String>();

    for(String sourceWord : sourceWords){
      PriorityQueue<String> mostLikely = new PriorityQueue<String>();
      Counter<String> probGivenTargetWords = probSourceGivenTarget.getCounter(sourceWord);
      Set<String> targetWords = probGivenTargetWords.keySet();

      for(String targetWord : targetWords){
        mostLikely.add(targetWord, probGivenTargetWords.getCount(targetWord));
      }
      
      for(int i =0; i < N_MOST_LIKELY && mostLikely.hasNext(); i++){
        double prob = mostLikely.getPriority();
        String targetWord = mostLikely.next();
        mostLikelyTargetGivenSource.setCount(sourceWord, targetWord, prob);
      }

    }

      return mostLikelyTargetGivenSource;
  }
  
  /*
   * This method must be implemented but children classes.
   */
  public abstract List<String> Decode(List<String> sourceSentence);
  
}
