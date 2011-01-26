package cs224n.wordaligner;

import java.util.List;

import cs224n.util.SentencePair;

public interface DistortionModel {

  public abstract void init(List<SentencePair> trainingPairs);

  public abstract void startIteration();

  public abstract double getProbability(int englishLength, int englishPosition,
      int frenchLength, int frenchPosition);

  public abstract void addFractionalCount(int englishLength,
      int englishPosition, int frenchLength, int frenchPosition, double count);

  public abstract void finishIteration();

}