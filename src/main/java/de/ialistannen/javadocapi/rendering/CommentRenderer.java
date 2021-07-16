package de.ialistannen.javadocapi.rendering;

import de.ialistannen.javadocapi.model.comment.JavadocCommentFragment;
import java.util.List;

public interface CommentRenderer {

  /**
   * Renders a list of fragments.
   *
   * @param fragments the fragments to render
   * @param baseUrl the base url to use (empty if none)
   * @return the rendered form
   */
  String render(List<JavadocCommentFragment> fragments, String baseUrl);
}
