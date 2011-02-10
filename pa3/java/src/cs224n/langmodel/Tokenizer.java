package cs224n.langmodel;

import java.util.Iterator;

/**
 * An iterator with the ability to peek at the next token.
 * @author Dan Klein
 */
public interface Tokenizer<T> extends Iterator<T> {
  T peek();
}
