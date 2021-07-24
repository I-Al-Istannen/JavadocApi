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
public class HtmlCommentRender implements CommentRenderer {

  private final LinkResolveStrategy linkResolveStrategy;

  public HtmlCommentRender(LinkResolveStrategy linkResolveStrategy) {
    this.linkResolveStrategy = linkResolveStrategy;
  }

  /**
   * Renders a list of fragments to an HTML text.
   *
   * @param fragments the fragments to render
   * @param baseUrl the base url
   * @return the rendered HTML
   */
  @Override
  public String render(List<JavadocCommentFragment> fragments, String baseUrl) {
    return fragments.stream()
        .map(it -> it.accept(new Visitor(linkResolveStrategy, baseUrl)))
        .collect(Collectors.joining());
  }

  private static class Visitor implements CommentVisitor<String> {

    private final LinkResolveStrategy linkResolveStrategy;
    private final String baseUrl;

    private Visitor(LinkResolveStrategy linkResolveStrategy, String baseUrl) {
      this.linkResolveStrategy = linkResolveStrategy;
      this.baseUrl = baseUrl;
    }

    @Override
    public String visitLink(JavadocCommentLink link) {
      String label = link.getLabel().orElse(formatLink(link.getTarget()));
      String target = linkResolveStrategy.resolveLink(link.getTarget(), baseUrl);
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
      // Magic incantation to make CopyDown emit proper language tags
      String codeOpenTag = "<code class=\"language-java\">";
      return switch (tag.getType()) {
        case LITERAL, VALUE -> codeOpenTag + tag.getArgument().orElse("") + "</code>";
        case CODE -> {
          String result = codeOpenTag + tag.getArgument().orElse("") + "</code>";
          if (result.lines().count() > 1) {
            result = "<pre><code>" + result + "</code></pre>";
          }
          yield result;
        }
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
}
