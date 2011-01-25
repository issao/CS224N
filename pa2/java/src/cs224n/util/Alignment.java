package cs224n.util;

import java.io.*;
import java.util.*;

/**
 * @author Dan Klein
 */ 


/**
   * Alignments serve two purposes, both to indicate your system's guessed
   * alignment, and to hold the gold standard alignments.  Alignments map index
   * pairs to one of three values, unaligned, possibly aligned, and surely
   * aligned.  Your alignment guesses should only contain sure and unaligned
   * pairs, but the gold alignments contain possible pairs as well.
   *
   * To build an alignemnt, start with an empty one and use
   * addAlignment(i,j,true).  To display one, use the render method.
   */
  public class Alignment {
    Set<Pair<Integer, Integer>> sureAlignments;
    Set<Pair<Integer, Integer>> possibleAlignments;

    public Set<Pair<Integer,Integer>> getSureAlignments(){
      return sureAlignments;
    }

    public boolean containsSureAlignment(int englishPosition, int frenchPosition) {
      return sureAlignments.contains(new Pair<Integer, Integer>(englishPosition, frenchPosition));
    }

    public boolean containsPossibleAlignment(int englishPosition, int frenchPosition) {
      return possibleAlignments.contains(new Pair<Integer, Integer>(englishPosition, frenchPosition));
    }

    public boolean removeAlignment(int englishPosition, int frenchPosition){
      return sureAlignments.remove(new Pair<Integer, Integer>(englishPosition, frenchPosition));
    }

    public void addAlignment(int englishPosition, int frenchPosition, boolean sure) {
      Pair<Integer, Integer> alignment = new Pair<Integer, Integer>(englishPosition, frenchPosition);
      if (sure)
        sureAlignments.add(alignment);
      possibleAlignments.add(alignment);
    }

    /*** START METHODS USED BY DECODE */
    public void shiftAlignmentsUp(int targetPosition){
      shiftAlignments(targetPosition, true, -999);
    }

    public void shiftAlignmentsDown(int targetPosition, int changeto){
      shiftAlignments(targetPosition, false, changeto);
    }

    public void swap(int i1, int i2, int j1, int j2){

      int ilength = i2-i1+1;
      int jlength = j2-j1+1;
      int diff = jlength - ilength;

      Set<Pair<Integer,Integer>> newAlignments = new HashSet<Pair<Integer,Integer>>();
      
      for(Pair<Integer,Integer> alignment : sureAlignments){
        int pos = alignment.getFirst();

        if(pos < i1 || pos > j2){
          newAlignments.add(alignment);
        }
        else if (pos >= i1 && pos <= i2){
          newAlignments.add(new Pair<Integer,Integer>(pos+j2-i2, alignment.getSecond()));
        }
        else if (pos > i2 && pos < j1){
          newAlignments.add(new Pair<Integer,Integer>(pos+diff, alignment.getSecond()));
        }
        else if(pos >= j1 && pos <= j2){
          newAlignments.add(new Pair<Integer,Integer>(pos-j1+i1, alignment.getSecond()));
        }
        else{
          System.err.println("Error in Alignment.swap()");
          System.exit(1);
        }
        
      }

      sureAlignments = newAlignments;
    }

    public String toString(){
      StringBuilder sb = new StringBuilder();
      for(Pair<Integer,Integer> a : sureAlignments){
        //sb.append(a+"; ");
        String alignment = "(e" + a.getFirst() + ", f" + a.getSecond() + ")";
        sb.append(alignment+"; ");
      }
      return sb.toString();
    }


    private void shiftAlignments(int targetPosition, boolean up, int changeto){

      Set<Pair<Integer,Integer>> newAlignments = new HashSet<Pair<Integer,Integer>>();

      for(Pair<Integer,Integer> alignment : sureAlignments){
        int pos = alignment.getFirst();
        //System.out.println("shift: "+pos);

        if(pos < targetPosition){
          newAlignments.add(alignment);
        }
        else if(pos == targetPosition){
          newAlignments.add( new Pair<Integer,Integer>( (up ? pos+1 : changeto) , alignment.getSecond()) );
        }
        else if(pos > targetPosition){
          newAlignments.add(new Pair<Integer,Integer>( (up ? pos+1 : pos-1), alignment.getSecond()) );
        }
      }

      sureAlignments = newAlignments;
    }

    public int getAlignedTarget(int sourcePosition){
      for(Pair<Integer,Integer> alignment : sureAlignments){
        if(alignment.getSecond() == sourcePosition){
          return alignment.getFirst();
        }
      }
      System.err.println("nothing aligned with "+sourcePosition);
      return -999;
    }

    public Set<Integer> getAlignedSources(int targetPosition){
      Set<Integer> sources = new HashSet<Integer>();
      for(Pair<Integer,Integer> alignment : sureAlignments){
        if(alignment.getFirst() == targetPosition){
          sources.add(alignment.getSecond());
        }
      }
      return sources;
    }
    /* END METHODS USED BY DECODER ***/

    public Alignment(Alignment a){
      sureAlignments = new HashSet<Pair<Integer,Integer>>(a.sureAlignments);
      possibleAlignments = new HashSet<Pair<Integer,Integer>>(a.possibleAlignments);
    }

    public Alignment() {
      sureAlignments = new HashSet<Pair<Integer, Integer>>();
      possibleAlignments = new HashSet<Pair<Integer, Integer>>();
    }
    
    public static String render(Alignment alignment, SentencePair sentencePair) {
      return render(alignment, alignment, sentencePair);
    }
    
    public static String render(Alignment reference, Alignment proposed, SentencePair sentencePair) {
      StringBuilder sb = new StringBuilder();
      for (int frenchPosition = 0; frenchPosition < sentencePair.getFrenchWords().size(); frenchPosition++) {
        for (int englishPosition = 0; englishPosition < sentencePair.getEnglishWords().size(); englishPosition++) {
          boolean sure = reference.containsSureAlignment(englishPosition, frenchPosition);
          boolean possible = reference.containsPossibleAlignment(englishPosition, frenchPosition);
          char proposedChar = ' ';
          if (proposed.containsSureAlignment(englishPosition, frenchPosition))
            proposedChar = '#';
          if (sure) {
            sb.append('[');
            sb.append(proposedChar);
            sb.append(']');
          } else {
            if (possible) {
              sb.append('(');
              sb.append(proposedChar);
              sb.append(')');
            } else {
              sb.append(' ');
              sb.append(proposedChar);
              sb.append(' ');
            }
          }
        }
        sb.append("| ");
        sb.append(sentencePair.getFrenchWords().get(frenchPosition));
        sb.append('\n');
      }
      for (int englishPosition = 0; englishPosition < sentencePair.getEnglishWords().size(); englishPosition++) {
        sb.append("---");
      }
      sb.append("'\n");
      boolean printed = true;
      int index = 0;
      while (printed) {
        printed = false;
        StringBuilder lineSB = new StringBuilder();
        for (int englishPosition = 0; englishPosition < sentencePair.getEnglishWords().size(); englishPosition++) {
          String englishWord = sentencePair.getEnglishWords().get(englishPosition);
          if (englishWord.length() > index) {
            printed = true;
            lineSB.append(' ');
            lineSB.append(englishWord.charAt(index));
            lineSB.append(' ');
          } else {
            lineSB.append("   ");
          }
        }
        index += 1;
        if (printed) {
          sb.append(lineSB);
          sb.append('\n');
        }
      }
      return sb.toString();
    }
  }
