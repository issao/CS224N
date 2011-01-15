package cs224n.util;

import java.io.*;
import java.util.*;

/**
 * @author Dan Klein
 */ 

 /** A <code>Sentences</code> object is a <code>Collection</code> of
   *  sentences backed by a text file containing one sentence per line.  It
   *  provides an iterator over the sentences.  Because the collection is
   *  disk-backed, any operation other than iterating is likely to be slow.
   */
  public class Sentences extends AbstractCollection<List<String>> {

    /** 
     * An <code>Iterator</code> wrapped around a
     * <code>BufferedReader</code>.  Each call to <code>next()</code> reads
     * a line, lowercases it, splits it (on whitespace) into words, and
     * returns a list of the words.
     */
    static class SentenceIterator implements Iterator<List<String>> {

      BufferedReader reader;

      public boolean hasNext() {
        try {
          return reader.ready();
        } catch (IOException e) {
          return false;
        }
      }

      public List<String> next() {
        try {
          String line = reader.readLine();
          String[] words = line.split("\\s+");
          List<String> sentence = new ArrayList<String>();
          for (int i = 0; i < words.length; i++) {
            String word = words[i];
            sentence.add(word.toLowerCase());
          }
          return sentence;
        } catch (IOException e) {
          throw new NoSuchElementException();
        }
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }

      public SentenceIterator(BufferedReader reader) {
        this.reader = reader;
      }

    } // end SentenceIterator

    String filename;

    /** Returns an iterator over the sentences in this collection. 
     */
    public Iterator<List<String>> iterator() {
      try {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        return new SentenceIterator(reader);
      } catch (FileNotFoundException e) {
        throw new RuntimeException("File not found: " + filename);
      }
    }

    /** Returns the numbers of sentences in this collection.  (This
     *  iterates through all sentences, so it may be slow.)
     */
    public int size() {
      int size = 0;
      Iterator<List<String>> i = iterator();
      while (i.hasNext()) {
        size++;
        i.next();
      }
      return size;
    }

    /** Constructs a new sentence collection from the name of the file
     *  containing the sentences.
     */
    public Sentences(String filename) {
      this.filename = filename;
      iterator();                       // causes error to be thrown if file not readable
    }

    /** Takes the name of a file containing sentences and returns a new
     *  <code>SentenceCollection</code> backed by that file.
     */
    public static class Reader {
      public static Collection<List<String>> readSentences(String filename)
        throws FileNotFoundException {
        return new Sentences(filename);
      }
    }

  } // end SentenceCollection
