package de.ialistannen.javadocapi.storage;

import static org.apache.commons.lang3.StringUtils.reverse;

import com.google.gson.Gson;
import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.types.JavadocField;
import de.ialistannen.javadocapi.model.types.JavadocMethod;
import de.ialistannen.javadocapi.model.types.JavadocType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SqlStorage {

  private final Gson gson;

  public SqlStorage(Gson gson) {
    this.gson = gson;
  }

  /**
   * Stores the given list of elements in a "JavadocElements table in the given Connection.
   *
   * @param elements the elements to save
   * @param connection the connection to use
   * @throws SQLException if an error occurred
   */
  protected void addAll(List<JavadocElement> elements, Connection connection) throws SQLException {
    String createTable = "CREATE TABLE IF NOT EXISTS JavadocElements\n"
        + "(\n"
        + "    reversed_qualified_name VARCHAR(40),\n"
        + "    qualified_name          VARCHAR(40) PRIMARY KEY,\n"
        + "    type                    VARCHAR(10) NOT NULL,\n"
        + "    data                    VARCHAR(100) NOT NULL\n"
        + ");"
        + "CREATE UNIQUE INDEX `REV_QUALIFIED_INDEX`\n"
        + "  ON JavadocElements (reversed_qualified_name COLLATE NOCASE)";

    try (PreparedStatement preparedStatement = connection.prepareStatement(createTable)) {
      preparedStatement.execute();
    }

    String insert = "INSERT INTO JavadocElements VALUES (?, ?, ?, ?);";
    try (PreparedStatement statement = connection.prepareStatement(insert)) {
      connection.setAutoCommit(false);

      for (int i = 0; i < elements.size(); i++) {
        JavadocElement element = elements.get(i);
        statement.setString(1, reverse(element.getQualifiedName().asString()));
        statement.setString(2, element.getQualifiedName().asString());
        statement.setString(3, ElementType.fromElement(element).name());
        statement.setString(4, gson.toJson(element));
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
  }

  protected List<JavadocElement> findAll(Connection connection) throws SQLException {
    String query = "SELECT * FROM JavadocElements;";
    try (PreparedStatement statement = connection.prepareStatement(query);
        ResultSet resultSet = statement.executeQuery()) {

      return parseResults(resultSet);
    }
  }

  private List<JavadocElement> parseResults(ResultSet resultSet) throws SQLException {
    List<JavadocElement> elements = new ArrayList<>();

    while (resultSet.next()) {
      ElementType type = ElementType.valueOf(resultSet.getString("type"));
      String dataString = resultSet.getString("data");
      elements.add(gson.fromJson(dataString, type.getElementClass()));
    }
    return elements;
  }

  protected List<JavadocType> findClassByName(Connection connection, String name)
      throws SQLException {
    String query = "SELECT * "
        + "FROM JavadocElements\n"
        + "WHERE reversed_qualified_name LIKE ? AND qualified_name NOT LIKE '%#%'";
    try (PreparedStatement statement = connection.prepareStatement(query)) {
      statement.setString(1, reverse(name) + "%");
      try (ResultSet resultSet = statement.executeQuery()) {
        return parseResults(resultSet)
            .stream()
            .filter(it -> it instanceof JavadocType)
            .map(it -> (JavadocType) it)
            .collect(Collectors.toList());
      }
    }
  }

  protected List<JavadocElement> findByQualifiedName(Connection connection, QualifiedName name)
      throws SQLException {
    String query = "SELECT * "
        + "FROM JavadocElements\n"
        + "WHERE qualified_name = ?";
    try (PreparedStatement statement = connection.prepareStatement(query)) {
      statement.setString(1, name.asString());
      try (ResultSet resultSet = statement.executeQuery()) {
        return parseResults(resultSet);
      }
    }
  }

  private enum ElementType {
    FIELD(JavadocField.class),
    METHOD(JavadocMethod.class),
    TYPE(JavadocType.class);

    private final Class<? extends JavadocElement> elementClass;

    ElementType(Class<? extends JavadocElement> elementClass) {
      this.elementClass = elementClass;
    }

    public Class<? extends JavadocElement> getElementClass() {
      return elementClass;
    }

    public static ElementType fromElement(JavadocElement element) {
      return Arrays.stream(values())
          .filter(it -> it.elementClass.isInstance(element))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Unknown element type " + element));
    }
  }
}
