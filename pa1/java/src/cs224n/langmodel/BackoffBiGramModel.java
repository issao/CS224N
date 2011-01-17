package cs224n.langmodel;


public class BackoffBiGramModel extends BackoffModel {

  public BackoffBiGramModel() {
    super(new SmoothNGramModel(2), new SmoothNGramModel(1));
    // TODO Auto-generated constructor stub
  }

}
