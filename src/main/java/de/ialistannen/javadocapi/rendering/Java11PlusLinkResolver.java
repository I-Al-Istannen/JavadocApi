package de.ialistannen.javadocapi.rendering;

import de.ialistannen.javadocapi.model.QualifiedName;

public class Java11PlusLinkResolver implements LinkResolveStrategy {

  @Override
  public String resolveLink(QualifiedName name, String baseUrl) {
    if (baseUrl.endsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }

    String urlPart = name.asString()
        .replace(".", "/")
        .replace("$", ".");

    urlPart = urlPart.replace("#", ".html#");
    if (!urlPart.contains("#")) {
      urlPart += ".html";
    }

    return baseUrl + "/" + urlPart;
  }
}
