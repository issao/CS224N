package cs224n.wordaligner;

import java.util.List;

import cs224n.util.Counter;
import cs224n.util.SentencePair;

public class FixedDisplacementDistortionModel implements DistortionModel {

  private Counter<Integer> distortionModelParams;
  private Counter<Integer> trainingDistortionModelParams;

  @Override
  public void init(List<SentencePair> trainingPairs) {
  }

  @Override
  public void startIteration() {
  }

  public double getFixedProbability(int englishLength, int englishPosition,
      int frenchLength, int frenchPosition) {
    if (englishPosition == -1) {
      return 0.2;
    } else {
      return (6 - bucket(englishLength, englishPosition, frenchLength, frenchPosition)) * 0.8 / 21;
    }
  }
  
  @Override
  public double getProbability(int englishLength, int englishPosition,
      int frenchLength, int frenchPosition) {
    return getFixedProbability(englishLength, englishPosition, frenchLength, frenchPosition);
  }

  private int bucket(int englishLength, int englishPosition, int frenchLength,
      int frenchPosition) {
    if (englishPosition == -1) {
      // I'm null
      return -1;
    }
    assert englishPosition >= 0;
    int bucket = (int) Math.floor(Math.abs(englishPosition - frenchPosition
        * 1.0 * englishLength / frenchLength));
    return Math.min(bucket, 5);
  }

  @Override
  public void addFractionalCount(int englishLength, int englishPosition,
      int frenchLength, int frenchPosition, double count) { 
  }

  @Override
  public void finishIteration() {
  }

}
