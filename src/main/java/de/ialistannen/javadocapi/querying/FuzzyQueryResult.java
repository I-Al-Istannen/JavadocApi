package de.ialistannen.javadocapi.querying;

import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.storage.ElementLoader;
import java.util.Objects;

public class FuzzyQueryResult implements QueryResult {

  private final boolean exact;
  private final QualifiedName qualifiedName;
  private final ElementLoader loader;
  private final ElementType type;

  public FuzzyQueryResult(boolean exact, QualifiedName qualifiedName, ElementLoader loader,
      ElementType type) {
    this.exact = exact;
    this.qualifiedName = qualifiedName;
    this.loader = loader;
    this.type = type;
  }

  public boolean isExact() {
    return exact;
  }

  @Override
  public QualifiedName getQualifiedName() {
    return qualifiedName;
  }

  @Override
  public ElementType getType() {
    return type;
  }

  @Override
  public ElementLoader getSourceLoader() {
    return loader;
  }

  @Override
  public String toString() {
    if (exact) {
      return "\033[36m" + qualifiedName.asString() + "\033[0m";
    }
    return qualifiedName.asString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FuzzyQueryResult that = (FuzzyQueryResult) o;
    return exact == that.exact && Objects.equals(qualifiedName, that.qualifiedName)
        && Objects.equals(loader, that.loader) && type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(exact, qualifiedName, loader, type);
  }
}
