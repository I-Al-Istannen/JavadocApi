package de.ialistannen.javadocapi.querying;

import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.types.JavadocType;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A {@link QueryApi} that delegates to one or more other APIs and aggregates the results.
 */
public class AggregatedQueryApi implements QueryApi {

  private final List<QueryApi> apis;

  public AggregatedQueryApi(List<QueryApi> apis) {
    this.apis = apis;
  }

  @Override
  public List<JavadocElement> findAll() {
    return apis.stream()
        .map(QueryApi::findAll)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  @Override
  public List<JavadocType> findClassByName(String name) {
    return apis.stream()
        .flatMap(it -> it.findClassByName(name).stream())
        .collect(Collectors.toList());
  }
}
