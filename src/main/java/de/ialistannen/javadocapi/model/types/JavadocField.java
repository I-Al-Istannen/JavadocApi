package de.ialistannen.javadocapi.model.types;

import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.comment.JavadocComment;
import java.util.List;
import java.util.Optional;

public class JavadocField implements JavadocElement {

  private final List<String> modifiers;
  private final QualifiedName qualifiedName;
  private final PossiblyGenericType type;
  private final JavadocComment comment;

  public JavadocField(QualifiedName qualifiedName, List<String> modifiers, PossiblyGenericType type,
      JavadocComment comment) {
    this.qualifiedName = qualifiedName;
    this.modifiers = modifiers;
    this.type = type;
    this.comment = comment;
  }

  public List<String> getModifiers() {
    return modifiers;
  }

  @Override
  public QualifiedName getQualifiedName() {
    return qualifiedName;
  }

  public PossiblyGenericType getType() {
    return type;
  }

  @Override
  public String getDeclaration(DeclarationStyle style) {
    return type.getDeclaration(style) + " " + qualifiedName.getSimpleName();
  }

  @Override
  public Optional<JavadocComment> getComment() {
    return Optional.ofNullable(comment);
  }

  @Override
  public String toString() {
    return "Field  " + qualifiedName;
  }
}
