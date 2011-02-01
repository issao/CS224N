package cs224n.wordaligner;

import java.util.List;

import cs224n.util.SentencePair;

public class FixedDisplacementDistortionModel implements DistortionModel {

  private static final int MAX_BUCKET = 10;
  private static final double NULL_PROBABILITY = 0.2;

  @Override
  public void init(List<SentencePair> trainingPairs) {
  }

  @Override
  public void startIteration() {
  }

  public double getFixedProbability(int englishLength, int englishPosition,
      int frenchLength, int frenchPosition) {
    if (englishPosition == -1) {
      return NULL_PROBABILITY;
    } else {
      return (MAX_BUCKET + 1 - bucket(englishLength, englishPosition,
          frenchLength, frenchPosition))
          * (1 -  NULL_PROBABILITY)
          / ((MAX_BUCKET + 1) * (MAX_BUCKET + 2) / 2.0);
    }
  }

  @Override
  public double getProbability(int englishLength, int englishPosition,
      int frenchLength, int frenchPosition) {
    return getFixedProbability(englishLength, englishPosition, frenchLength,
        frenchPosition);
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
    return Math.min(bucket, MAX_BUCKET);
  }

  @Override
  public void addFractionalCount(int englishLength, int englishPosition,
      int frenchLength, int frenchPosition, double count) {
  }

  @Override
  public void finishIteration() {
  }

}
