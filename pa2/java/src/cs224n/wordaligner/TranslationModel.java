package cs224n.wordaligner;

import java.util.List;

import cs224n.util.CounterMap;
import cs224n.util.SentencePair;

public class TranslationModel {
  private CounterMap<String, String> translationModelParams;
  private CounterMap<String, String> trainingTranslationModelParams;

  
  public void init(List<SentencePair> trainingPairs) {
    translationModelParams = new CounterMap<String, String>();
    
    // Initialize everything to uniform probability
    for (SentencePair pair : trainingPairs) {
      List<String> englishWords = pair.getEnglishWords();
      List<String> frenchWords = pair.getFrenchWords();
      for (String frenchWord : frenchWords) {
        for (String englishWord : englishWords) {
          translationModelParams.setCount(englishWord, frenchWord, 1);
        }
        translationModelParams.setCount(WordAligner.NULL_WORD, frenchWord, 1);
      }
    }
    translationModelParams.normalizeConditionalProbabilities();
  }

  public CounterMap<String, String> getTranslationModelParams() {
    return translationModelParams;
  }

  public void startIteration() {
    trainingTranslationModelParams = new CounterMap<String, String>();
  }
  
  public double getProbability(String englishWord, String frenchWord) {
    return translationModelParams.getCount(englishWord, frenchWord);
  }
  public void addFractionalCount(String englishWord, String frenchWord, double count) {
//    System.out.println(englishWord + "->"  + frenchWord + " " + count);
    trainingTranslationModelParams.incrementCount(englishWord, frenchWord, count);
  }
  
  public void finishIteration() {
//    System.out.println("Fractional count: " + trainingTranslationModelParams);
    trainingTranslationModelParams.normalizeConditionalProbabilities();
    translationModelParams = trainingTranslationModelParams;
    trainingTranslationModelParams = null;
  }
  
}
