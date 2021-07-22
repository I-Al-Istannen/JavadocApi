package de.ialistannen.javadocapi.model.types;

import de.ialistannen.javadocapi.model.JavadocElement.DeclarationStyle;
import de.ialistannen.javadocapi.model.QualifiedName;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public interface AnnotationValue {

  String getDeclaration(DeclarationStyle style);

  class PrimitiveAnnotationValue implements AnnotationValue {

    private final String value;

    public PrimitiveAnnotationValue(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String getDeclaration(DeclarationStyle style) {
      return value;
    }
  }

  class QualifiedAnnotationValue implements AnnotationValue {

    private final QualifiedName name;

    public QualifiedAnnotationValue(QualifiedName name) {
      this.name = name;
    }

    public QualifiedName getName() {
      return name;
    }

    @Override
    public String getDeclaration(DeclarationStyle style) {
      return name.formatted(style);
    }
  }

  class ListAnnotationValue implements AnnotationValue {

    private final List<AnnotationValue> values;

    public ListAnnotationValue(List<AnnotationValue> values) {
      this.values = values;
    }

    public List<AnnotationValue> getValues() {
      return Collections.unmodifiableList(values);
    }

    @Override
    public String getDeclaration(DeclarationStyle style) {
      return "{" +
          values.stream()
              .map(it -> it.getDeclaration(style))
              .collect(Collectors.joining(", ")) + "}";
    }
  }
}
