package de.ialistannen.javadocapi.classpath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GradleToPomRewriter {

  private static final Pattern DEPENDENCY_DECLARATION = Pattern.compile(
      "(api|implementation|compileOnly|runtimeOnly)([ (])([\"'])(.+)([\"'])"
  );
  private static final Pattern DEPENDENCY_PARTS = Pattern.compile(
      "([\\w.-]+):([\\w.-]+):([\\w.]+)(@pom)?"
  );
  private static final Pattern REPOSITORY_DECLARATION = Pattern.compile(
      "maven\\([\"'](.+)[\"']\\)"
  );

  private static final String POM_TEMPLATE = """
      <?xml version="1.0" encoding="UTF-8"?>
      <project xmlns="http://maven.apache.org/POM/4.0.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <modelVersion>4.0.0</modelVersion>
            
        <groupId>de.ialistannen</groupId>
        <artifactId>From-Gradle</artifactId>
        <version>0.0.0</version>
        
        <dependencies>
      <<DEPENDENCIES>>
        </dependencies>
        
        <repositories>
      <<REPOSITORIES>>
        </repositories>
      </project>
      """;
  private static final String DEPENDENCY_TEMPLATE = """
      <dependency>
        <groupId> <<GROUP_ID>> </groupId>
        <artifactId> <<ARTIFACT_ID>> </artifactId>
        <version> <<VERSION>> </version><<POM_TYPE_LINE>>
      </dependency>
      """;
  private static final String REPOSITORY_TEMPLATE = """
      <repository>
        <id> <<ID>> </id>
        <url> <<URL>> </url>
      </repository>
      """;

  public String rewrite(Path gradleBuildFile) throws IOException {
    String contents = Files.readString(gradleBuildFile);
    StringJoiner dependencies = new StringJoiner("\n");
    StringJoiner repositories = new StringJoiner("\n");

    for (Dependency dependency : findDependencies(contents)) {
      dependencies.add(
          DEPENDENCY_TEMPLATE
              .replace(" <<GROUP_ID>> ", dependency.groupId)
              .replace(" <<ARTIFACT_ID>> ", dependency.artifactId)
              .replace(" <<VERSION>> ", dependency.version)
              .replace("<<POM_TYPE_LINE>>", dependency.isPom ? "\n  <type>pom</type>" : "")
      );
    }

    for (Repository repository : findRepositories(contents)) {
      repositories.add(
          REPOSITORY_TEMPLATE
              .replace(" <<URL>> ", repository.url)
              .replace(" <<ID>> ", "id-" + repository.id)
      );
    }

    return POM_TEMPLATE
        .replace("<<DEPENDENCIES>>", dependencies.toString().indent(4))
        .replace("<<REPOSITORIES>>", repositories.toString().indent(4));
  }

  private List<Dependency> findDependencies(String fileContent) {
    List<String> dependencyStrings = new ArrayList<>();
    Matcher matcher = DEPENDENCY_DECLARATION.matcher(fileContent);
    while (matcher.find()) {
      dependencyStrings.add(matcher.group(4));
    }

    return dependencyStrings.stream()
        .map(it -> {
          Matcher partMatcher = DEPENDENCY_PARTS.matcher(it);
          if (!partMatcher.find()) {
            throw new IllegalArgumentException("Unknown dependency format '" + it + "'");
          }

          return new Dependency(
              partMatcher.group(1),
              partMatcher.group(2),
              partMatcher.group(3),
              partMatcher.group(4) != null
          );
        })
        .collect(Collectors.toList());
  }

  private List<Repository> findRepositories(String fileContent) {
    List<Repository> repositories = new ArrayList<>();
    Matcher matcher = REPOSITORY_DECLARATION.matcher(fileContent);

    while (matcher.find()) {
      repositories.add(new Repository(matcher.group(1)));
    }

    return repositories;
  }

  private static class Repository {

    private static final AtomicInteger ids = new AtomicInteger();

    private final String url;
    private final int id;

    private Repository(String url) {
      this.url = url;
      this.id = ids.getAndIncrement();
    }

    @Override
    public String toString() {
      return "Repository{" +
          "url='" + url + '\'' +
          ", id=" + id +
          '}';
    }
  }

  private static class Dependency {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final boolean isPom;

    private Dependency(String groupId, String artifactId, String version, boolean isPom) {
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.version = version;
      this.isPom = isPom;
    }

    @Override
    public String toString() {
      return "Dependency{" +
          "groupId='" + groupId + '\'' +
          ", artifactId='" + artifactId + '\'' +
          ", version='" + version + '\'' +
          ", isPom=" + isPom +
          '}';
    }
  }
}
