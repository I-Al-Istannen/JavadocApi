package de.ialistannen.javadocapi.storage;

import com.google.gson.Gson;
import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.types.JavadocType;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
      String createFtsTable = """
          CREATE VIRTUAL TABLE Completions USING fts5
          (
              qualified_name,
              priority
          );""";

      try (PreparedStatement preparedStatement = connection.prepareStatement(createFtsTable)) {
        preparedStatement.execute();
      }

      String insert = "INSERT INTO Completions VALUES (?, ?);";
      try (PreparedStatement statement = connection.prepareStatement(insert)) {
        connection.setAutoCommit(false);

        for (int i = 0; i < elements.size(); i++) {
          JavadocElement element = elements.get(i);
          String fullName = element.getQualifiedName().asStringWithModule();
          statement.setString(1, fullName);
          statement.setInt(2, element instanceof JavadocType ? 10 : 0);
          statement.addBatch();
          if (i % 1000 == 0) {
            statement.executeBatch();
            connection.commit();
          }
        }

        statement.executeBatch();
        connection.commit();
        connection.setAutoCommit(true);
      }

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
  public Collection<LoadResult<JavadocElement>> findElementByName(String name) {
    return withConnection(connection -> super.findElementByName(connection, name)
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

  @Override
  public Collection<String> autocomplete(String prompt) {
    return withConnection(connection -> {
      String sanitizedPrompt = '"' + prompt.replace("\"", "\"\"") + '"';

      String query = """
          SELECT *
          FROM Completions
          WHERE qualified_name MATCH ? AND rank MATCH 'bm25(10.0, 5.0)'
          ORDER BY priority DESC, rank DESC;""";

      try (PreparedStatement statement = connection.prepareStatement(query)) {
        statement.setString(1, sanitizedPrompt);
        try (ResultSet resultSet = statement.executeQuery()) {
          List<String> results = new ArrayList<>();
          while (resultSet.next()) {
            results.add(resultSet.getString("qualified_name"));
          }
          return results;
        }
      }
    });
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
    return "sqlite:" + file.getFileName();
  }

  private interface SqlCallable<T> {

    T get(Connection connection) throws SQLException;
  }

}
