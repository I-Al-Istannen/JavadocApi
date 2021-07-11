package de.ialistannen.javadocapi.querying;

import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.storage.ElementLoader;

public class FuzzyQueryResult implements QueryResult {

  private final boolean exact;
  private final QualifiedName qualifiedName;
  private final ElementLoader loader;

  public FuzzyQueryResult(boolean exact, QualifiedName qualifiedName, ElementLoader loader) {
    this.exact = exact;
    this.qualifiedName = qualifiedName;
    this.loader = loader;
  }

  public boolean isExact() {
    return exact;
  }

  @Override
  public QualifiedName getQualifiedName() {
    return qualifiedName;
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
}
