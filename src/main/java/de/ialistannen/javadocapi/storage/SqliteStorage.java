package de.ialistannen.javadocapi.storage;

import com.google.gson.Gson;
import de.ialistannen.javadocapi.model.JavadocElement;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class SqliteStorage extends SqlStorage {

  public SqliteStorage(Gson gson) {
    super(gson);
  }

  public void store(List<JavadocElement> elements, Path file) throws SQLException {
    try (Connection connection = DriverManager.getConnection(buildUrl(file))) {
      super.store(elements, connection);
    }
  }

  public List<JavadocElement> load(Path file) throws SQLException {
    try (Connection connection = DriverManager.getConnection(buildUrl(file))) {
      return super.load(connection);
    }
  }

  private String buildUrl(Path file) {
    return "jdbc:sqlite:" + file.toAbsolutePath();
  }
}
