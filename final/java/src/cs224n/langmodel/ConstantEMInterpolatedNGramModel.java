package cs224n.langmodel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs224n.util.Counter;

public class ConstantEMInterpolatedNGramModel extends NGram implements TunableModel, Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = -405863396361059062L;
  private static final double MINIMUM_ENTROPY_STEP = 0.01;
  private static final int NUMBER_ITERATIONS = 10;
  private List<NGram> models;
  private NGram lastModel;
  private boolean modelTuned;
  private List<Double> weight;
  private EmpiricalNGramModel ngram;
  
  public ConstantEMInterpolatedNGramModel(List<NGram> models, int n) {
    super(n);
    this.models = models;
    for (int i = 0; i < models.size(); i++) {
      assert models.get(i).getN() <= n;
    }
    lastModel = models.get(models.size() - 1);
    // The last NGram should be of size exactly n.
    assert lastModel.getN() == n;
    modelTuned = false;
    weight = new ArrayList<Double>();
    ngram = new EmpiricalNGramModel(n);
  }

  @Override
  public void tune(Collection<List<String>> trainingSentences) {
    modelTuned = true;
    ngram.train(trainingSentences);

    // Initialize weights.
    weight.addAll(Collections.nCopies(models.size(), 1.0 / models.size()));

    // Initialize intermediate probabilities;
    // P(ngram, Y)
    Map<List<String>, List<Double>> jointProbability = new HashMap<List<String>, List<Double>>();
    // P(Y | word)
    Map<List<String>, List<Double>> reverseConditional = new HashMap<List<String>, List<Double>>();
    for (List<String> prefix : ngram.knownPrefixes()) {
      Set<String> wordSet = ngram.knownWords(prefix);
      for (String word : wordSet) {
        List<String> ng = new ArrayList<String>(prefix);
        ng.add(word);
        jointProbability.put(ng, new ArrayList<Double>());
        jointProbability.get(ng)
            .addAll(Collections.nCopies(models.size(), 0.0));
        reverseConditional.put(ng, new ArrayList<Double>());
        reverseConditional.get(ng).addAll(
            Collections.nCopies(models.size(), 0.0));
      }
    }

    // Initialize the fractional counts;
    // E[N(word, Y)]
    List<Counter<List<String>>> fractionalCount = new ArrayList<Counter<List<String>>>();
    for (int i = 0; i < models.size(); i++) {
      fractionalCount.add(new Counter<List<String>>());
    }

    double previousEntropy = Double.NEGATIVE_INFINITY;
    for (int iteration = 0; iteration < NUMBER_ITERATIONS; iteration++) {
      // E-Step 1- Compute P(word, Y) = P(word|Y) * P(Y)
      for (List<String> prefix : ngram.knownPrefixes()) {
        Set<String> wordSet = ngram.knownWords(prefix);
        for (String word : wordSet) {
          List<String> ng = new ArrayList<String>(prefix);
          ng.add(word);
          for (int i = 0; i < models.size(); i++) {
            jointProbability.get(ng).set(
                i,
                models.get(i).getWordProbability(
                    models.get(i).chopPrefix(prefix), word)
                    * weight.get(i));
          }
        }
      }

      // E-step 2- Compute P(Y | word) by normalizing per word.
      // Compute entropy at the same time: H = SUM(Count(word) * P(word))
      double entropy = 0.0;
      for (List<String> prefix : ngram.knownPrefixes()) {
        Set<String> wordSet = ngram.knownWords(prefix);
        for (String word : wordSet) {
          List<String> ng = new ArrayList<String>(prefix);
          ng.add(word);
          double total = 0.0;
          for (int i = 0; i < models.size(); i++) {
            total += jointProbability.get(ng).get(i);
          }
          for (int i = 0; i < models.size(); i++) {
            reverseConditional.get(ng).set(i,
                jointProbability.get(ng).get(i) / total);
          }
          entropy += ngram.getCount(prefix, word) * Math.log(total);
        }
      }

      // E-step 3- Distribute observed count according to P(Y| word)
      for (List<String> prefix : ngram.knownPrefixes()) {
        Set<String> wordSet = ngram.knownWords(prefix);
        for (String word : wordSet) {
          List<String> ng = new ArrayList<String>(prefix);
          ng.add(word);
          for (int i = 0; i < models.size(); i++) {
            fractionalCount.get(i).setCount(
                ng,
                ngram.getCount(prefix, word)
                    * reverseConditional.get(ng).get(i));
          }
        }
      }

      // M-step- Sum the above per model, divide by total word count.
      // that is the new P(Y).
      double total = 0.0;
      for (int i = 0; i < models.size(); i++) {
        total += fractionalCount.get(i).totalCount();
      }
      for (int i = 0; i < models.size(); i++) {
        weight.set(i, fractionalCount.get(i).totalCount() / total);
      }

      checkWeights(weight);
      System.out.println(weight + " entropy: " + entropy);
      

      if (entropy - previousEntropy < MINIMUM_ENTROPY_STEP) {
        break;
      }
      previousEntropy = entropy;
    }

  }

  private void checkWeights(List<Double> weightList) {
    double sum = 0.0;
    for (Double d : weightList) {
      assert d > -1E-6;
      assert d < 1 + 1E-6;
      sum += d;
    }
    assert Math.abs(sum - 1.0) < 1E-6;
  }

  @Override
  public void train(Collection<List<String>> trainingSentences) {
    for (NGram model : models) {
      model.train(trainingSentences);
    }
  }

  @Override
  public double getWordProbability(List<String> prefix, String word) {
    assert modelTuned == true;
    
    double probability = 0.0;
    for (int i = 0; i < models.size(); i++) {
      probability += models.get(i).getWordProbability(
          models.get(i).chopPrefix(prefix), word)
          * weight.get(i);
    }
    return probability;
  }

  @Override
  public double checkModel() {
    assert modelTuned == true;
    double sum = 0.0;
    for (NGram model : models) {
      sum += model.checkModel();
    }
    sum += super.checkModel();
    return sum / (models.size() + 1);
  }

  @Override
  public List<Double> modelWeigths() {
    return new ArrayList<Double>();
  }

  @Override
  public Set<List<String>> knownPrefixes() {
    return lastModel.knownPrefixes();
  }

  @Override
  public Set<String> knownWords(List<String> prefix) {
    return lastModel.knownWords(prefix);
  }

  @Override
  protected Set<String> lexicon() {
    return lastModel.lexicon();
  }

}
