package de.ialistannen.javadocapi.spoon.filtering;

import java.util.function.Predicate;
import spoon.reflect.declaration.CtElement;

public interface FluentFilter {

  boolean keep(CtElement element);

  default <R extends CtElement> FluentFilter narrow(Class<R> clazz, Predicate<R> test) {
    return element -> {
      if (!this.keep(element)) {
        return false;
      }
      if (!clazz.isInstance(element)) {
        return false;
      }
      return test.test(clazz.cast(element));
    };
  }

  default FluentFilter and(FluentFilter other) {
    return element -> this.keep(element) && other.keep(element);
  }

  default FluentFilter or(FluentFilter other) {
    return element -> this.keep(element) || other.keep(element);
  }

  static <R extends CtElement> FluentFilter forType(Class<R> clazz, Predicate<R> test) {
    return element -> {
      if (!clazz.isInstance(element)) {
        return false;
      }
      return test.test(clazz.cast(element));
    };
  }
}
