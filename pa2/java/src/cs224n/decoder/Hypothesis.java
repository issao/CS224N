package cs224n.decoder;
import cs224n.wordaligner.*;
import cs224n.langmodel.*;
import cs224n.util.*;

import java.util.*;
import java.io.*;


/* Helper class holds the current hypothesis */

class Hypothesis {
  boolean recalcNeeded = true;
  private Alignment alignment;

  private List<String> sourceSentence;
  private LanguageModel lm;
  private WordAligner wa;

  private List<String> targetSentence;
  private double langlog = 0;   // log of P(e)
  private double translog = 0;  // log of P(f|e)
  private double elen = 0; // log of length(e)

  private double lmweight = 0, transweight = 0, lengthweight = 0;

  public Hypothesis(List<String> sourceSentence, LanguageModel lm , WordAligner wa, double lmweight, double transweight, double lengthweight){
    this.sourceSentence = sourceSentence;
    this.lm = lm;
    this.wa = wa;
    alignment = new Alignment();
    targetSentence = new ArrayList<String>();
    this.lmweight = lmweight;
    this.transweight = transweight;
    this.lengthweight = lengthweight;
    recalcNeeded = true;
  }

  public Hypothesis(Hypothesis h){
    sourceSentence = h.sourceSentence; // this one should remain unaltered
    wa = h.wa; // this one should remain unaltered
    lm = h.lm; // this one should remain unaltered
    targetSentence = new ArrayList<String>(h.targetSentence);
    alignment = new Alignment(h.alignment);
    this.translog = h.translog;
    this.langlog = h.langlog;
    this.elen = h.elen;
    this.lmweight = h.lmweight;
    this.transweight = h.transweight;
    this.lengthweight = h.lengthweight;
    recalcNeeded = h.recalcNeeded;
  }


  public void addAlignment(int englishPosition, int frenchPosition, boolean sure) {
    alignment.addAlignment(englishPosition, frenchPosition, sure);
    recalcNeeded = true;
  }
  
  public int getAlignedTarget(int sourcePosition){
    return alignment.getAlignedTarget(sourcePosition);
  }

  public Set<Integer> getAlignedSources(int targetPosition){
    return alignment.getAlignedSources(targetPosition);
  }
  

  public boolean removeAlignment(int englishPosition, int frenchPosition){
    recalcNeeded = true;
    return alignment.removeAlignment(englishPosition,frenchPosition);
  }


  public void setTargetSentence(int i, String str) {
    recalcNeeded = true;
    targetSentence.set(i,str);
  }


  public List<String> dupTargetSentence() {
    return new ArrayList<String>(targetSentence);
  }

  public String getTargetSentence(int i) {
    return targetSentence.get(i);
  }

  public void addTargetSentence(String str) {
    targetSentence.add(str);
    recalcNeeded = true;
  }

  public int getTargetSentSize() {
    return targetSentence.size();
  }

  public void deleteWord(int index, int changeto){
    if(index == -1) return;
    targetSentence.remove(index);
    alignment.shiftAlignmentsDown(index, changeto);
    recalcNeeded = true;
  }

  public void addWord(String word, int target_index, int source_index){
    targetSentence.add(target_index, word);
    alignment.shiftAlignmentsUp(target_index);
    if(source_index > -1){
      alignment.addAlignment(target_index,source_index,true);
    }
    recalcNeeded = true;
  }
    
  public void swap (int i1, int i2, int j1, int j2){
    List<String> newTargetSentence = new ArrayList<String>();
    newTargetSentence.addAll(targetSentence.subList(0,i1));
    newTargetSentence.addAll(targetSentence.subList(j1,j2+1));
    newTargetSentence.addAll(targetSentence.subList(i2+1,j1));
    newTargetSentence.addAll(targetSentence.subList(i1,i2+1));
    newTargetSentence.addAll(targetSentence.subList(j2+1,targetSentence.size()));
    assert(targetSentence.size() == newTargetSentence.size());
    //System.out.println(targetSentence.size()+" to "+newTargetSentence.size());
    targetSentence = newTargetSentence;
    alignment.swap(i1,i2,j1,j2);
    recalcNeeded = true;
  }


  /*
   * Returns the log probability.  while not theoretically justfied
   * playing around with the constants that are multiplied by the
   * different probabilities can lead to better results (such as
   * increasing the weight of language model probability).
   */
  public double getProb(){
    //return 2.0*langlog + 1.0*translog;
    if (recalcNeeded) calcProbs();
    return lmweight*langlog + transweight*translog + lengthweight*elen;
    //return 2.0*langlog + 1.0*translog;
    //return 1.0*langlog + 1.0*translog;
  }

  public double getlogLang() {
    if (recalcNeeded) calcProbs();
    return langlog;
  }

  public double getlogTrans() {
    if (recalcNeeded) calcProbs();
    return translog;
  }

  public double getElen() {
    if (recalcNeeded) calcProbs();
    return elen;
  }



  /* Calculates probability of hypothesis according
   *   passed in language and alignment models.
   */

  private void calcProbs() {
    langlog = Math.log(lm.getSentenceProbability(targetSentence));
    //langlog = Math.log(lm.getSentenceProbability(targetSentence))/targetSentence.size()*sourceSentence.size();
    translog = Math.log(wa.getAlignmentProb(targetSentence, sourceSentence, alignment));
    //translog = wa.getAlignmentLogProb(targetSentence, sourceSentence, alignment);
    //elen = Math.log(targetSentence.size());
    elen = targetSentence.size();
    recalcNeeded = false;
    //System.err.println(" (RECAL) ");
  }

  public String toString(){
    StringBuilder sb = new StringBuilder();
    //for(String word : targetSentence){
    for(int i = 0; i < targetSentence.size(); i++){
      String word = targetSentence.get(i);
      sb.append("(e"+i+")"+word+" ");
    }
    sb.append("\n");
    sb.append(alignment.toString());
    return sb.toString();
  }
}

