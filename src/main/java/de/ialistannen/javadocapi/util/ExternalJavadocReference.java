package de.ialistannen.javadocapi.util;

import java.util.Set;

public class ExternalJavadocReference {

  private final String baseUrl;
  private final Set<String> packages;

  public ExternalJavadocReference(String baseUrl, Set<String> packages) {
    this.baseUrl = baseUrl;
    this.packages = Set.copyOf(packages);
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public Set<String> getPackages() {
    return packages;
  }

  @Override
  public String toString() {
    return "ExternalJavadocReference{" +
        "baseUrl='" + baseUrl + '\'' +
        ", packages=" + packages +
        '}';
  }
}
