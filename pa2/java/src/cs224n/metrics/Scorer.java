package cs224n.metrics;

/**
 * @author Pi-Chuan Chang
 */
public interface Scorer {
  void reset();

  void add(SegStats stats);

  void sub(SegStats stats);

  double score();
}
