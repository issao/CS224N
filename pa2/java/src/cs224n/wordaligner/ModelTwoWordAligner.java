package cs224n.wordaligner;

public class ModelTwoWordAligner extends ModelNWordAligner {

  public ModelTwoWordAligner() {
    super(new ModelOneWordAligner(), new DisplacementDistortionModel());
  }

}
