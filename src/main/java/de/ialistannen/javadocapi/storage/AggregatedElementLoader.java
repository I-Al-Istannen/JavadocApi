package de.ialistannen.javadocapi.storage;

import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
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
  public Collection<LoadResult<JavadocElement>> findAll() {
    return apis.stream()
        .map(ElementLoader::findAll)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  @Override
  public Collection<LoadResult<JavadocType>> findClassByName(String name) {
    return apis.stream()
        .flatMap(it -> it.findClassByName(name).stream())
        .collect(Collectors.toList());
  }

  @Override
  public Collection<LoadResult<JavadocElement>> findElementByName(String name) {
    return apis.stream()
        .flatMap(it -> it.findElementByName(name).stream())
        .collect(Collectors.toList());
  }

  @Override
  public Collection<LoadResult<JavadocElement>> findByQualifiedName(QualifiedName name) {
    return apis.stream()
        .flatMap(loader -> loader.findByQualifiedName(name).stream())
        .collect(Collectors.toList());
  }
}
