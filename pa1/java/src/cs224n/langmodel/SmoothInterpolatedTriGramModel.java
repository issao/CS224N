package cs224n.langmodel;

public class SmoothInterpolatedTriGramModel extends InterpolatedTriGramModel {

  public SmoothInterpolatedTriGramModel() {
    super(new SmoothNGramModel(1), new EmpiricalNGramModel(2),
        new EmpiricalNGramModel(3));
  }

}
