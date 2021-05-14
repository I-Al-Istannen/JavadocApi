package de.ialistannen.javadocapi.model.types;

import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.comment.JavadocComment;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JavadocType implements JavadocElement {

  private final List<String> modifiers;
  private final QualifiedName name;
  private final List<QualifiedName> members;
  private final JavadocComment comment;
  private final List<JavadocAnnotation> annotations;
  private final List<JavadocTypeParameter> typeParameters;
  private final Type type;
  private final List<PossiblyGenericSupertype> superInterfaces;
  private final PossiblyGenericSupertype superClass;

  public JavadocType(QualifiedName name, List<String> modifiers,
      List<QualifiedName> members, JavadocComment comment, List<JavadocAnnotation> annotations,
      List<JavadocTypeParameter> typeParameters, Type type,
      List<PossiblyGenericSupertype> superInterfaces,
      PossiblyGenericSupertype superClass) {
    this.name = name;
    this.modifiers = modifiers;
    this.members = List.copyOf(members);
    this.comment = comment;
    this.annotations = annotations;
    this.typeParameters = typeParameters;
    this.type = type;
    this.superInterfaces = superInterfaces;
    this.superClass = superClass;
  }

  public List<PossiblyGenericSupertype> getSuperInterfaces() {
    return superInterfaces;
  }

  public PossiblyGenericSupertype getSuperClass() {
    return superClass;
  }

  public List<String> getModifiers() {
    return Collections.unmodifiableList(modifiers);
  }

  public List<QualifiedName> getMembers() {
    return members;
  }

  @Override
  public QualifiedName getQualifiedName() {
    return name;
  }

  @Override
  public Optional<JavadocComment> getComment() {
    return Optional.ofNullable(comment);
  }

  public List<JavadocAnnotation> getAnnotations() {
    return Collections.unmodifiableList(annotations);
  }

  public List<JavadocTypeParameter> getTypeParameters() {
    return typeParameters;
  }

  public Type getType() {
    return type;
  }

  @Override
  public String getDeclaration(DeclarationStyle style) {
    String result = "";

    if (!getAnnotations().isEmpty()) {
      result += getAnnotations().stream()
          .map(it -> it.getDeclaration(style))
          .collect(Collectors.joining("\n"));
      result += "\n";
    }

    result += String.join(" ", getModifiers()) + " ";
    result += getType().getKeyword() + " ";

    result += getQualifiedName().formatted(style);

    if (!getTypeParameters().isEmpty()) {
      result += getTypeParameters().stream()
          .map(it -> it.getDeclaration(style))
          .collect(Collectors.joining(", ", "<", ">"));
    }

    if (getSuperClass() != null) {
      result += " extends " + getSuperClass().getDeclaration(style);
    }

    if (!getSuperInterfaces().isEmpty()) {
      result += " implements " + getSuperInterfaces().stream()
          .map(it -> it.getDeclaration(style))
          .collect(Collectors.joining(", "));
    }

    return result;
  }

  @Override
  public String toString() {
    return type + " " + name;
  }

  public static class PossiblyGenericSupertype {

    private final QualifiedName type;
    private final List<JavadocTypeParameter> parameters;

    public PossiblyGenericSupertype(QualifiedName type, List<JavadocTypeParameter> parameters) {
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

  public enum Type {
    ENUM("enum"),
    CLASS("class"),
    INTERFACE("interface"),
    ANNOTATION("@interface");

    private final String keyword;

    Type(String keyword) {
      this.keyword = keyword;
    }

    public String getKeyword() {
      return keyword;
    }
  }
}
