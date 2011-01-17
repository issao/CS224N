package cs224n.langmodel;

public class ZipfChimeraInterpolatedTriGramModel extends InterpolatedTriGramModel {

  public ZipfChimeraInterpolatedTriGramModel() {
    super(new SmoothNGramModel(1), new BackoffZipfBiGramModel(),
        new BackoffZipfTriGramModel());
  }

}
