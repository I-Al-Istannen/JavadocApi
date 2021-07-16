package de.ialistannen.javadocapi.rendering;

import de.ialistannen.javadocapi.model.comment.JavadocCommentFragment;
import io.github.furstenheim.CodeBlockStyle;
import io.github.furstenheim.CopyDown;
import io.github.furstenheim.OptionsBuilder;
import java.util.List;

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
    return copyDown.convert(htmlCommentRender.render(fragments, baseUrl));
  }
}
