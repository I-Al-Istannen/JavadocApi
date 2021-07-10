package de.ialistannen.javadocapi.storage;

import com.google.gson.Gson;
import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class SqliteStorage extends SqlStorage implements QueryApi {

  private final Path file;

  public SqliteStorage(Gson gson, Path file) {
    super(gson);
    this.file = file;
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
  public List<JavadocElement> findAll() {
    return withConnection(super::findALl);
  }

  @Override
  public List<JavadocElement> findClassByName(String name) {
    return withConnection(connection -> super.findClassByName(connection, name));
  }

  @Override
  public List<JavadocElement> findEnclosedElements(QualifiedName className) {
    return withConnection(connection -> super.findEnclosedElements(connection, className.asString()));
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

  private interface SqlCallable<T> {

    T get(Connection connection) throws SQLException;
  }

}
