package cs224n.langmodel;

import java.util.ArrayList;
import java.util.List;

public class EMInterpolatedEmpiricalTriGramModel extends
    EMInterpolatedNGramModel {

  public EMInterpolatedEmpiricalTriGramModel() {
    super(createModelList(), new EmpiricalInterpolatedTriGramModel(), 3);
  }

  private static List<NGram> createModelList() {
    List<NGram> models = new ArrayList<NGram>();
    models.add(new SmoothNGramModel(1));
    models.add(new EmpiricalNGramModel(2));
    models.add(new EmpiricalNGramModel(3));
    return models;
  }

}
