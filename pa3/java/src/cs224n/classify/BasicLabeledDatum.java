package cs224n.classify;

import cs224n.classify.LabeledDatum;

import java.util.List;

/**
 * A minimal implementation of a labeled datum, wrapping a list of features and
 * a label.
 *
 * @author Dan Klein
 */
public class BasicLabeledDatum <F,L> implements LabeledDatum<F, L> {
  L label;
  List<F> features;

  public L getLabel() {
    return label;
  }

  public List<F> getFeatures() {
    return features;
  }

  public String toString() {
    return "<" + getLabel() + " : " + getFeatures().toString() + ">";
  }

  public BasicLabeledDatum(L label, List<F> features) {
    this.label = label;
    this.features = features;
  }
}
