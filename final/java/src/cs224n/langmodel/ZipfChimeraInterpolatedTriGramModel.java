package cs224n.langmodel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ZipfChimeraInterpolatedTriGramModel extends ConstantEMInterpolatedNGramModel implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = -1790054240678057591L;

  public ZipfChimeraInterpolatedTriGramModel() {
    super(createModelList(), 3);
  }

  private static List<NGram> createModelList() {
    List<NGram> models = new ArrayList<NGram>();
    models.add(new SmoothNGramModel(1));
    models.add(new BackoffZipfBiGramModel());
    models.add(new BackoffZipfTriGramModel());
    return models;
  }

}
