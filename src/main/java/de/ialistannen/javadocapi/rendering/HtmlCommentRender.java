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
      String argument = fixIndentOfFirstLine(tag.getArgument().orElse(""));

      return switch (tag.getType()) {
        case LITERAL, VALUE -> codeOpenTag + argument + "</code>";
        case CODE -> {
          String result = codeOpenTag + escapeHtml(argument) + "</code>";
          if (result.lines().count() > 1) {
            result = "<pre>" + result + "</pre>";
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

    private String escapeHtml(String text) {
      return text.replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Spoon doesn't parse tags correctly and might swallow the indent for the first line and turn:
     * <pre>
     * {@code
     * {@code
     *   Hello world
     * }}
     * </pre>
     * into {@code "Hello world"}
     *
     * @param text the code text to fix
     * @return the input with the indent of the first line normalized to the following lines if
     *     needed
     */
    private String fixIndentOfFirstLine(String text) {
      if (text.startsWith(" ")) {
        return text;
      }
      List<String> lines = text.lines().collect(Collectors.toList());
      if (lines.size() <= 1) {
        return text;
      }
      int indent = lines.stream()
          .skip(1)
          .filter(it -> !it.isBlank())
          .mapToInt(it -> it.length() - it.stripLeading().length())
          .min()
          .orElse(0);

      return " ".repeat(indent) + text;
    }
  }
}
