package de.ialistannen.javadocapi.indexing;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class IndexerConfig {

  private final Set<String> allowedPackages;
  private final List<String> resourcePaths;
  private final String outputPath;
  private final String mavenHome;
  private final String buildFile;

  public IndexerConfig(Set<String> allowedPackages, List<String> resourcePaths, String outputPath,
      String mavenHome, String buildFile) {
    this.allowedPackages = allowedPackages;
    this.resourcePaths = resourcePaths;
    this.outputPath = outputPath;
    this.mavenHome = mavenHome;
    this.buildFile = buildFile;

    if (buildFile != null && mavenHome == null) {
      throw new IllegalArgumentException("pomFile requires mavenHome");
    }
  }

  public Set<String> getAllowedPackages() {
    return allowedPackages;
  }

  public List<String> getResourcePaths() {
    return resourcePaths;
  }

  public String getOutputPath() {
    return outputPath;
  }

  public Optional<Path> getMavenHome() {
    return Optional.ofNullable(mavenHome).map(Path::of);
  }

  public Optional<Path> getBuildFile() {
    return Optional.ofNullable(buildFile).map(Path::of);
  }
}
