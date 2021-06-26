package de.ialistannen.javadocapi.rendering;

import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.comment.CommentVisitor;
import de.ialistannen.javadocapi.model.comment.JavadocCommentFragment;
import de.ialistannen.javadocapi.model.comment.JavadocCommentInlineTag;
import de.ialistannen.javadocapi.model.comment.JavadocCommentLink;
import de.ialistannen.javadocapi.model.comment.JavadocCommentText;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Renders a List of {@link JavadocCommentFragment}s to an HTML test.
 */
public class HtmlCommentRender implements CommentVisitor<String>, CommentRenderer {

  /**
   * Renders a list of fragments to an HTML text.
   *
   * @param fragments the fragments to render
   * @return the rendered HTML
   */
  @Override
  public String render(List<JavadocCommentFragment> fragments) {
    return fragments.stream().map(it -> it.accept(this)).collect(Collectors.joining());
  }

  @Override
  public String visitLink(JavadocCommentLink link) {
    String label = link.getLabel().orElse(formatLink(link.getTarget()));
    String target = "https://" + link.getTarget().asString();
    if (!link.isPlain()) {
      label = "<code>" + label + "</code>";
    }
    return "<a href=\"" + target + "\">" + label + "</a>";
  }

  private String formatLink(QualifiedName name) {
    if (name.asString().contains("#")) {
      String parent = name.getLexicalParent().orElseThrow().getSimpleName();
      parent += name.asString().substring(name.asString().indexOf("#"));
      return parent;
    }
    return name.getSimpleName();
  }

  @Override
  public String visitInlineTag(JavadocCommentInlineTag tag) {
    return switch (tag.getType()) {
      case LITERAL, VALUE -> "<code>" + tag.getArgument().orElse("") + "</code>";
      case CODE -> "<pre>" + tag.getArgument().orElse("") + "</pre>";
      case DOC_ROOT -> "{Why did you place a doc root?}";
      case INDEX -> "{Who uses an index?}";
      case INHERIT_DOC -> "{@inheritDoc}";
      case LINK, LINKPLAIN -> "";
      case UNKNOWN -> "{UNKNOWN}";
    };
  }

  @Override
  public String visitText(JavadocCommentText text) {
    return text.getText();
  }
}
