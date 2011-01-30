package cs224n.langmodel;

import java.util.ArrayList;
import java.util.List;

public class ZipfChimeraInterpolatedTriGramModel extends ConstantEMInterpolatedNGramModel {

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
