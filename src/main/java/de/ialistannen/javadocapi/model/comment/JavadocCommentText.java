package de.ialistannen.javadocapi.model.comment;

public class JavadocCommentText implements JavadocCommentFragment {

  private final String text;

  public JavadocCommentText(String text) {
    this.text = text;
  }

  public String getText() {
    return text;
  }

  @Override
  public <T> T accept(CommentVisitor<T> visitor) {
    return visitor.visitText(this);
  }

  @Override
  public String toString() {
    return '"' + text + '"';
  }
}
