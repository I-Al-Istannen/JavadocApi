package de.ialistannen.javadocapi.storage;

import com.google.gson.Gson;
import de.ialistannen.javadocapi.model.JavadocElement;
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
  public void store(List<JavadocElement> elements, Connection connection) throws SQLException {
    String createTable = "CREATE TABLE IF NOT EXISTS JavadocElements\n"
        + "(\n"
        + "    qualified_name VARCHAR(40) PRIMARY KEY,\n"
        + "    type           VARCHAR(10) NOT NULL,\n"
        + "    data           VARCHAR(100) NOT NULL\n"
        + ");";

    try (PreparedStatement preparedStatement = connection.prepareStatement(createTable)) {
      preparedStatement.execute();
    }

    String insert = "INSERT INTO JavadocElements VALUES (?, ?, ?);";
    try (PreparedStatement statement = connection.prepareStatement(insert)) {

      for (JavadocElement element : elements) {
        statement.setString(1, element.getQualifiedName().asString());
        statement.setString(2, ElementType.fromElement(element).name());
        statement.setString(3, gson.toJson(element));
        statement.addBatch();
      }

      statement.executeBatch();
    }
  }

  public List<JavadocElement> load(Connection connection) throws SQLException {
    List<JavadocElement> elements = new ArrayList<>();

    String query = "SELECT * FROM JavadocElements;";
    try (PreparedStatement statement = connection.prepareStatement(query);
        ResultSet resultSet = statement.executeQuery()) {

      while (resultSet.next()) {
        ElementType type = ElementType.valueOf(resultSet.getString(2));
        String dataString = resultSet.getString(3);
        elements.add(gson.fromJson(dataString, type.getElementClass()));
      }
    }

    return elements;
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
