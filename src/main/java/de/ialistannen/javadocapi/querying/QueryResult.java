package de.ialistannen.javadocapi.querying;

import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.storage.ElementLoader;

public interface QueryResult {

  /**
   * @return the qualified name of the result element
   */
  QualifiedName getQualifiedName();

  /**
   * Returns the underlying {@link ElementLoader} that produces this result. This might be useful if
   * you aggregate over multiple {@link ElementLoader}s.
   *
   * @return the {@link QueryApi} that supplied this result
   */
  ElementLoader getSourceLoader();
}
