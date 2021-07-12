package de.ialistannen.javadocapi.storage;

import com.google.gson.Gson;
import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.types.JavadocType;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SqliteStorage extends SqlStorage implements ElementLoader {

  private final Path file;

  public SqliteStorage(Gson gson, Path file) {
    super(gson);
    this.file = file;
  }

  /**
   * @return the loaded file
   */
  public Path getFile() {
    return file;
  }

  /**
   * Adds the given elements to this storage, writing it out to disk.
   *
   * @param elements the elements to add
   */
  public void addAll(List<JavadocElement> elements) {
    withConnection(connection -> {
      super.addAll(elements, connection);

      return null;
    });
  }

  @Override
  public Collection<LoadResult<JavadocElement>> findAll() {
    return withConnection(connection -> super.findAll(connection)
        .stream()
        .map(element -> new LoadResult<>(element, this))
        .collect(Collectors.toList()));
  }

  @Override
  public Collection<LoadResult<JavadocType>> findClassByName(String name) {
    return withConnection(connection -> super.findClassByName(connection, name)
        .stream()
        .map(element -> new LoadResult<>(element, this))
        .collect(Collectors.toList()));
  }

  @Override
  public Collection<LoadResult<JavadocElement>> findByQualifiedName(QualifiedName name) {
    return withConnection(connection -> super.findByQualifiedName(connection, name))
        .stream()
        .map(element -> new LoadResult<>(element, this))
        .collect(Collectors.toList());
  }

  private <T> T withConnection(SqlCallable<T> callable) {
    try (Connection connection = DriverManager.getConnection(buildUrl(file))) {
      return callable.get(connection);
    } catch (SQLException e) {
      throw new FetchException(e);
    }
  }

  private String buildUrl(Path file) {
    return "jdbc:sqlite:" + file.toAbsolutePath();
  }

  @Override
  public String toString() {
    return "sqlite:" + file;
  }

  private interface SqlCallable<T> {

    T get(Connection connection) throws SQLException;
  }

}
