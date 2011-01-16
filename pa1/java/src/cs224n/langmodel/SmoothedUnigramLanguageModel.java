package cs224n.langmodel;

import cs224n.util.Counter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A dummy language model -- uses empirical unigram counts, plus a single
 * ficticious count for unknown words. (That is, we pretend that there is a
 * single unknown word, and that we saw it just once during training.)
 * 
 * 
 * @author Yaron and Issao
 */
public class SmoothedUnigramLanguageModel implements LanguageModel {

  private static final String STOP = "</S>";
  private static final String UNKNOWN = "<UNK/>";

  private Counter<String> wordCounter;
  private ArrayList<Integer> frequencyCount;
  private int maxFrequencyForGT;
  private double normalizingFactor;

  // -----------------------------------------------------------------------

  /**
   * Constructs a new, empty unigram language model.
   */
  public SmoothedUnigramLanguageModel() {
    wordCounter = new Counter<String>();
    frequencyCount = new ArrayList<Integer>();
  }

  /**
   * Constructs a unigram language model from a collection of sentences. A
   * special stop token is appended to each sentence, and then the frequencies
   * of all words (including the stop token) over the whole collection of
   * sentences are compiled.
   */
  public SmoothedUnigramLanguageModel(Collection<List<String>> sentences) {
    this();
    train(sentences);
  }

  // -----------------------------------------------------------------------

  /**
   * Constructs a unigram language model from a collection of sentences. A
   * special stop token is appended to each sentence, and then the frequencies
   * of all words (including the stop token) over the whole collection of
   * sentences are compiled.
   */
  public void train(Collection<List<String>> sentences) {
    wordCounter = new Counter<String>();
    for (List<String> sentence : sentences) {
      for (String word : sentence) {
        wordCounter.incrementCount(word, 1.0);
      }
      wordCounter.incrementCount(STOP, 1.0);
    }
    
    smooth();
  }

  private void smooth() {
    // Initialize frequencyCount
    int maxCount = (int) wordCounter.getCount(wordCounter.argMax());
    for (int i = 0; i <= maxCount; i++) {
      frequencyCount.add(0);
    }

    // Count the number of words that occur k times.
    for (String word : wordCounter.keySet()) {
      frequencyCount.set((int) wordCounter.getCount(word),
          frequencyCount.get((int) wordCounter.getCount(word)) + 1);
    }

    // Find the first index that is 0.
    for (int i = 1; i <= maxCount; i++) {
      if (frequencyCount.get(i) == 0) {
        maxFrequencyForGT = i - 2;
        break;
      }
    }

    // Compute normalizingFactor
    // Initialize it to the weight of the unknown word.
    normalizingFactor = 1;
    double sum = 0;
    for (String word : wordCounter.keySet()) {
      sum += getWordProbability(word);
    }

    // remember to add the UNK. In this SmoothedUnigramLanguageModel
    // we assume there is only one UNK, so we add...
    sum += getWordProbability(UNKNOWN);

    normalizingFactor = sum;
  }

  // -----------------------------------------------------------------------

  private double getWordProbability(String word) {
    double count = wordCounter.getCount(word);
    if (count == 0) { // unknown word
      return frequencyCount.get(1) / normalizingFactor;
    } else if (count <= maxFrequencyForGT) {
      return (count + 1) * frequencyCount.get((int) count + 1)
          / frequencyCount.get((int) count) / normalizingFactor;
    } else {
      return count / normalizingFactor;
    }
  }

  /**
   * Returns the probability, according to the model, of the word specified by
   * the argument sentence and index. Smoothing is used, so that all words get
   * positive probability, even if they have not been seen before.
   */
  public double getWordProbability(List<String> sentence, int index) {
    String word = sentence.get(index);
    return getWordProbability(word);
  }

  /**
   * Returns the probability, according to the model, of the specified sentence.
   * This is the product of the probabilities of each word in the sentence
   * (including a final stop token).
   */
  public double getSentenceProbability(List<String> sentence) {
    List<String> stoppedSentence = new ArrayList<String>(sentence);
    stoppedSentence.add(STOP);
    double probability = 1.0;
    for (int index = 0; index < stoppedSentence.size(); index++) {
      probability *= getWordProbability(stoppedSentence, index);
    }
    return probability;
  }

  /**
   * checks if the probability distribution properly sums up to 1
   */
  public double checkModel() {
    double sum = 0.0;
    // since this is a unigram model,
    // the event space is everything in the vocabulary (including STOP)
    // and a UNK token

    // this loop goes through the vocabulary (which includes STOP)
    for (String word : wordCounter.keySet()) {
      sum += getWordProbability(word);
    }

    // remember to add the UNK. In this SmoothedUnigramLanguageModel
    // we assume there is only one UNK, so we add...
    sum += getWordProbability(UNKNOWN);

    return sum;
  }

  /**
   * Returns a random word sampled according to the model. A simple
   * "roulette-wheel" approach is used: first we generate a sample uniform on
   * [0, 1]; then we step through the vocabulary eating up probability mass
   * until we reach our sample.
   */
  public String generateWord() {
    double sample = Math.random();
    double sum = 0.0;
    for (String word : wordCounter.keySet()) {
      sum += getWordProbability(word);
      if (sum > sample) {
        return word;
      }
    }
    return "*UNKNOWN*"; // a little probability mass was reserved for unknowns
  }

  /**
   * Returns a random sentence sampled according to the model. We generate words
   * until the stop token is generated, and return the concatenation.
   */
  public List<String> generateSentence() {
    List<String> sentence = new ArrayList<String>();
    String word = generateWord();
    while (!word.equals(STOP)) {
      sentence.add(word);
      word = generateWord();
    }
    return sentence;
  }

}
