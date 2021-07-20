package de.ialistannen.javadocapi.model.types;

import de.ialistannen.javadocapi.model.JavadocElement.DeclarationStyle;
import de.ialistannen.javadocapi.model.QualifiedName;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A type that might just wrap a {@link QualifiedName} or wrap it and provide additionally type
 * parameters.
 */
public class PossiblyGenericType {

  private final QualifiedName type;
  private final List<JavadocTypeParameter> parameters;

  public PossiblyGenericType(QualifiedName type, List<JavadocTypeParameter> parameters) {
    this.type = type;
    this.parameters = parameters;
  }

  public QualifiedName getType() {
    return type;
  }

  public List<JavadocTypeParameter> getParameters() {
    return parameters;
  }

  public String getDeclaration(DeclarationStyle style) {
    String result = getType().formatted(style);

    if (!getParameters().isEmpty()) {
      result += getParameters().stream()
          .map(it -> it.getDeclaration(style))
          .collect(Collectors.joining(", ", "<", ">"));
    }

    return result;
  }
}
