package de.ialistannen.javadocapi.model;

import de.ialistannen.javadocapi.model.comment.JavadocComment;
import java.util.Optional;

/**
 * A javadoc element.
 */
public interface JavadocElement {

  /**
   * @return the name of this element
   */
  QualifiedName getQualifiedName();

  /**
   * @return the comment associated with this element
   */
  Optional<JavadocComment> getComment();

  /**
   * Returns the declaration in the given style.
   *
   * @param style the declaration style to use
   * @return the declaration as a String in the given style
   */
  String getDeclaration(DeclarationStyle style);

  enum DeclarationStyle {
    SHORT,
    QUALIFIED
  }
}
