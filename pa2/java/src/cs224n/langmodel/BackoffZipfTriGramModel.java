package cs224n.langmodel;


public class BackoffZipfTriGramModel extends BackoffModel {

  public BackoffZipfTriGramModel() {
    super(new ZipfSmoothNGramModel(3), new BackoffZipfBiGramModel());
  }

}
