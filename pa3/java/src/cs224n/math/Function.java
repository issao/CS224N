package cs224n.math;

/**
 */
public interface Function {
  int dimension();
  double valueAt(double[] x);
}
