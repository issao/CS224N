package cs224n.langmodel;

public class ZipfSmoothInterpolatedTriGramModel extends InterpolatedTriGramModel {

  public ZipfSmoothInterpolatedTriGramModel() {
    super(new ZipfSmoothNGramModel(1), new ZipfSmoothNGramModel(2),
        new ZipfSmoothNGramModel(3));
  }

}
