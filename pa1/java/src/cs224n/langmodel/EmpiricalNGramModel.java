package cs224n.langmodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs224n.util.Counter;

public class EmpiricalNGramModel extends NGram {

  private Set<String> lexicon;
  private Map<List<String>, Counter<String>> rawCount;
  private Map<List<String>, Double> totalCount;

  public EmpiricalNGramModel(int n) {
    super(n);
    lexicon = new HashSet<String>();
    rawCount = new HashMap<List<String>, Counter<String>>();
    totalCount = new HashMap<List<String>, Double>();
  }

  @Override
  public void train(Collection<List<String>> trainingSentences) {
    for (List<String> sentence : trainingSentences) {
      List<String> stoppedSentence = new ArrayList<String>(sentence);
      for (int i = 0; i < n - 1; i++) {
        stoppedSentence.add(0, START);
      }
      stoppedSentence.add(STOP);
      for (int i = n - 1; i < stoppedSentence.size(); i++) {
        lexicon.add(stoppedSentence.get(i));
        addNgramCount(stoppedSentence, i);
      }
    }
    for (List<String> prefix : rawCount.keySet()) {
      totalCount.put(prefix, rawCount.get(prefix).totalCount());
    }
  }

  private void addNgramCount(List<String> stoppedSentence, int index) {
    assert index + 1 >= n;
    List<String> prefix = getPrefix(stoppedSentence, index);
    if (!rawCount.containsKey(prefix)) {
      rawCount.put(prefix, new Counter<String>());
    }
    rawCount.get(prefix).incrementCount(stoppedSentence.get(index), 1.0);
  }

  @Override
  protected double getWordProbability(List<String> prefix, String word) {
    assert prefix.size() == n - 1;
    if (!rawCount.containsKey(prefix)) {
      // Missing prefix, give uniform probability.
      return 1.0 / (lexicon().size() + 1); 
    }
    return rawCount.get(prefix).getCount(word) / totalCount.get(prefix);
  }

  @Override
  protected Set<List<String>> knownPrefixes() {
    return rawCount.keySet();
  }

  @Override
  protected Set<String> knownWords(List<String> prefix) {
    assert prefix.size() == n - 1;
    return rawCount.get(prefix).keySet();
  }

  @Override
  protected Set<String> lexicon() {
    return lexicon;
  }
  
  public int getCount(List<String> prefix, String word) {
    assert prefix.size() == n - 1;
    if (!rawCount.containsKey(prefix)) {
      return 0;
    }
    return (int) rawCount.get(prefix).getCount(word);
  }

  public Counter<String> getPrefixCounter(List<String> prefix) {
    assert prefix.size() == n - 1;
    if (!rawCount.containsKey(prefix)) {
      return null;
    }
    return rawCount.get(prefix);
  }
}
