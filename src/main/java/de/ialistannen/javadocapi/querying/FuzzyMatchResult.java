package de.ialistannen.javadocapi.querying;

import de.ialistannen.javadocapi.model.QualifiedName;

public class FuzzyMatchResult {

  private final boolean exact;
  private final QualifiedName qualifiedName;

  public FuzzyMatchResult(boolean exact, QualifiedName qualifiedName) {
    this.exact = exact;
    this.qualifiedName = qualifiedName;
  }

  public boolean isExact() {
    return exact;
  }

  public QualifiedName getQualifiedName() {
    return qualifiedName;
  }

  @Override
  public String toString() {
    if (exact) {
      return "\033[36m" + qualifiedName.asString() + "\033[0m";
    }
    return qualifiedName.asString();
  }
}
