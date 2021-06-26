package de.ialistannen.javadocapi.indexing;

import java.util.List;
import java.util.Set;

public class IndexerConfig {

  private final Set<String> allowedPackages;
  private final List<String> resourcePaths;
  private final String outputPath;

  public IndexerConfig(Set<String> allowedPackages, List<String> resourcePaths, String outputPath) {
    this.allowedPackages = allowedPackages;
    this.resourcePaths = resourcePaths;
    this.outputPath = outputPath;
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
}
