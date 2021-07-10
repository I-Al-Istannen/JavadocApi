package de.ialistannen.javadocapi.storage;

import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
import java.util.List;

public interface QueryApi {

  /**
   * @return all javadoc elements in this storage
   * @throws FetchException if an error occurs
   */
  List<JavadocElement> findAll();

  /**
   * Searchs for a class by its name. The name can be an arbitrary substring of the name anchored at
   * the end:
   *
   * <ul>
   *   <li>{@code String}</li>
   *   <li>{@code .String}</li>
   *   <li>{@code lang.String}</li>
   *   <li>{@code java.lang.String}</li>
   * </ul>
   *
   * @param name the name of the class you are searching for
   * @return a list with all javadoc elements matching the class filter
   * @throws FetchException if an error occurs
   */
  List<JavadocElement> findClassByName(String name);

  /**
   * Returns all enclosed elements of the type with the given qualified name.
   *
   * @param className the name of the class
   * @return all child elements of the given class
   * @throws FetchException if an error occurs
   */
  List<JavadocElement> findEnclosedElements(QualifiedName className);

  class FetchException extends RuntimeException {

    public FetchException(Throwable cause) {
      super(cause);
    }

    public FetchException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
