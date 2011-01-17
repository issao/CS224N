package cs224n.langmodel;

public class EmpiricalInterpolatedTriGramModel extends InterpolatedTriGramModel {

  public EmpiricalInterpolatedTriGramModel() {
    super(new SmoothNGramModel(1), new SmoothNGramModel(2),
        new SmoothNGramModel(3));
  }

}
