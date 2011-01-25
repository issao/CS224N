package cs224n.wordaligner;

import cs224n.util.*; 

import java.util.List;
import java.util.Set;

 /**
   * WordAligners have one method: alignSentencePair, which takes a sentence
   * pair and produces an alignment which specifies an english source for each
   * french word which is not aligned to "null".  Explicit alignment to
   * position -1 is equivalent to alignment to "null".
   */
  public abstract class WordAligner {

    public static final String NULL_WORD = "<NULL>";
    
    public abstract Alignment alignSentencePair(SentencePair sentencePair);
    
    /* Will return P(a,f|e) where f is sourceSentence, e is targetSentence */
    public abstract double getAlignmentProb(List<String> targetSentence, List<String> sourceSentence, Alignment alignment);
    
    public abstract  CounterMap<String,String> getProbSourceGivenTarget();
    
    public abstract void train(List<SentencePair> trainingPairs);

  }
