package de.ialistannen.javadocapi.indexing;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class IndexerConfig {

  private final Set<String> allowedPackages;
  private final List<String> resourcePaths;
  private final String outputPath;
  private final String mavenHome;
  private final List<String> buildFiles;
  private final boolean outputTimings;

  public IndexerConfig(Set<String> allowedPackages, List<String> resourcePaths, String outputPath,
      String mavenHome, List<String> buildFiles, boolean outputTimings) {
    this.allowedPackages = allowedPackages;
    this.resourcePaths = resourcePaths;
    this.outputPath = outputPath;
    this.mavenHome = mavenHome;
    this.buildFiles = buildFiles;
    this.outputTimings = outputTimings;
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

  public boolean isOutputTimings() {
    return outputTimings;
  }

  public List<Path> getBuildFiles() {
    if (buildFiles == null) {
      return Collections.emptyList();
    }
    return buildFiles.stream().map(Path::of).collect(Collectors.toList());
  }
}
