package cs224n.assignments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cs224n.langmodel.EmpiricalNGramModel;
import cs224n.langmodel.NGram;

public class SentencePrinter {
  private static final double logPperSpace = 2.0;

  public static void print(List<String> sentence, NGram model) {
    int n = model.getN();
    List<String> prefix = new ArrayList<String>();
    prefix.addAll(Collections.nCopies(n - 1, NGram.START));
    System.out.println(flatten(prefix));
    String spaces = "";
    for (String word : sentence) {
      double logP = -1 * Math.log(model.getWordProbability(prefix, word));
      int nSpaces = (int) (logP / logPperSpace);
      for (int i = 0; i < nSpaces; i++) {
        spaces += " ";
      }
      System.out.print(spaces + flatten(prefix) + " " + word + " ["
          + Math.exp(-1 * logP) + "]");
      if (!model.knownPrefixes().contains(prefix)) {
        System.out.print("[*]");
      } else if (!model.knownWords(prefix).contains(word)) {
        System.out.print("[**]");
      }
      System.out.println();
      prefix.remove(0);
      prefix.add(word);
    }
  }

  private static String flatten(List<String> sentence) {
    String s = "";
    for (int i = 0; i < sentence.size(); i++) {
      if (i > 0) {
        s += " ";
      }
      s += sentence.get(i);
    }
    return s;
  }
}
