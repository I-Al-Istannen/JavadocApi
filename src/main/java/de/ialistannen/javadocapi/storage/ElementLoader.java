package de.ialistannen.javadocapi.storage;

import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.types.JavadocType;
import java.util.Collection;
import java.util.function.Function;

public interface ElementLoader {

  /**
   * @return all javadoc elements in this storage
   * @throws FetchException if an error occurs
   */
  Collection<LoadResult<JavadocElement>> findAll();

  /**
   * Searchs for a class by its name. This is a specialized version of {@link
   * #findElementByName(String)}.
   *
   * @param name the name of the class you are searching for
   * @return a list with all javadoc elements matching the class filter
   * @throws FetchException if an error occurs
   * @see #findElementByName(String)
   */
  Collection<LoadResult<JavadocType>> findClassByName(String name);

  /**
   * Searchs for an element by its name. The name can be an arbitrary substring of the name anchored
   * at the end:
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
  Collection<LoadResult<JavadocElement>> findElementByName(String name);

  /**
   * Finds an element by its qualified name. This might not be unique if multiple loaders have the
   * same element, so a List is returned.
   *
   * @param name the name of the element
   * @return the element, if found
   */
  Collection<LoadResult<JavadocElement>> findByQualifiedName(QualifiedName name);

  /**
   * Tries to autocomplete a given prompt.
   *
   * @param prompt the prompt to complete
   * @return completions
   */
  Collection<String> autocomplete(String prompt);

  class FetchException extends RuntimeException {

    public FetchException(Throwable cause) {
      super(cause);
    }
  }

  class LoadResult<T> {

    private final T result;
    private final ElementLoader loader;

    public LoadResult(T result, ElementLoader loader) {
      this.result = result;
      this.loader = loader;
    }

    public T getResult() {
      return result;
    }

    public ElementLoader getLoader() {
      return loader;
    }

    public <R> LoadResult<R> map(Function<T, R> mapper) {
      return new LoadResult<>(mapper.apply(getResult()), getLoader());
    }

    public LoadResult<T> withLoader(ElementLoader loader) {
      return new LoadResult<>(result, loader);
    }
  }
}
