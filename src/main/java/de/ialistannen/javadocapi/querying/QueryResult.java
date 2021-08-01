package de.ialistannen.javadocapi.querying;

import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.types.JavadocType;
import de.ialistannen.javadocapi.storage.ElementLoader;
import java.util.Optional;

public interface QueryResult {

  /**
   * @return the qualified name of the result element
   */
  QualifiedName getQualifiedName();

  /**
   * @return the type of this result
   */
  ElementType getType();

  /**
   * Returns the underlying {@link ElementLoader} that produces this result. This might be useful if
   * you aggregate over multiple {@link ElementLoader}s.
   *
   * @return the {@link QueryApi} that supplied this result
   */
  ElementLoader getSourceLoader();

  enum ElementType {
    METHOD,
    FIELD,

    ANNOTATION,
    ENUM,
    INTERFACE,
    CLASS;

    public static Optional<ElementType> fromType(JavadocType.Type type) {
      return switch (type) {
        case ANNOTATION -> Optional.of(ANNOTATION);
        case ENUM -> Optional.of(ENUM);
        case INTERFACE -> Optional.of(INTERFACE);
        case CLASS -> Optional.of(CLASS);
      };
    }
  }
}
