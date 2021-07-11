package de.ialistannen.javadocapi.storage;

import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.types.JavadocType;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A {@link ElementLoader} that delegates to one or more other APIs and aggregates the results.
 */
public class AggregatedElementLoader implements ElementLoader {

  private final List<ElementLoader> apis;

  public AggregatedElementLoader(List<ElementLoader> apis) {
    this.apis = apis;
  }

  @Override
  public List<LoadResult<JavadocElement>> findAll() {
    return apis.stream()
        .map(ElementLoader::findAll)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  @Override
  public List<LoadResult<JavadocType>> findClassByName(String name) {
    return apis.stream()
        .flatMap(it -> it.findClassByName(name).stream())
        .collect(Collectors.toList());
  }
}
