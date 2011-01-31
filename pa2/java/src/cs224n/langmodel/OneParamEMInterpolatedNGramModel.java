package cs224n.langmodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs224n.util.Counter;

public class OneParamEMInterpolatedNGramModel extends NGram implements
    TunableModel {

  private static final double MINIMUM_ENTROPY_STEP = 0.01;
  private static final int NUMBER_ITERATIONS = 10;
  private List<NGram> models;
  private NGram lastModel;
  private boolean modelTuned;
  private Map<String, List<Double>> weights;
  private EmpiricalNGramModel ngram;
  private NGram backupModel;

  public OneParamEMInterpolatedNGramModel(List<NGram> models,
      NGram backupModel, int n) {
    super(n);
    this.models = models;
    for (int i = 0; i < models.size(); i++) {
      assert models.get(i).getN() <= n;
    }
    lastModel = models.get(models.size() - 1);
    // The last NGram should be of size exactly n.
    assert lastModel.getN() == n;
    modelTuned = false;
    weights = new HashMap<String, List<Double>>();
    ngram = new EmpiricalNGramModel(n);
    this.backupModel = backupModel;
  }

  private String extractKey(List<String> prefix) {
    return prefix.get(prefix.size() - 1);
  }

  @Override
  public void tune(Collection<List<String>> trainingSentences) {
    modelTuned = true;
    ngram.train(trainingSentences);
    System.out.println("Tune backup model.");
    if (backupModel instanceof TunableModel) {
      ((TunableModel) backupModel).tune(trainingSentences);
    }
    System.out.println("Backup model tuned");
    
    // Initialize the weights.
    for (List<String> prefix : ngram.knownPrefixes()) {
      String currentKey = extractKey(prefix);
      if (weights.containsKey(currentKey)) {
        // Weight already computed for this key.
        continue;
      }
      System.out.println(currentKey);
      // Initialize weights.
      List<Double> weight = new ArrayList<Double>();
      weight.addAll(Collections.nCopies(models.size(), 1.0 / models.size()));
      weights.put(extractKey(prefix), weight);
    }

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
    Map<String, List<Counter<List<String>>>> fractionalCount = new HashMap<String, List<Counter<List<String>>>>();
    new ArrayList<Counter<List<String>>>();
    for (List<String> prefix : ngram.knownPrefixes()) {
      String currentKey = extractKey(prefix);
      if (fractionalCount.containsKey(currentKey)) {
        // Weight already computed for this key.
        continue;
      }
      fractionalCount.put(currentKey, new ArrayList<Counter<List<String>>>());
      for (int i = 0; i < models.size(); i++) {
        fractionalCount.get(currentKey).add(new Counter<List<String>>());
      }
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
                    * weights.get(extractKey(prefix)).get(i));
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
            fractionalCount.get(extractKey(prefix)).get(i).setCount(
                ng,
                ngram.getCount(prefix, word)
                    * reverseConditional.get(ng).get(i));
          }
        }
      }

      // M-step- Sum the above per model, divide by total word count.
      // that is the new P(Y).
      for (String key : weights.keySet()) {
        double total = 0.0;
        for (int i = 0; i < models.size(); i++) {
          total += fractionalCount.get(key).get(i).totalCount();
        }
        for (int i = 0; i < models.size(); i++) {
          weights.get(key).set(i, fractionalCount.get(key).get(i).totalCount() / total);
        }
        checkWeights(weights.get(key));
        System.out.println("key[" + key + "] "+ weights.get(key) + " entropy: " + entropy);
      }

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
    backupModel.train(trainingSentences);
  }

  @Override
  public double getWordProbability(List<String> prefix, String word) {
    assert modelTuned == true;
    if (!weights.containsKey(prefix)) {
      return backupModel.getWordProbability(prefix, word);
    }

    double probability = 0.0;
    for (int i = 0; i < models.size(); i++) {
      probability += models.get(i).getWordProbability(
          models.get(i).chopPrefix(prefix), word)
          * weights.get(prefix).get(i);
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
    sum += backupModel.checkModel();
    return sum / (models.size() + 2);
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
