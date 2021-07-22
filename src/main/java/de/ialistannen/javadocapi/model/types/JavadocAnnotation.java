package de.ialistannen.javadocapi.model.types;

import de.ialistannen.javadocapi.model.JavadocElement.DeclarationStyle;
import de.ialistannen.javadocapi.model.QualifiedName;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class JavadocAnnotation {

  private final QualifiedName type;
  private final Map<String, AnnotationValue> values;

  public JavadocAnnotation(QualifiedName type, Map<String, AnnotationValue> values) {
    this.type = type;
    this.values = values;
  }

  public QualifiedName getType() {
    return type;
  }

  public Map<String, AnnotationValue> getValues() {
    return Collections.unmodifiableMap(values);
  }

  public String getDeclaration(DeclarationStyle style) {
    String result = "@" + type.formatted(style);

    if (!getValues().isEmpty()) {
      result += "(";
      result += getValues().entrySet()
          .stream()
          .map(it -> it.getKey() + " = " + it.getValue().getDeclaration(style))
          .collect(Collectors.joining(", "));
      result += ")";
    }

    return result;
  }

  @Override
  public String toString() {
    return "JavadocAnnotation{" +
        "type=" + type +
        ", values=" + values +
        '}';
  }
}
