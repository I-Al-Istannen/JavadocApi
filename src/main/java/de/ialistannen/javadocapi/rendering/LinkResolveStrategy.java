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

  default String formatNamePart(QualifiedName name) {
    String urlPart;

    if (!name.asString().contains("#")) {
      urlPart = name.asString();
    } else {
      urlPart = name.asString().substring(0, name.asString().indexOf('#'));
    }

    urlPart = urlPart
        .replace(".", "/")
        .replace("$", ".");

    if (name.asString().contains("#")) {
      urlPart += "#" + name.asString().substring(name.asString().indexOf("#") + 1);
    }

    urlPart = urlPart.replace("#", ".html#");
    if (!urlPart.contains("#")) {
      urlPart += ".html";
    }

    return urlPart;
  }
}
