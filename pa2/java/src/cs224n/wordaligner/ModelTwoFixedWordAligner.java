package cs224n.wordaligner;

public class ModelTwoFixedWordAligner extends ModelNWordAligner {

  public ModelTwoFixedWordAligner() {
    super(new ModelOneWordAligner(), new FixedDisplacementDistortionModel());
  }

}
