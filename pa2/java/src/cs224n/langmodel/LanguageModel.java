package cs224n.langmodel;

import java.util.Collection;
import java.util.List;

/**
 * Language models assign probabilities to sentences and generate sentences.
 *
 * @author Dan Klein
 */
public interface LanguageModel {

  public void train(Collection<List<String>> trainingSentences);

  // This is actually not needed.
  // We forgot to remove it from the handout this year (2008).
  // But we will post on FAQ page shortly to announce this change.

  //public double getUnigramProbability(String word);

  public double getSentenceProbability(List<String> sentence);

  public double getWordProbability(List<String> sentence, int index);

  public List<String> generateSentence();

}
