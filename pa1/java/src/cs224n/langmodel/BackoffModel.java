package cs224n.langmodel;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BackoffModel extends NGram {

  private NGram primaryModel;
  private NGram backoffModel;
  private Map<List<String>, Double> alpha;

  public BackoffModel(NGram primary, NGram backoff) {
    super(primary.getN());

    this.primaryModel = primary;
    this.backoffModel = backoff;
    alpha = new HashMap<List<String>, Double>();

    assert primary.getN() >= backoff.getN();
  }

  @Override
  public void train(Collection<List<String>> trainingSentences) {
    primaryModel.train(trainingSentences);
    backoffModel.train(trainingSentences);
    calculateAlpha();
  }

  private void calculateAlpha() {
    for (List<String> prefix : knownPrefixes()) {
      double calcedAlpha = computeSingleAlpha(prefix);
      alpha.put(prefix, calcedAlpha);
    }
  }

  private double computeSingleAlpha(List<String> prefix) {
    // Calculate sum of probabilities in the lower order
    double sumProbabilityPrimary = 0.0, sumProbabilitySecondary = 0.0;
    for (String word : knownWords(prefix)) {
      assert !word.equals(UNKNOWN);
      sumProbabilityPrimary += primaryModel.getWordProbability(prefix, word);
      sumProbabilitySecondary += backoffModel.getWordProbability(
          getBackoffModelPrefix(prefix), word);
    }
    return (1 - sumProbabilityPrimary) / (1 - sumProbabilitySecondary);
  }

  private List<String> getBackoffModelPrefix(List<String> prefix) {
    return prefix.subList(n - backoffModel.getN(), n - 1);
  }

  @Override
  protected double getWordProbability(List<String> prefix, String word) {
    if (!knownPrefixes().contains(prefix)) {
      // Must back off with implicit alpha = 1;
      return backoffModel.getWordProbability(getBackoffModelPrefix(prefix), word);
    }
    if (!knownWords(prefix).contains(word)) {
      // Backoff with alpha.
      return backoffModel.getWordProbability(getBackoffModelPrefix(prefix), word) * alpha.get(prefix);
    }
    return primaryModel.getWordProbability(prefix, word);
  }

  @Override
  protected Set<List<String>> knownPrefixes() {
    return primaryModel.knownPrefixes();
  }

  @Override
  protected Set<String> knownWords(List<String> prefix) {
    return primaryModel.knownWords(prefix);
  }

  @Override
  protected Set<String> lexicon() {
    return primaryModel.lexicon();
  }

}
