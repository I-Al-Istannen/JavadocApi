package de.ialistannen.javadocapi.rendering;

import de.ialistannen.javadocapi.model.comment.JavadocCommentFragment;
import io.github.furstenheim.CopyDown;
import java.util.List;

public class MarkdownCommentRenderer implements CommentRenderer {

  private final HtmlCommentRender htmlCommentRender = new HtmlCommentRender();
  private final CopyDown copyDown = new CopyDown();

  @Override
  public String render(List<JavadocCommentFragment> fragments) {
    return copyDown.convert(htmlCommentRender.render(fragments));
  }
}
