package de.ialistannen.javadocapi.classpath;

import java.util.Objects;

public class Repository {

  private static final String REPOSITORY_TEMPLATE = """
      <repository>
        <id> <<ID>> </id>
        <url> <<URL>> </url>
      </repository>
      """;

  private final String url;

  public Repository(String url) {
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  public String format(String id) {
    return REPOSITORY_TEMPLATE
        .replace(" <<URL>> ", getUrl())
        .replace(" <<ID>> ", id);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Repository that = (Repository) o;
    return Objects.equals(url, that.url);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url);
  }

  @Override
  public String toString() {
    return "Repository{" +
        "url='" + url + '\'' +
        '}';
  }
}
