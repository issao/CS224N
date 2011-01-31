package cs224n.langmodel;

public class SmoothInterpolatedTriGramModel extends InterpolatedTriGramModel {

  public SmoothInterpolatedTriGramModel() {
    super(new SmoothNGramModel(1), new SmoothNGramModel(2),
        new SmoothNGramModel(3));
  }

}
