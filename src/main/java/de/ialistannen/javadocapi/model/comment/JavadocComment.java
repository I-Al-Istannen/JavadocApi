package de.ialistannen.javadocapi.model.comment;

import java.util.Collections;
import java.util.List;

public class JavadocComment {

  private final List<JavadocCommentTag> tags;
  private final List<List<JavadocCommentFragment>> paragraphs;

  public JavadocComment(List<JavadocCommentTag> tags,
      List<List<JavadocCommentFragment>> paragraphs) {
    this.tags = List.copyOf(tags);
    this.paragraphs = paragraphs;
  }

  public List<JavadocCommentTag> getTags() {
    return tags;
  }

  public List<List<JavadocCommentFragment>> getParagraphs() {
    return Collections.unmodifiableList(paragraphs);
  }

  @Override
  public String toString() {
    return "JavadocComment{" +
        "tags=" + tags +
        ", paragraphs=" + paragraphs +
        '}';
  }
}
