package cs224n.langmodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs224n.util.Counter;

/**
 * A dummy language model -- uses empirical unigram counts, plus a single
 * ficticious count for unknown words. (That is, we pretend that there is a
 * single unknown word, and that we saw it just once during training.)
 * 
 * @author Dan Klein
 */
public class SmoothedBackOffNGramLanguageModel implements LanguageModel {

  private static final String START = "<S>";
  private static final String STOP = "</S>";
  private static final String UNKNOWN = "<UNK/>";

  private Set<String> lexicon;
  private Map<List<String>, Counter<String>> uniGram;
  private Map<List<String>, Counter<String>> biGram;
  private Map<List<String>, Counter<String>> triGram;
  private Map<List<String>, Counter<String>> smoothedTriGram;
  private Map<List<String>, Counter<String>> smoothedBiGram;
  private Map<List<String>, Counter<String>> smoothedUniGram;
  private Map<List<String>, Double> alphaTriGram;
  private Map<List<String>, Double> alphaBiGram;

  // -----------------------------------------------------------------------

  /**
   * Constructs a new, empty unigram language model.
   */
  public SmoothedBackOffNGramLanguageModel() {
    uniGram = new HashMap<List<String>, Counter<String>>();
    biGram = new HashMap<List<String>, Counter<String>>();
    triGram = new HashMap<List<String>, Counter<String>>();
    lexicon = new HashSet<String>();
  }

  /**
   * Constructs a unigram language model from a collection of sentences. A
   * special stop token is appended to each sentence, and then the frequencies
   * of all words (including the stop token) over the whole collection of
   * sentences are compiled.
   */
  public SmoothedBackOffNGramLanguageModel(Collection<List<String>> sentences) {
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
    for (List<String> sentence : sentences) {
      List<String> stoppedSentence = new ArrayList<String>(sentence);
      stoppedSentence.add(0, START);
      stoppedSentence.add(0, START);
      stoppedSentence.add(STOP);
      for (int i = 2; i < stoppedSentence.size(); i++) {
        lexicon.add(stoppedSentence.get(i));
        addNgramCount(stoppedSentence, i, 1, uniGram);
        addNgramCount(stoppedSentence, i, 2, biGram);
        addNgramCount(stoppedSentence, i, 3, triGram);
      }
    }
    smoothedUniGram = smooth(uniGram, 1);
    smoothedBiGram = smooth(biGram, 2);
    smoothedTriGram = smooth(triGram, 3);
    alphaBiGram = computeAlpha(smoothedBiGram, 2);
    alphaTriGram = computeAlpha(smoothedTriGram, 3);
  }
  
  private double getAlpha(List<String> prefix, int n) {
    if (n == 2) {
      return alphaBiGram.get(prefix);
    } else if (n == 3) {
      return alphaTriGram.get(prefix);
    }
    assert false;
    return -1;
  }

  private void addNgramCount(List<String> sentence, int index, int n,
      Map<List<String>, Counter<String>> ngram) {
    assert index + 1 >= n;
    List<String> prefix = getPrefix(sentence, index, n);
    if (!ngram.containsKey(prefix)) {
      ngram.put(prefix, new Counter<String>());
    }
    ngram.get(prefix).incrementCount(sentence.get(index), 1.0);
  }
  
  private Map<List<String>, Double> computeAlpha(Map<List<String>, Counter<String>> ngram, int n) {
    Map<List<String>, Double> alpha = new HashMap<List<String>, Double>();
    for (List<String> prefix : ngram.keySet()) {
      alpha.put(prefix, computeSingleAlpha(prefix, ngram, n));
    }
    return alpha;
  }
  
  private double computeSingleAlpha(List<String> prefix,
      Map<List<String>, Counter<String>> ngram, int n) {
    double sumProbabilityLowOrder = 0.0;
    for (String word : ngram.get(prefix).keySet()) {
      if (!word.equals(UNKNOWN)) {
        sumProbabilityLowOrder += getWordProbability(
            prefix.subList(1, n - 1), word, n - 1);
      }
    }
    sumProbabilityLowOrder = 1.0 - sumProbabilityLowOrder;
    return ngram.get(prefix).getCount(UNKNOWN)
        / sumProbabilityLowOrder;
  }

  private Map<List<String>, Counter<String>> smooth(
      Map<List<String>, Counter<String>> ngram, int n) {
    Counter<Integer> frequencyCount = new Counter<Integer>();
    int maxCount = 0;
    // Do we still need maxCount?
    for (List<String> prefix : ngram.keySet()) {
      maxCount = Math.max(maxCount,
          (int) ngram.get(prefix).getCount(ngram.get(prefix).argMax()));
    }
    for (List<String> prefix : ngram.keySet()) {
      for (String word : ngram.get(prefix).keySet()) {
        frequencyCount.incrementCount((int) ngram.get(prefix).getCount(word),
            1.0);
      }
    }

    // Maybe set this in a better way? Zipff distribution?
    int maxFrequencyForGT = 0;
    for (int i = 1; i <= maxCount; i++) {
      if (frequencyCount.getCount(i) == 0) {
        maxFrequencyForGT = i - 2;
        break;
      }
    }

    Map<List<String>, Counter<String>> smoothedNgram = new HashMap<List<String>, Counter<String>>();

    double normalizingFactor = 0.0;

    // Handle the case of missing nGrams
    double totalMissingNgramsGTCount = frequencyCount.getCount(1);
    normalizingFactor += totalMissingNgramsGTCount;
    int totalNgrams = 0;
    for (List<String> prefix : ngram.keySet()) {
      totalNgrams += ngram.get(prefix).size();
    }
    // Figure out if we have to do more stuff for missing prefixes.
    int totalMissingNgrams = (int) Math.pow(lexicon.size(), n) - totalNgrams;

    for (List<String> prefix : ngram.keySet()) {
      if (!smoothedNgram.containsKey(prefix)) {
        smoothedNgram.put(prefix, new Counter<String>());
      }
      Counter<String> prefixCounter = ngram.get(prefix);
      Counter<String> smoothedPrefixCounter = smoothedNgram.get(prefix);
      for (String word : prefixCounter.keySet()) {
        int wordFrequency = (int) prefixCounter.getCount(word);
        assert wordFrequency > 0;
        if (wordFrequency <= maxFrequencyForGT) {
          double wordGTCount = (wordFrequency + 1)
              * frequencyCount.getCount(wordFrequency + 1)
              / frequencyCount.getCount(wordFrequency);
          smoothedPrefixCounter.setCount(word, wordGTCount);
        } else {
          smoothedPrefixCounter.setCount(word, wordFrequency);
        }
        normalizingFactor += smoothedPrefixCounter.getCount(word);
      }
      int prefixMissingNgrams = lexicon.size() - prefixCounter.size();
      if (n != 1) {
        smoothedPrefixCounter.setCount(UNKNOWN, totalMissingNgramsGTCount
            * prefixMissingNgrams / totalMissingNgrams);
      } else {
        smoothedPrefixCounter.setCount(UNKNOWN, totalMissingNgramsGTCount);
      }
    }

    return smoothedNgram;
  }

  private List<String> getPrefix(List<String> sentence, int index, int n) {
    return sentence.subList(index - n + 1, index);
  }

  // -----------------------------------------------------------------------

  private double getWordProbability(List<String> prefix, String word) {
    return getWordProbability(prefix, word, 3);
  }

  private Map<List<String>, Counter<String>> getNGram(int n) {
    if (n == 1) {
      return smoothedUniGram;
    } else if (n == 2) {
      return smoothedBiGram;
    } else if (n == 3) {
      return smoothedTriGram;
    }
    assert false;
    return null;
  }

  private double getWordProbability(List<String> prefix, String word, int n) {
    Map<List<String>, Counter<String>> ngram = getNGram(n);
    if (!ngram.containsKey(prefix)) {
      // Missing prefix, back off.
      // TODO deal with n = 1.
      return getWordProbability(prefix.subList(1, n - 1), word, n - 1);

      // backoff TODO
      // Smooth all three of them. DONE
      // Generalize getWordProbabilty DONE
      // Whenever outputing this uniform crap, back off instead. DONE
      // To do that, we need to computa \alpha(y) DONE
      // And also, remember, we need to back off possibly twice. DONE
      // For the unigram case, remember that when we smooth it and
      // have to allow for the possibility of UNKNOWN. DONE
    }
    Counter<String> smoothedPrefixCounter = ngram.get(prefix);
    if (!smoothedPrefixCounter.keySet().contains(word)) {
      if (n == 1) {
        // UNKNOWN word.
        return ngram.get(prefix).getCount(UNKNOWN); // Deal with OOV word in a
                                                    // better way?
      }
      return getAlpha(prefix, n)
          * getWordProbability(prefix.subList(1, n - 1), word, n - 1);
    }
    double a = smoothedPrefixCounter.getCount(word);
    double b = smoothedPrefixCounter.totalCount();
    double p = a / b;
    return p;
  }

  /**
   * Returns the probability, according to the model, of the word specified by
   * the argument sentence and index. Smoothing is used, so that all words get
   * positive probability, even if they have not been seen before.
   */
  public double getWordProbability(List<String> sentence, int index) {
    return getWordProbability(getPrefix(sentence, index, 3),
        sentence.get(index));
  }

  /**
   * Returns the probability, according to the model, of the specified sentence.
   * This is the product of the probabilities of each word in the sentence
   * (including a final stop token).
   */
  public double getSentenceProbability(List<String> sentence) {
    List<String> stoppedSentence = new ArrayList<String>(sentence);
    stoppedSentence.add(STOP);
    stoppedSentence.add(0, START);
    stoppedSentence.add(0, START);
    double probability = 1.0;
    for (int index = 2; index < stoppedSentence.size(); index++) {
      // TODO Use log likelihood?
      probability *= getWordProbability(stoppedSentence, index);
    }
    return probability;
  }

  /**
   * checks if the probability distribution properly sums up to 1
   */
  public double checkModel() {
    int checked = 0;
    double sum = 0.0;
    for (List<String> prefix : smoothedTriGram.keySet()) {
      double sample = Math.random();
      // We expect to check ~20 distributions
      if (sample < 20.0 / smoothedTriGram.size()) {
        checked++;
        for (String word : lexicon) {
          sum += getWordProbability(prefix, word);
        }
      }
    }
    System.out.println("checked " + checked + " conditional probabilities");
    return sum / checked;
  }

  /**
   * Returns a random word sampled according to the model. A simple
   * "roulette-wheel" approach is used: first we generate a sample uniform on
   * [0, 1]; then we step through the vocabulary eating up probability mass
   * until we reach our sample.
   */
  public String generateWord(List<String> prefix) {
    double sample = Math.random();
    double sum = 0.0;
    // This might be simpler if instead of startWordCounter, we had a list
    // of all words. This leaves no room for unknown.
    for (String word : lexicon) {
      sum += getWordProbability(prefix, word);
      if (sum > sample) {
        return word;
      }
    }
    assert false;
    return STOP; // a little probability mass was reserved for unknowns
  }

  /**
   * Returns a random sentence sampled according to the model. We generate words
   * until the stop token is generated, and return the concatenation.
   */
  public List<String> generateSentence() {
    List<String> sentence = new ArrayList<String>();
    List<String> prefix = new ArrayList<String>();
    prefix.add(START);
    prefix.add(START);
    String word;
    do {
      word = generateWord(prefix);
      sentence.add(word);
      prefix.add(word);
      prefix.remove(0);
    } while (!word.equals(STOP));
    return sentence;
  }

}
