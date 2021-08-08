package de.ialistannen.javadocapi.util;

import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.rendering.LinkResolveStrategy;
import java.util.List;

public class ExternalJavadocAwareLinkResolveStrategy implements LinkResolveStrategy {

  private final LinkResolveStrategy underlying;
  private final List<ExternalJavadocReference> externalJavadocReferences;

  public ExternalJavadocAwareLinkResolveStrategy(LinkResolveStrategy underlying,
      List<ExternalJavadocReference> externalJavadocReferences) {
    this.underlying = underlying;
    this.externalJavadocReferences = List.copyOf(externalJavadocReferences);
  }

  @Override
  public String resolveLink(QualifiedName name, String baseUrl) {
    String nameAsString = name.asString();
    String packageName;

    if (nameAsString.contains("#")) {
      packageName = name.getLexicalParent()
          .flatMap(QualifiedName::getLexicalParent)
          .map(QualifiedName::asString)
          .orElse("nope");
    } else {
      packageName = name.getLexicalParent().map(QualifiedName::asString).orElse("nope");
    }

    for (ExternalJavadocReference reference : externalJavadocReferences) {
      if (reference.getPackages().contains(packageName)) {
        return underlying.resolveLink(name, reference.getBaseUrl());
      }
    }

    return underlying.resolveLink(name, baseUrl);
  }

  @Override
  public String formatNamePart(QualifiedName name) {
    return underlying.formatNamePart(name);
  }
}
