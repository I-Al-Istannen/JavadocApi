package de.ialistannen.javadocapi.model.comment;

import java.util.List;
import java.util.Optional;

public class JavadocCommentTag {

  private final String tagName;
  private final String argument;
  private final List<JavadocCommentFragment> content;

  public JavadocCommentTag(String tagName, String argument, List<JavadocCommentFragment> content) {
    this.tagName = tagName;
    this.argument = argument;
    this.content = content;
  }

  public String getTagName() {
    return tagName;
  }

  public Optional<String> getArgument() {
    return Optional.ofNullable(argument);
  }

  public List<JavadocCommentFragment> getContent() {
    return content;
  }

  @Override
  public String toString() {
    return "@" + tagName + getArgument().map(it -> " " + it).orElse("") + " " + content;
  }

}
