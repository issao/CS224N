package cs224n.classify;

import cs224n.util.Counter;

/**
 * @author Dan Klein
 */
public interface ProbabilisticClassifier<F,L> extends Classifier<F,L> {
  Counter<L> getProbabilities(Datum<F> datum);
  Counter<L> getLogProbabilities(Datum<F> datum);
}
