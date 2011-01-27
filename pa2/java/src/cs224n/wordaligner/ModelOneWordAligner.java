package cs224n.wordaligner;

public class ModelOneWordAligner extends ModelNWordAligner {

  public ModelOneWordAligner() {
    super(null, new ConstantDistortionModel());
  }
}
