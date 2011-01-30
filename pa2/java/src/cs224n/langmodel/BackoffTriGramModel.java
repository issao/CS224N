package cs224n.langmodel;


public class BackoffTriGramModel extends BackoffModel {

  public BackoffTriGramModel() {
    super(new SmoothNGramModel(3), new BackoffBiGramModel());
  }

}
