package de.ialistannen.javadocapi.rendering;

import de.ialistannen.javadocapi.model.comment.JavadocCommentFragment;
import io.github.furstenheim.CodeBlockStyle;
import io.github.furstenheim.CopyDown;
import io.github.furstenheim.OptionsBuilder;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

public class MarkdownCommentRenderer implements CommentRenderer {

  private final HtmlCommentRender htmlCommentRender;
  private final CopyDown copyDown;

  public MarkdownCommentRenderer(LinkResolveStrategy linkResolveStrategy) {
    htmlCommentRender = new HtmlCommentRender(linkResolveStrategy);
    copyDown = new CopyDown(
        OptionsBuilder.anOptions()
            .withCodeBlockStyle(CodeBlockStyle.FENCED)
            .build()
    );
  }

  @Override
  public String render(List<JavadocCommentFragment> fragments, String baseUrl) {
    String html = htmlCommentRender.render(fragments, baseUrl);
    // Clean up pre elements without code as a direct child - the markdown renderer will ignore them
    // and it might be a bit nicer to always have a "code" element.
    Document document = Jsoup.parseBodyFragment(html);
    for (Element pre : document.getElementsByTag("pre")) {
      if (pre.children().size() != 1 || !pre.children().get(0).tagName().equals("code")) {
        Element code = pre.appendElement("code");
        code.attr("class", "language-java");

        // Move existing children one inside the code element
        List<Node> childNodes = pre.childNodesCopy();
        List.copyOf(pre.childNodes()).forEach(Node::remove);
        childNodes.forEach(code::appendChild);

        pre.appendChild(code);
      }
    }

    for (Element code : document.getElementsByTag("code")) {
      for (TextNode node : code.textNodes()) {
        String content = node.getWholeText();
        boolean hasNewline = content.endsWith("\n");
        if (hasNewline) {
          content = content.substring(0, content.length() - 1);
        }
        node.text(content.stripIndent() + (hasNewline ? "\n" : ""));
      }
    }

    for (Element pre : document.getElementsByTag("pre")) {
      Element quote = pre.parent();
      if (quote != null && quote.tagName().equals("blockquote")) {
        quote.unwrap();
      }
    }

    return copyDown.convert(document.getElementsByTag("body").get(0).html());
  }
}
