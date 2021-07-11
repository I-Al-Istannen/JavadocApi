package de.ialistannen.javadocapi.querying;

import de.ialistannen.javadocapi.storage.ElementLoader;
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

}
