package cs224n.langmodel;

import java.util.Collection;
import java.util.List;

public interface TunableModel {
  public void tune(Collection<List<String>> trainingSentences);
  public List<Double> modelWeigths();

}
