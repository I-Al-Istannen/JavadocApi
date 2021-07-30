package de.ialistannen.javadocapi.spoon.filtering;

import static de.ialistannen.javadocapi.spoon.filtering.FluentFilter.forType;

import java.util.Set;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtModule;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;

public class IndexerFilterChain {

  private final Set<String> packageWhitelist;

  public IndexerFilterChain(Set<String> packageWhitelist) {
    this.packageWhitelist = Set.copyOf(packageWhitelist);
  }

  public FluentFilter asFilter() {
    FluentFilter filter = element -> false;
    filter = filter.or(forType(CtType.class, CtModifiable::isPublic));
    filter = filter.or(forType(CtField.class, it -> it.isPublic() || it.isProtected()));
    filter = filter.or(forType(CtConstructor.class, it -> it.isProtected() || it.isPublic()));
    filter = filter.or(forType(CtMethod.class, it -> it.isProtected() || it.isPublic()));

    filter = filter.or(forType(CtPackage.class, this::includePackage));

    filter = filter.or(forType(CtModule.class, ignored -> true));

    return filter;
  }

  private boolean includePackage(CtPackage it) {
    if (packageWhitelist.contains("*")) {
      return true;
    }

    if (packageWhitelist.contains(it.getQualifiedName())) {
      return true;
    }

    for (String allowedPackage : packageWhitelist) {
      if (allowedPackage.startsWith(it.getQualifiedName())) {
        return true;
      }
    }
    return false;
  }
}
