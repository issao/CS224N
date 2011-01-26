package cs224n.wordaligner;

import java.util.List;

import cs224n.util.SentencePair;

public class ConstantDistortionModel implements DistortionModel  {
 
  @Override
  public void init(List<SentencePair> trainingPairs) {
    
  }
  
  @Override
  public void startIteration() {
    
  }
  
 
  @Override
  public double getProbability(int englishLength, int englishPosition, int frenchLength, int frenchPosition) {
    return 1.0 / (englishLength + 1);
  }

  @Override
  public void addFractionalCount(int englishLength, int englishPosition, int frenchLength, int frenchPosition,
       double count) {
    
  }

  @Override
  public void finishIteration() {
    
  }
}
