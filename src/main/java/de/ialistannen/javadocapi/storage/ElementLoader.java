package de.ialistannen.javadocapi.storage;

import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.types.JavadocType;
import java.util.List;
import java.util.function.Function;

public interface ElementLoader {

  /**
   * @return all javadoc elements in this storage
   * @throws FetchException if an error occurs
   */
  List<LoadResult<JavadocElement>> findAll();

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
  List<LoadResult<JavadocType>> findClassByName(String name);

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
  }
}
