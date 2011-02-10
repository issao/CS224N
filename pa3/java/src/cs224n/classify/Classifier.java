package cs224n.classify;

/**
 */
public interface Classifier<F,L> {
  L getLabel(Datum<F> datum);
}
