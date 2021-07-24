package de.ialistannen.javadocapi.util;

import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.types.JavadocType;
import de.ialistannen.javadocapi.storage.ElementLoader;
import java.util.Collection;
import java.util.stream.Collectors;

public class BaseUrlElementLoader implements ElementLoader {

  private final ElementLoader delegate;
  private final String baseUrl;

  public BaseUrlElementLoader(ElementLoader delegate, String baseUrl) {
    this.delegate = delegate;
    this.baseUrl = baseUrl;
  }

  public String getBaseUrl() {
    return baseUrl;
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
