package de.ialistannen.javadocapi.classpath;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class Pom {

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
        
        <dependencyManagement>
          <dependencies>
        <<DEPENDENCY_MANAGEMENT>>
          </dependencies>
        </dependencyManagement>
      </project>
      """;

  private final Set<Dependency> dependencies;
  private final Set<Dependency> dependencyManagement;
  private final Set<Repository> repositories;

  public Pom(Set<Dependency> dependencies, Set<Dependency> dependencyManagement,
      Set<Repository> repositories) {
    this.dependencies = Set.copyOf(dependencies);
    this.dependencyManagement = dependencyManagement;
    this.repositories = Set.copyOf(repositories);
  }

  public Set<Dependency> getDependencies() {
    return dependencies;
  }

  public Set<Repository> getRepositories() {
    return repositories;
  }

  public Set<Dependency> getDependencyManagement() {
    return dependencyManagement;
  }

  public Pom merge(Pom other) {
    return new Pom(
        union(dependencies, other.dependencies),
        union(dependencyManagement, other.dependencyManagement),
        union(repositories, other.repositories)
    );
  }

  public String format() {
    StringJoiner repositoriesString = new StringJoiner("\n");
    String dependenciesString = dependencies.stream()
        .map(Dependency::format)
        .collect(Collectors.joining("\n"));
    String dependencyManagementString = dependencyManagement.stream()
        .map(Dependency::format)
        .collect(Collectors.joining("\n"));

    int currentRepoId = 0;
    for (Repository repository : repositories) {
      repositoriesString.add(repository.format("id-" + ++currentRepoId));
    }

    return POM_TEMPLATE
        .replace("<<DEPENDENCIES>>", dependenciesString.indent(4))
        .replace("<<DEPENDENCY_MANAGEMENT>>", dependencyManagementString.indent(4))
        .replace("<<REPOSITORIES>>", repositoriesString.toString().indent(4));
  }

  private <T> Set<T> union(Set<T> first, Set<T> second) {
    Set<T> result = new HashSet<>(first);
    result.addAll(first);
    result.addAll(second);
    return result;
  }
}
