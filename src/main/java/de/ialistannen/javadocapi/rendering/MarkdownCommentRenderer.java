package de.ialistannen.javadocapi.rendering;

import de.ialistannen.javadocapi.model.comment.JavadocCommentFragment;
import io.github.furstenheim.CodeBlockStyle;
import io.github.furstenheim.CopyDown;
import io.github.furstenheim.LinkStyle;
import io.github.furstenheim.Options;
import io.github.furstenheim.OptionsBuilder;
import java.util.List;

public class MarkdownCommentRenderer implements CommentRenderer {

  private final HtmlCommentRender htmlCommentRender = new HtmlCommentRender();
  private final CopyDown copyDown = new CopyDown(
      OptionsBuilder.anOptions()
          .withCodeBlockStyle(CodeBlockStyle.FENCED)
          .build()
  );

  @Override
  public String render(List<JavadocCommentFragment> fragments) {
    return copyDown.convert(htmlCommentRender.render(fragments));
  }
}
