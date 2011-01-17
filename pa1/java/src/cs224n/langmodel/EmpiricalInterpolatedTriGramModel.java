package cs224n.langmodel;

public class EmpiricalInterpolatedTriGramModel extends InterpolatedTriGramModel {

  public EmpiricalInterpolatedTriGramModel() {
    super(new SmoothNGramModel(1), new EmpiricalNGramModel(2),
        new EmpiricalNGramModel(3));
  }

}
