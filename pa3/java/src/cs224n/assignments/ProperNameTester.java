package cs224n.assignments;

import cs224n.classify.LabeledDatum;
import cs224n.classify.ProbabilisticClassifier;
import cs224n.classify.BasicLabeledDatum;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Harness for testing the accuracy of a proper name type classifier.  Edit the
 * main method so that your classifier takes the place of the
 * MostFrequentLabelClassifier.
 *
 * @author Dan Klein
 */
public class ProperNameTester {

  static List<LabeledDatum<Character, String>> loadData(String fileName) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(fileName));
    List<LabeledDatum<Character,String>> labeledDatums = new ArrayList<LabeledDatum<Character, String>>();
    while (reader.ready()) {
      String line = reader.readLine();
      String[] parts = line.split("\t");
      String label = parts[0];
      char[] chars = parts[1].toCharArray();
      List<Character> features = new ArrayList<Character>();
      for (int i = 0; i < chars.length; i++) {
        char c = chars[i];
        features.add(c);
      }
      LabeledDatum<Character, String> labeledDatum = new BasicLabeledDatum<Character, String>(label, features);
      labeledDatums.add(labeledDatum);
    }
    return labeledDatums;
  }

  static void testClassifier(ProbabilisticClassifier<Character, String> classifier, List<LabeledDatum<Character, String>> testData) {
    double numCorrect = 0.0;
    double numTotal = 0.0;
    for (LabeledDatum<Character, String> testDatum : testData) {
      String label = classifier.getLabel(testDatum);
      if (label.equals(testDatum.getLabel()))
        numCorrect += 1.0;
      numTotal += 1.0;
    }
    double accuracy = numCorrect / numTotal;
    System.out.println("Accuracy: "+accuracy);
  }

  public static void main(String[] args) throws IOException {
    String basePath = ".";
    if (args.length > 0) {
      basePath = args[0];
    }
    List<LabeledDatum<Character, String>> trainingData = loadData(basePath+"/pnp-train.txt");
    List<LabeledDatum<Character, String>> validationData = loadData(basePath+"/pnp-validate.txt");
    List<LabeledDatum<Character, String>> testData = loadData(basePath+"/pnp-test.txt");
    ProbabilisticClassifier<Character, String> mostFrequentLabelClassifier = new MostFrequentLabelClassifier.Factory<Character, String>().trainClassifier(trainingData);
    testClassifier(mostFrequentLabelClassifier, testData);
  }
}
