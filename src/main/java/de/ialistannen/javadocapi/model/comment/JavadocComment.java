package de.ialistannen.javadocapi.model.comment;

import java.util.List;

public class JavadocComment {

  private final List<JavadocCommentTag> tags;
  private final List<JavadocCommentFragment> shortDescription;
  private final List<JavadocCommentFragment> longDescription;

  public JavadocComment(List<JavadocCommentTag> tags, List<JavadocCommentFragment> shortDescription,
      List<JavadocCommentFragment> longDescription) {
    this.tags = List.copyOf(tags);
    this.shortDescription = List.copyOf(shortDescription);
    this.longDescription = List.copyOf(longDescription);
  }

  public List<JavadocCommentTag> getTags() {
    return tags;
  }

  public List<JavadocCommentFragment> getShortDescription() {
    return shortDescription;
  }

  public List<JavadocCommentFragment> getLongDescription() {
    return longDescription;
  }

  @Override
  public String toString() {
    return "JavadocComment{" +
        "tags=" + tags +
        ", shortDescription=" + shortDescription +
        ", longDescription=" + longDescription +
        '}';
  }
}
