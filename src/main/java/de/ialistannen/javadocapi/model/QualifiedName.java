package de.ialistannen.javadocapi.model;

import de.ialistannen.javadocapi.model.JavadocElement.DeclarationStyle;
import java.util.Objects;
import java.util.Optional;

/**
 * The qualified name of a javadoc element.
 */
public class QualifiedName {

  private final String asText;
  private final String moduleName;

  public QualifiedName(String asText, String moduleName) {
    this.asText = asText;
    this.moduleName = moduleName;
  }

  public QualifiedName(String asText) {
    this(asText, null);
  }

  /**
   * @return the <em>lexical</em> parent, if any
   */
  public Optional<QualifiedName> getLexicalParent() {
    if (asText.contains("#")) {
      return Optional.of(new QualifiedName(
          asText.substring(0, asText.indexOf("#")),
          moduleName
      ));
    }
    if (asText.contains(".")) {
      return Optional.of(new QualifiedName(
          asText.substring(0, indexOfLastSeparator()),
          moduleName
      ));
    }
    return Optional.empty();
  }

  /**
   * Returns the simple name. The simple name of "java.lang.String" is "String" and the simple name
   * of "String#length" is "length".
   *
   * @return the simple name (i.e. without any qualifier)
   */
  public String getSimpleName() {
    if (asText.contains("#")) {
      return asText.substring(asText.indexOf("#") + 1)
          // remove () from methods
          .replaceAll("\\(.*\\)", "");
    }
    return asText.substring(indexOfLastSeparator() + 1);
  }

  private int indexOfLastSeparator() {
    if (asText.endsWith("...")) {
      return asText.lastIndexOf('.', asText.length() - 4);
    }
    return asText.lastIndexOf('.');
  }

  /**
   * @return the module name, if any
   */
  public Optional<String> getModuleName() {
    return Optional.ofNullable(moduleName);
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

  public String asString() {
    return asText;
  }

  public String asStringWithModule() {
    if (moduleName != null) {
      return moduleName + "/" + asString();
    }
    return asString();
  }

  @Override
  public String toString() {
    return '{' + asText + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QualifiedName that = (QualifiedName) o;
    return Objects.equals(asText, that.asText) && Objects.equals(moduleName,
        that.moduleName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(asText, moduleName);
  }
}
