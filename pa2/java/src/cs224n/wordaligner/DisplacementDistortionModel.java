package cs224n.wordaligner;

import java.util.List;

import cs224n.util.Counter;
import cs224n.util.SentencePair;

public class DisplacementDistortionModel implements DistortionModel {

  private static final double NULL_PROBABILITY = 0.2;
  private Counter<Integer> distortionModelParams;
  private Counter<Integer> trainingDistortionModelParams;

  @Override
  public void init(List<SentencePair> trainingPairs) {
    distortionModelParams = new Counter<Integer>();
    for (int i = -1; i <= 5; i++) {
      if (i == -1) {
        distortionModelParams.setCount(i, NULL_PROBABILITY);
      } else {
        distortionModelParams.setCount(i, (6 - i) * (1 - NULL_PROBABILITY) / 21);
      }
    }
  }

  @Override
  public void startIteration() {
    trainingDistortionModelParams = new Counter<Integer>();
    System.out.println("Start of iteration: " + distortionModelParams);
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
    assert englishPosition >= 0;
    int bucket = (int) Math.floor(Math.abs(englishPosition - frenchPosition
        * 1.0 * englishLength / frenchLength));
    return Math.min(bucket, 5);
  }

  @Override
  public void addFractionalCount(int englishLength, int englishPosition,
      int frenchLength, int frenchPosition, double count) {
    if (englishPosition == -1) {
      // The NULL probability is fixed, so skip.
      return;
    }
    trainingDistortionModelParams.incrementCount(
        bucket(englishLength, englishPosition, frenchLength, frenchPosition),
        count);
  }

  @Override
  public void finishIteration() {
    trainingDistortionModelParams.normalize(1 - NULL_PROBABILITY);
    trainingDistortionModelParams.setCount(-1, NULL_PROBABILITY);
    distortionModelParams = trainingDistortionModelParams;
    trainingDistortionModelParams = null;
    System.out.println("End of iteration: " + distortionModelParams);
  }

}
