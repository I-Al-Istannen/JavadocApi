package de.ialistannen.javadocapi.util;

import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.types.JavadocType;
import de.ialistannen.javadocapi.rendering.LinkResolveStrategy;
import de.ialistannen.javadocapi.storage.ElementLoader;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class BaseUrlElementLoader implements ElementLoader {

  private final ElementLoader delegate;
  private final String baseUrl;
  private final List<ExternalJavadocReference> externalJavadocs;
  private final LinkResolveStrategy linkResolveStrategy;

  public BaseUrlElementLoader(ElementLoader delegate, String baseUrl,
      List<ExternalJavadocReference> externalJavadocs, LinkResolveStrategy linkResolveStrategy) {
    this.delegate = delegate;
    this.baseUrl = baseUrl;
    this.externalJavadocs = externalJavadocs;
    this.linkResolveStrategy = new ExternalJavadocAwareLinkResolveStrategy(
        linkResolveStrategy,
        externalJavadocs
    );
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public List<ExternalJavadocReference> getExternalJavadocs() {
    return externalJavadocs;
  }

  public LinkResolveStrategy getLinkResolveStrategy() {
    return linkResolveStrategy;
  }

  @Override
  public Collection<LoadResult<JavadocElement>> findAll() {
    return delegate.findAll().stream().map(it -> it.withLoader(this)).collect(Collectors.toList());
  }

  @Override
  public Collection<LoadResult<JavadocType>> findClassByName(String name) {
    return delegate.findClassByName(name)
        .stream()
        .map(it -> it.withLoader(this))
        .collect(Collectors.toList());
  }

  @Override
  public Collection<LoadResult<JavadocElement>> findElementByName(String name) {
    return delegate.findElementByName(name)
        .stream()
        .map(it -> it.withLoader(this))
        .collect(Collectors.toList());
  }

  @Override
  public Collection<LoadResult<JavadocElement>> findByQualifiedName(QualifiedName qualifiedName) {
    return delegate.findByQualifiedName(qualifiedName)
        .stream()
        .map(it -> it.withLoader(this))
        .collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

}
