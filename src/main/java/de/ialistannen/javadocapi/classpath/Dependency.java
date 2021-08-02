package de.ialistannen.javadocapi.classpath;

import java.util.Objects;
import java.util.Optional;

public class Dependency {

  private static final String DEPENDENCY_TEMPLATE = """
      <dependency>
        <groupId> <<GROUP_ID>> </groupId>
        <artifactId> <<ARTIFACT_ID>> </artifactId><<VERSION>><<POM_TYPE_LINE>>
      </dependency>
      """;

  private final String groupId;
  private final String artifactId;
  private final String version;
  private final boolean isPom;

  public Dependency(String groupId, String artifactId, String version, boolean isPom) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.isPom = isPom;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public Optional<String> getVersion() {
    return Optional.ofNullable(version);
  }

  public boolean isPom() {
    return isPom;
  }

  public String format() {
    return DEPENDENCY_TEMPLATE
        .replace(" <<GROUP_ID>> ", getGroupId())
        .replace(" <<ARTIFACT_ID>> ", getArtifactId())
        .replace("<<VERSION>>", version != null ? "\n  <version>" + version + "</version>" : "")
        .replace(
            "<<POM_TYPE_LINE>>",
            isPom()
                ? "\n  <type>pom</type>\n <scope>import</scope>"
                : ""
        );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Dependency that = (Dependency) o;
    return isPom == that.isPom && Objects.equals(groupId, that.groupId) && Objects
        .equals(artifactId, that.artifactId) && Objects.equals(version, that.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, artifactId, version, isPom);
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
