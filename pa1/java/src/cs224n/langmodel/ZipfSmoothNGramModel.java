package cs224n.langmodel;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs224n.util.Counter;

public class ZipfSmoothNGramModel extends NGram {

  private static final int MIN_FREQUENCY_FOR_GT = 20;
  private static final String MISSING = "<MISSING/>";
  private EmpiricalNGramModel empiricalNGram;
  private Map<List<String>, Counter<String>> smoothCount;
  private double zipfA;
  private double zipfB;

  public ZipfSmoothNGramModel(int n) {
    super(n);
    empiricalNGram = new EmpiricalNGramModel(n);
    smoothCount = new HashMap<List<String>, Counter<String>>();
  }

  @Override
  public void train(Collection<List<String>> trainingSentences) {
    empiricalNGram.train(trainingSentences);
    computeSmoothCounts();
  }

  private void computeSmoothCounts() {
    smoothCount = new HashMap<List<String>, Counter<String>>();

    // Compute maxCount.
    int maxCount = 0;
    for (List<String> prefix : knownPrefixes()) {
      Counter<String> prefixCounter = empiricalNGram.getPrefixCounter(prefix);
      maxCount = Math.max(maxCount,
          (int) prefixCounter.getCount(prefixCounter.argMax()));
    }

    // Compute frequencyCount - do the double loop and compute it.
    Counter<Integer> frequencyCount = new Counter<Integer>();
    for (List<String> prefix : knownPrefixes()) {
      for (String word : knownWords(prefix)) {
        frequencyCount.incrementCount(empiricalNGram.getCount(prefix, word),
            1.0);
      }
    }

    // Compute the maxFrequencyForGT
    fitZipfCurve(frequencyCount, maxCount);
    int maxFrequencyForGT = 0;
    for (int i = 1; i <= maxCount; i++) {
      if (frequencyCount.getCount(i) <= MIN_FREQUENCY_FOR_GT) {
        maxFrequencyForGT = i - 2;
        break;
      }
    }

    // Compute totalNGrams
    int totalNgrams = 0;
    for (List<String> prefix : knownPrefixes()) {
      totalNgrams += knownWords(prefix).size();
    }
    // Compute missingNGrams
    // Each word can be any in the lexicon, and we allow the last word to be
    // UNKNOWN
    double totalMissingNgrams = Math.pow(lexicon().size(), n - 1)
        * (lexicon().size() + 1) - totalNgrams;
    assert totalMissingNgrams > 0.0;
    // Compute total count to be distributed to missingNGrams
    double totalMissingNgramsGTCount = frequencyCount.getCount(1);

    // Iterate over the ngrams to compute the smoothed counts.
    for (List<String> prefix : knownPrefixes()) {
      smoothCount.put(prefix, new Counter<String>());
      Counter<String> prefixCounter = empiricalNGram.getPrefixCounter(prefix);
      Counter<String> smoothedPrefixCounter = smoothCount.get(prefix);
      for (String word : prefixCounter.keySet()) {
        int wordFrequency = empiricalNGram.getCount(prefix, word);
        assert wordFrequency > 0;
        if (wordFrequency <= maxFrequencyForGT) {
          double wordGTCount = (wordFrequency + 1)
              * frequencyCount.getCount(wordFrequency + 1)
              / frequencyCount.getCount(wordFrequency);
          smoothedPrefixCounter.setCount(word, wordGTCount);
        } else {
          // Use Zipf curve.
          double wordGTCount = (wordFrequency + 1) * zipf(wordFrequency + 1)
              / zipf(wordFrequency);
          smoothedPrefixCounter.setCount(word, wordGTCount);
        }
      }
      int prefixMissingNgrams = lexicon().size() - knownWords(prefix).size()
          + 1; // +1 for UNKNOWN
      // TODO: Experiment with different normalizing factor.
      smoothedPrefixCounter.setCount(MISSING, totalMissingNgramsGTCount
          * prefixMissingNgrams / totalMissingNgrams);

      // Now change smoothed counts to conditional probabilities
      double totalCount = smoothedPrefixCounter.totalCount();
      for (String word : smoothedPrefixCounter.keySet()) {
        smoothedPrefixCounter.setCount(word,
            smoothedPrefixCounter.getCount(word) / totalCount);
      }
      assert Math.abs(1.0 - smoothedPrefixCounter.totalCount()) < 1e-6;
    }
  }

  private void fitZipfCurve(Counter<Integer> frequencyCount, int maxCount) {
    int count = 0;
    double sumX = 0.0, sumY = 0.0, sumXY = 0.0, sumXX = 0.0;
    for (int i = 1; i <= maxCount; i++) {
      // We ignore zeroes so that they don't pull down the fit, specially around
      // the tail which is full of large gaps.
      if (frequencyCount.getCount(i) == 0.0) {
        continue;
      }
      double y = Math.log(frequencyCount.getCount(i));
      double x = Math.log(i);
      sumX += x;
      sumY += y;
      sumXY += x * y;
      sumXX += x * x;
      count++;
    }

    zipfB = (sumXY - sumX * sumY / count) / (sumXX - sumX * sumX / count);
    zipfA = sumY / count - zipfB * sumX / count;
  }

  private double zipf(int i) {
    return Math.exp(zipfA) * Math.pow(i, zipfB);
  }

  @Override
  protected double getWordProbability(List<String> prefix, String word) {
    assert prefix.size() == n - 1;
    if (!knownPrefixes().contains(prefix)) {
      // Missing prefix, give uniform probability.
      return 1.0 / (lexicon().size() + 1);
    }
    Counter<String> smoothedPrefixCounter = smoothCount.get(prefix);
    if (!knownWords(prefix).contains(word)) {
      // NOTE We are dealing with UKNOWN by giving it equal weight as the
      // missing ngrams. An alternative method would be to check if the
      // word is in the lexicon to differentiate between missing ngram
      // and unknown word.
      // Once we add backoff, this does not matter except for the unigram case.
      int prefixMissingNgrams = lexicon().size() - knownWords(prefix).size()
          + 1; // +1 for UKNOWN
      return smoothedPrefixCounter.getCount(MISSING) / prefixMissingNgrams;
    }
    return smoothedPrefixCounter.getCount(word);
  }

  @Override
  protected Set<List<String>> knownPrefixes() {
    return empiricalNGram.knownPrefixes();
  }

  @Override
  protected Set<String> knownWords(List<String> prefix) {
    assert prefix.size() == n - 1;
    return empiricalNGram.knownWords(prefix);
  }

  @Override
  protected Set<String> lexicon() {
    return empiricalNGram.lexicon();
  }

}
