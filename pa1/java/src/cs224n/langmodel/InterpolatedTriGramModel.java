package cs224n.langmodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import cs224n.assignments.LanguageModelTester;

public class InterpolatedTriGramModel extends NGram implements TunableModel {

  private NGram modelOne;
  private NGram modelTwo;
  private NGram modelThree;
  
  private boolean modelTuned;
  private static final int kGridPoints = 10;
  
  private double alpha, beta, gamma;
  
  public InterpolatedTriGramModel(NGram modelOne, NGram modelTwo, NGram modelThree) {
    super(3);
    this.modelOne = modelOne;
    this.modelTwo = modelTwo;
    this.modelThree = modelThree;
    modelTuned = false;
  }

  @Override
  public void tune(Collection<List<String>> trainingSentences) {
    modelTuned = true;
    double bestAlpha = 0, bestBeta = 0, bestGamma = 1, bestPerplexity = Double.POSITIVE_INFINITY;
    for (int i = 0; i <= kGridPoints; i++) {
      for (int j = 0; j <= kGridPoints - i; j++) {
        alpha = ((double)i)/kGridPoints;
        beta = ((double)j/kGridPoints);
        gamma = 1.0 - alpha - beta;

        assert -1E-6 <= alpha && alpha <= 1 + 1E-6;
        assert -1E-6 <= beta && beta <= 1 + 1E-6;
        assert -1E-6 <= gamma && gamma <= 1 + 1E-6;
        double perplexity = LanguageModelTester.computePerplexity(this, trainingSentences);
        System.out.println(alpha + ", " + beta + ", "+ gamma +":" + perplexity);
        if (perplexity < bestPerplexity) {
           bestPerplexity = perplexity;
           bestAlpha = alpha;
           bestBeta = beta;
           bestGamma = gamma;
        }
        
      }
    }
    alpha = bestAlpha;
    beta = bestBeta;
    gamma = bestGamma;
    System.out.println("Tuned alpha, betta, gamma: " + alpha + ", " + beta + ", " + gamma + ": " + bestPerplexity);
  }
  
  @Override
  public void train(Collection<List<String>> trainingSentences) {
    modelOne.train(trainingSentences);
    modelTwo.train(trainingSentences);
    modelThree.train(trainingSentences);
    
    alpha = 0.1;
    beta = 0.5;
    gamma = 0.4;
    assert alpha + beta + gamma == 1;
  }
  

  @Override
  public double getWordProbability(List<String> prefix, String word) {
    return alpha * modelOne.getWordProbability(modelOne.chopPrefix(prefix), word) +
           beta * modelTwo.getWordProbability(modelTwo.chopPrefix(prefix), word) +     
           gamma * modelThree.getWordProbability(modelThree.chopPrefix(prefix), word);
  }

  @Override
  public double checkModel() {
    assert modelTuned == true;
    return (modelOne.checkModel() + modelTwo.checkModel() + modelThree.checkModel() + super.checkModel()) / 4; 
  }

  @Override
  protected Set<List<String>> knownPrefixes() {
    return modelThree.knownPrefixes();
  }

  @Override
  protected Set<String> knownWords(List<String> prefix) {
    return modelThree.knownWords(prefix);
  }

  @Override
  protected Set<String> lexicon() {
    return modelThree.lexicon();
  }

  @Override
  public List<Double> modelWeigths() {
    List<Double> weights = new ArrayList<Double>();
    weights.add(alpha);
    weights.add(beta);
    weights.add(gamma);
    return weights;
  }

}
