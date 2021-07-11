package de.ialistannen.javadocapi.model;

import de.ialistannen.javadocapi.model.JavadocElement.DeclarationStyle;
import java.util.Optional;

/**
 * The qualified name of a javadoc element.
 */
public class QualifiedName extends Identifier {

  private final String asText;

  public QualifiedName(String asText) {
    this.asText = asText;
  }

  /**
   * @return the <em>lexical</em> parent, if any
   */
  public Optional<QualifiedName> getLexicalParent() {
    if (asText.contains("#")) {
      return Optional.of(new QualifiedName(asText.substring(0, asText.indexOf("#"))));
    }
    if (asText.contains(".")) {
      return Optional.of(new QualifiedName(asText.substring(0, asText.lastIndexOf('.'))));
    }
    return Optional.empty();
  }

  /**
   * Returns the simple name. The simple name of "java.lang.String" is "String" and the simple name
   * of "String#lenhth" is "length".
   *
   * @return the simple name (i.e. without any qualifier)
   */
  public String getSimpleName() {
    if (asText.contains("#")) {
      return asText.substring(asText.indexOf("#") + 1)
          // remove () from methods
          .replaceAll("\\(.*\\)", "");
    }
    return asText.substring(asText.lastIndexOf('.') + 1);
  }

  /**
   * Checks if this qualified name belongs to a method by verifying {@literal (} appears in the
   * name.
   *
   * @return true if this name is for a method
   */
  public boolean isMethod() {
    return asText.contains("(");
  }

  /**
   * @param style the style to use
   * @return the name formatted according to the given declaration style
   */
  public String formatted(DeclarationStyle style) {
    return style == DeclarationStyle.SHORT ? getSimpleName() : asString();
  }

  @Override
  public String asString() {
    return asText;
  }

  @Override
  public String toString() {
    return '{' + asText + '}';
  }
}
