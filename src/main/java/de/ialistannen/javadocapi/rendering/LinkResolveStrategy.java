package de.ialistannen.javadocapi.rendering;

import de.ialistannen.javadocapi.model.QualifiedName;

public interface LinkResolveStrategy {

  /**
   * Resolves the link for a given javadoc element.
   *
   * @param name the name of the element to resolve it for
   * @param baseUrl the base url of the loader the element comes from
   * @return the absolute link
   */
  String resolveLink(QualifiedName name, String baseUrl);
}
