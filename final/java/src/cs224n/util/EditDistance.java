package cs224n.util;

//import java.io.*;
import java.util.*;

/**
 * @author Dan Klein
 */


/**
   * An edit distance is a measure of difference between two sentences.  We
   * assume that one sentence can be transformed into another through a
   * sequence of INSERT, DELETE, and SUBSTITUTE operations, each costing 1
   * unit.  For example, the sentence "The quick brown fox jumped over the
   * lazy dog." can be transformed into the sentence "The fox tripped over
   * the fat lazy dog." using 2 DELETEs, 1 SUBSTITUTE, and 1 INSERT, for a
   * cost of 4.  In general, given two sentences, there are multiple
   * sequences of operations which transform the first sentence into the
   * second; the edit distance is defined to be the cost of the
   * <i>cheapest</i> such sequence.
   */
  public class EditDistance {

    static double INSERT_COST = 1.0;
    static double DELETE_COST = 1.0;
    static double SUBSTITUTE_COST = 1.0;

    private double[][] initialize(double[][] d) {
      for (int i = 0; i < d.length; i++) {
        for (int j = 0; j < d[i].length; j++) {
          d[i][j] = Double.NaN;
        }
      }
      return d;
    }

    /**
     * Returns the edit distance between two sentences.  Operates by
     * calling a recursive (dynamic programming) helper method.
     */
    public double getDistance(List<String> firstList, List<String> secondList) {
      double[][] bestDistances =
        initialize(new double[firstList.size() + 1][secondList.size() + 1]);
      return getDistance(firstList, secondList, 0, 0, bestDistances);
    }

    /**
     * Computes the edit distance between a suffix of the first list and a
     * suffix of the second listusing dynamic programming.  The best
     * distances for small suffixes are computed first and memoized in the
     * bestDistances table, which is passed around through recursive calls.
     * Edit distances for longer suffixes are computed in terms of edit
     * distances for shorter suffixes by examining alternative operations.
     */
    private double getDistance(List<String> firstList,
                               List<String> secondList,
                               int firstPosition,
                               int secondPosition,
                               double[][] bestDistances) {
      
      // if either suffix has "negative" length...
      if (firstPosition > firstList.size() || secondPosition > secondList.size())
        return Double.POSITIVE_INFINITY;

      // if both suffixes are null, distance is 0 (base case for recursion)
      if (firstPosition == firstList.size() && secondPosition == secondList.size())
        return 0.0;

      // if this distance has not yet been memoized in table...
      if (Double.isNaN(bestDistances[firstPosition][secondPosition])) {

        // start distance high and try alternative ways to reduce it...
        double distance = Double.POSITIVE_INFINITY;

        distance =                      // insert a word?
          Math.min(distance, INSERT_COST +
                   getDistance(firstList, secondList, 
                               firstPosition + 1, secondPosition,
                               bestDistances));
        distance =                      // delete a word?
          Math.min(distance, DELETE_COST +
                   getDistance(firstList, secondList, 
                               firstPosition, secondPosition + 1, 
                               bestDistances));
        distance =                      // substitute a word?
          Math.min(distance, SUBSTITUTE_COST +
                   getDistance(firstList, secondList,
                               firstPosition + 1, secondPosition + 1,
                               bestDistances));
        
        // if first words match, no cost incurred here
        if (firstPosition < firstList.size() && secondPosition < secondList.size()) {
          if (firstList.get(firstPosition).equals(secondList.get(secondPosition))) {
            distance = 
              Math.min(distance, 
                       getDistance(firstList, secondList,
                                   firstPosition + 1, secondPosition + 1,
                                   bestDistances));
          }
        }

        // memoize for future reference
        bestDistances[firstPosition][secondPosition] = distance;
      }
      return bestDistances[firstPosition][secondPosition];
    }
  }
