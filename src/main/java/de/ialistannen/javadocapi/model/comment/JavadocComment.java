package de.ialistannen.javadocapi.model.comment;

import java.util.Collections;
import java.util.List;

public class JavadocComment {

  private final List<JavadocCommentTag> tags;
  private final List<JavadocCommentFragment> content;

  public JavadocComment(List<JavadocCommentTag> tags, List<JavadocCommentFragment> content) {
    this.tags = List.copyOf(tags);
    this.content = content;
  }

  public List<JavadocCommentTag> getTags() {
    return tags;
  }

  public List<JavadocCommentFragment> getContent() {
    return Collections.unmodifiableList(content);
  }

  @Override
  public String toString() {
    return "JavadocComment{" +
        "tags=" + tags +
        ", content=" + content +
        '}';
  }
}
