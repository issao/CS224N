package cs224n.wordaligner;

import java.util.List;

import cs224n.util.Counter;
import cs224n.util.SentencePair;

public class DisplacementDistortionModel implements DistortionModel {

  private Counter<Integer> distortionModelParams;
  private Counter<Integer> trainingDistortionModelParams;

  @Override
  public void init(List<SentencePair> trainingPairs) {
    distortionModelParams = new Counter<Integer>();
    for (int i = -1; i <= 5; i++) {
      distortionModelParams.setCount(i, 1 / 7.0);
    }
  }

  @Override
  public void startIteration() {
    trainingDistortionModelParams = new Counter<Integer>();
  }

  @Override
  public double getProbability(int englishLength, int englishPosition,
      int frenchLength, int frenchPosition) {
    return distortionModelParams.getCount(
        bucket(englishLength, englishPosition, frenchLength, frenchPosition));
  }

  private int bucket(int englishLength, int englishPosition, int frenchLength,
      int frenchPosition) {
    if (englishPosition == -1) {
      // I'm null
      return -1;
    }
    int bucket = (int) Math.floor(Math.abs(englishPosition - frenchPosition
        * 1.0 * englishLength / frenchLength));
    return Math.min(bucket, 5);
  }

  @Override
  public void addFractionalCount(int englishLength, int englishPosition,
      int frenchLength, int frenchPosition, double count) {
    trainingDistortionModelParams.incrementCount(
        bucket(englishLength, englishPosition, frenchLength, frenchPosition),
        count);
  }

  @Override
  public void finishIteration() {
    trainingDistortionModelParams.normalize();
    distortionModelParams = trainingDistortionModelParams;
    trainingDistortionModelParams = null;
  }

}
