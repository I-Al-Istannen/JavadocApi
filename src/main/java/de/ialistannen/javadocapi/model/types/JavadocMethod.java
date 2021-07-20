package de.ialistannen.javadocapi.model.types;

import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.comment.JavadocComment;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JavadocMethod implements JavadocElement {

  private final List<String> modifiers;
  private final QualifiedName name;
  private final PossiblyGenericType returnType;
  private final List<Parameter> parameters;
  private final List<QualifiedName> thrownTypes;
  private final List<JavadocAnnotation> annotations;
  private final JavadocComment comment;
  private final List<JavadocTypeParameter> typeParameters;

  public JavadocMethod(QualifiedName name, PossiblyGenericType returnType, List<String> modifiers,
      List<Parameter> parameters, List<QualifiedName> thrownTypes,
      List<JavadocAnnotation> annotations, List<JavadocTypeParameter> typeParameters,
      JavadocComment comment) {
    this.name = name;
    this.returnType = returnType;
    this.modifiers = modifiers;
    this.parameters = parameters;
    this.thrownTypes = thrownTypes;
    this.annotations = annotations;
    this.comment = comment;
    this.typeParameters = typeParameters;
  }

  public List<String> getModifiers() {
    return modifiers;
  }

  public PossiblyGenericType getReturnType() {
    return returnType;
  }

  public List<Parameter> getParameters() {
    return Collections.unmodifiableList(parameters);
  }

  public List<JavadocTypeParameter> getTypeParameters() {
    return Collections.unmodifiableList(typeParameters);
  }

  @Override
  public QualifiedName getQualifiedName() {
    return name;
  }

  @Override
  public Optional<JavadocComment> getComment() {
    return Optional.ofNullable(comment);
  }

  public List<QualifiedName> getThrownTypes() {
    return thrownTypes;
  }

  public List<JavadocAnnotation> getAnnotations() {
    return Collections.unmodifiableList(annotations);
  }

  @Override
  public String getDeclaration(DeclarationStyle style) {
    String result = "";

    if (!getAnnotations().isEmpty()) {
      result += getAnnotations().stream()
          .map(annotation -> annotation.getDeclaration(style))
          .collect(Collectors.joining("\n", "", "\n"));
    }

    if (!getModifiers().isEmpty()) {
      result += String.join(" ", getModifiers()) + " ";
    }

    if (!getTypeParameters().isEmpty()) {
      result += getTypeParameters().stream()
          .map(it -> it.getDeclaration(style))
          .collect(Collectors.joining(", ", "<", "> "));
    }

    result += getReturnType().getDeclaration(style) + " ";
    result += getQualifiedName().getSimpleName();

    result += "(";
    result += getParameters().stream()
        .map(it -> it.getType().getDeclaration(style) + " " + it.getName())
        .collect(Collectors.joining(", "));
    result += ")";

    if (!thrownTypes.isEmpty()) {
      result += " throws ";
      result += getThrownTypes().stream()
          .map(it -> it.formatted(style))
          .collect(Collectors.joining(", "));
    }

    return result;
  }

  @Override
  public String toString() {
    return "Method " + name;
  }

  public static class Parameter {

    private final PossiblyGenericType type;
    private final String name;

    public Parameter(PossiblyGenericType type, String name) {
      this.type = type;
      this.name = name;
    }

    public PossiblyGenericType getType() {
      return type;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return type + " " + name;
    }
  }
}
