package cs224n.assignments;

import java.util.List;

public class TestResult {
  public String modelName;
  public boolean wasTuned;
  public String trainingPerplexity;
  public String validationPerplexity;

  public String testingPerplexity;

  public String jumblePerplexity;
  public String wordErrorRate;
  public String percentCorrect;
  public double modelSum;
  public List<Double> weights;

  public static String banner() {
    return "Name,TuningValues,trainingPerp,validationPerp,testingPerp,jumblePerp,WER,%right,sum";
  }

  public String toString() {
    return modelName + "," + (wasTuned ? weights.toString().replaceAll(",", ":") : "N/A")+ "," + trainingPerplexity + ","
        + validationPerplexity + "," + testingPerplexity + ","
        + jumblePerplexity + "," + wordErrorRate + "," + percentCorrect + ","
        + modelSum;
  }

}
