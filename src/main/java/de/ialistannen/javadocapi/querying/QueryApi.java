package de.ialistannen.javadocapi.querying;

import de.ialistannen.javadocapi.storage.ElementLoader;
import java.util.Collection;
import java.util.List;

public interface QueryApi<T extends QueryResult> {

  /**
   * Finds all elements satisfying the given query.
   *
   * @param loader the loader to use for lookups
   * @param queryString the query as a string
   * @return all elements satisfying the query
   */
  List<T> query(ElementLoader loader, String queryString);

  /**
   * Returns autocompletion suggestions for the given query.
   *
   * @param loader the loader to use for lookups
   * @param prompt the query as a string
   * @return a list with autocomplete suggestions
   */
  Collection<String> autocomplete(ElementLoader loader, String prompt);
}
