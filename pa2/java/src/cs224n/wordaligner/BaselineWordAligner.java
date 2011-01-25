package cs224n.wordaligner;  

import cs224n.util.*;
import java.util.List;

/**
   * Simple alignment baseline which maps french positions to english positions.
   * If the french sentence is longer, all final word map to null.
   */
  public class BaselineWordAligner extends WordAligner {

    private CounterMap<String,String> dummy;

    public Alignment alignSentencePair(SentencePair sentencePair) {
      Alignment alignment = new Alignment();
      int numFrenchWords = sentencePair.getFrenchWords().size();
      int numEnglishWords = sentencePair.getEnglishWords().size();
      for (int frenchPosition = 0; frenchPosition < numFrenchWords; frenchPosition++) {
        int englishPosition = frenchPosition;
        if (englishPosition >= numEnglishWords)
          englishPosition = -1;
        alignment.addAlignment(englishPosition, frenchPosition, true);
      }
      return alignment;
    }


    public double getAlignmentProb(List<String> targetSentence, List<String> sourceSentence, Alignment alignment) { return 0; }

    
    public CounterMap<String,String> getProbSourceGivenTarget(){ return dummy; }

    public void train(List<SentencePair> trainingPairs) {
      dummy = new CounterMap<String,String>();
      for(SentencePair pair : trainingPairs){
        List<String> targetWords = pair.getEnglishWords();
        List<String> sourceWords = pair.getFrenchWords();
        for(String source : sourceWords){
          for(String target : targetWords){
            dummy.setCount(source, target, 1.0);
          }
        }
      }
    }

  }
