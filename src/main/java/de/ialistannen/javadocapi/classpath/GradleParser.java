package de.ialistannen.javadocapi.classpath;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GradleParser {

  private static final Pattern DEPENDENCY_DECLARATION = Pattern.compile(
      "(api|implementation|compileOnly|runtimeOnly)(\\(platform)?([ (])([\"'])(.+)([\"'])"
  );
  private static final Pattern DEPENDENCY_PARTS = Pattern.compile(
      "([\\w.-]+):([\\w.-]+):?([\\w.$-]+)?(@pom)?"
  );
  private static final Pattern REPOSITORY_DECLARATION = Pattern.compile(
      "maven\\([\"'](.+)[\"']\\)"
  );

  /**
   * Retries a given gradle build file to a {@link Pom}.
   *
   * @param gradleFile the contents of the gradle build file
   * @return the resulting pom
   */
  public Pom parseGradleFile(String gradleFile) {
    Map<Boolean, List<Dependency>> allDependencies = findDependencies(gradleFile).stream()
        .collect(Collectors.partitioningBy(Dependency::isPom));

    return new Pom(
        Set.copyOf(allDependencies.getOrDefault(false, List.of())),
        Set.copyOf(allDependencies.getOrDefault(true, List.of())),
        Set.copyOf(findRepositories(gradleFile))
    );
  }

  private List<Dependency> findDependencies(String fileContent) {
    List<String> dependencyStrings = new ArrayList<>();
    Matcher matcher = DEPENDENCY_DECLARATION.matcher(fileContent);
    while (matcher.find()) {
      dependencyStrings.add(matcher.group(5));
    }

    return dependencyStrings.stream()
        .map(it -> {
          Matcher partMatcher = DEPENDENCY_PARTS.matcher(it);
          if (!partMatcher.find()) {
            throw new IllegalArgumentException("Unknown dependency format '" + it + "'");
          }

          String version = partMatcher.group(3);
          if (version != null && version.startsWith("$")) {
            version = resolveVariable(fileContent, version);
          }

          return new Dependency(
              partMatcher.group(1),
              partMatcher.group(2),
              version,
              partMatcher.group(4) != null || partMatcher.group(2).endsWith("-bom")
          );
        })
        .collect(Collectors.toList());
  }

  private String resolveVariable(String fileContent, String variable) {
    String needle = variable.substring(1) + " = ";
    int index = fileContent.indexOf(needle);

    if (index < 0) {
      throw new IllegalArgumentException("Couldn't find variable declaration");
    }

    String relevantLinePart = fileContent.substring(index, fileContent.indexOf('\n', index));
    Matcher matcher = Pattern.compile("[\"'](.+?)[\"']").matcher(relevantLinePart);

    if (!matcher.find()) {
      throw new IllegalArgumentException(
          "Couldn't extract variable value: " + variable + "\n" + relevantLinePart
      );
    }

    return matcher.group(1);
  }

  private List<Repository> findRepositories(String fileContent) {
    List<Repository> repositories = new ArrayList<>();
    Matcher matcher = REPOSITORY_DECLARATION.matcher(fileContent);

    while (matcher.find()) {
      repositories.add(new Repository(matcher.group(1)));
    }

    return repositories;
  }

}
