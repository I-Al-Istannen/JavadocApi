package de.ialistannen.javadocapi.util;

import static java.util.stream.Collectors.toMap;

import de.ialistannen.javadocapi.model.QualifiedName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class NameShortener {

  /**
   * Shortens the qualified names as much as possible while still ensuring they are unique.
   *
   * @param names the names to shorten
   * @return a map from original to shortened name
   */
  public Map<String, String> shortenMatches(Set<QualifiedName> names) {
    Map<String, String> typeFixpoints = findFixpoint(
        names.stream()
            .map(QualifiedName::asString)
            .collect(Collectors.toSet())
    );

    Set<String> allParameterTypes = typeFixpoints.values().stream()
        .map(this::getAllParameterTypes)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());

    Map<String, String> parameterFixpoint = findFixpoint(allParameterTypes);

    return typeFixpoints.entrySet().stream()
        .map(entry -> {
          String original = entry.getKey();
          String replaced = mapParameters(entry.getValue(), parameterFixpoint);
          return Map.entry(original, replaced);
        })
        .collect(toMap(Entry::getKey, Entry::getValue));
  }

  private Map<String, String> findFixpoint(Set<String> inputTypes) {
    Map<String, String> currentTypes = inputTypes.stream().collect(toMap(it -> it, it -> it));

    while (true) {
      Map<String, String> iteration = typeIteration(currentTypes);
      if (iteration.equals(currentTypes)) {
        break;
      }
      currentTypes = iteration;
    }

    return currentTypes;
  }

  private Map<String, String> typeIteration(Map<String, String> names) {
    Map<String, List<Map.Entry<String, String>>> types = names.entrySet().stream()
        .collect(
            toMap(
                it -> stripFirstNesting(getType(it.getValue())),
                it -> new ArrayList<>(List.of(
                    Map.entry(
                        it.getKey(),
                        it.getValue()
                    )
                )),
                (a, b) -> {
                  a.addAll(b);
                  return a;
                }
            )
        );

    Map<String, String> oldToNew = new HashMap<>();

    for (var entry : types.entrySet()) {
      // If we have "java.lang.String#foo()" and "java.lang.String#bar()", we still want to shorten
      // them. This is what this stream here checks - if there is only one distinct type
      // (two times java.lang.String in the example), count them as one.
      int uniqueEntriesInGroup = (int) entry.getValue()
          .stream()
          .map(Entry::getKey)
          .map(this::getType)
          .distinct()
          .count();
      if (uniqueEntriesInGroup == 1) {
        // Each entry has a mapping: Original Type -> Stripped Type with method/field name and type!
        for (var type : entry.getValue()) {
          oldToNew.put(type.getKey(), stripFirstNesting(type.getValue()));
        }
      } else {
        // Each entry has a mapping: Original Type -> Stripped Type with method/field name and type!
        for (var name : entry.getValue()) {
          oldToNew.put(name.getKey(), name.getValue());
        }
      }
    }

    return oldToNew;
  }

  private String mapParameters(String type, Map<String, String> parameterReplacements) {
    if (!type.contains("(")) {
      return type;
    }
    String base = type.substring(0, type.indexOf("("));

    String parameters = getAllParameterTypes(type).stream()
        .map(parameterReplacements::get)
        .collect(Collectors.joining(","));

    return base + "(" + parameters + ")";
  }

  private List<String> getAllParameterTypes(String input) {
    if (!input.contains("(")) {
      return List.of();
    }
    String parameterSection = input.substring(input.indexOf("(") + 1, input.indexOf(")"));

    return Arrays.stream(parameterSection.split(","))
        .map(String::strip)
        .collect(Collectors.toList());
  }

  private String stripFirstNesting(String input) {
    // Hey#foo(java.lang.String)
    if (input.contains("#") && input.indexOf('.') > input.indexOf('#')) {
      return input;
    }

    if (!input.contains(".")) {
      return input;
    }

    return input.substring(input.indexOf('.') + 1);
  }

  private String getType(String name) {
    if (name.contains("#")) {
      return name.substring(0, name.indexOf("#"));
    }
    return name;
  }
}
