package cs224n.wordaligner;

import java.util.List;

import cs224n.util.Alignment;
import cs224n.util.Counter;
import cs224n.util.CounterMap;
import cs224n.util.SentencePair;

/**
 * Simple alignment baseline which maps french positions to english positions.
 * If the french sentence is longer, all final word map to null.
 */
public class NaiveWordAligner extends WordAligner {

  private Counter<String> frenchCounter;
  private Counter<String> englishCounter;
  private CounterMap<String, String> joint;
  private CounterMap<String, String> translationModel;

  public Alignment alignSentencePair(SentencePair sentencePair) {
    Alignment alignment = new Alignment();
    int numFrenchWords = sentencePair.getFrenchWords().size();
    for (int frenchPosition = 0; frenchPosition < numFrenchWords; frenchPosition++) {
      String frenchWord = sentencePair.getFrenchWords().get(frenchPosition);
      double bestProbability = 0;
      int bestIndex = -1;
      for (int englishPosition = 0; englishPosition < sentencePair
          .getEnglishWords().size(); englishPosition++) {
        String englishWord = sentencePair.getEnglishWords()
            .get(englishPosition);
        double probability = translationModel.getCount(englishWord, frenchWord);
        if (probability > bestProbability) {
          bestProbability = probability;
          bestIndex = englishPosition;
        }
      }
      alignment.addAlignment(bestIndex, frenchPosition, true);
    }
    return alignment;
  }

  public double getAlignmentProb(List<String> targetSentence,
      List<String> sourceSentence, Alignment alignment) {
    return 0;
  }

  public CounterMap<String, String> getProbSourceGivenTarget() {
    return translationModel;
  }

  public void train(List<SentencePair> trainingPairs) {
    joint = new CounterMap<String, String>();
    frenchCounter = new Counter<String>();
    englishCounter = new Counter<String>();

    for (SentencePair pair : trainingPairs) {
      List<String> englishWords = pair.getEnglishWords();
      List<String> frenchWords = pair.getFrenchWords();
      for (String frenchWord : frenchWords) {
        for (String englishWord : englishWords) {
          joint.incrementCount(englishWord, frenchWord, 1);
          frenchCounter.incrementCount(frenchWord, 1);
          englishCounter.incrementCount(englishWord, 1);
        }
      }
    }
    normalize(frenchCounter);
    normalize(englishCounter);
    normalize(joint);

    translationModel = new CounterMap<String, String>();
    for (String englishWord : joint.keySet()) {
      for (String frenchWord : joint.getCounter(englishWord).keySet()) {
        translationModel.setCount(
            englishWord,
            frenchWord,
            joint.getCount(englishWord, frenchWord)
                / frenchCounter.getCount(frenchWord)
                / englishCounter.getCount(englishWord));
      }
    }

    normalizeConditionalProbabilities(translationModel);
  }

  private void normalizeConditionalProbabilities(
      CounterMap<String, String> translationModel) {
    for (String frenchWord : translationModel.keySet()) {
      normalize(translationModel.getCounter(frenchWord));
    }
  }

  private void normalize(CounterMap<String, String> counterMap) {
    double totalCount = counterMap.totalCount();
    for (String key : counterMap.keySet()) {
      for (String value : counterMap.getCounter(key).keySet()) {
        counterMap.setCount(key, value, counterMap.getCount(key, value)
            / totalCount);
      }
    }
  }

  private void normalize(Counter<String> counter) {
    double totalCount = counter.totalCount();
    for (String key : counter.keySet()) {
      counter.setCount(key, counter.getCount(key) / totalCount);
    }
  }
}
