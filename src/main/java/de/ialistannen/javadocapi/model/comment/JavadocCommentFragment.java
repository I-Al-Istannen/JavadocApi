package de.ialistannen.javadocapi.model.comment;

public interface JavadocCommentFragment {

  /**
   * Accepts a visitor!
   *
   * @param visitor the visitor to accept
   * @param <T> the return type of the visitor
   * @return the result of the visitor
   */
  <T> T accept(CommentVisitor<T> visitor);
}
