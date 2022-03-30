package de.ialistannen.javadocapi.querying;

import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.types.JavadocType;
import de.ialistannen.javadocapi.querying.QueryResult.ElementType;
import de.ialistannen.javadocapi.storage.ElementLoader;
import de.ialistannen.javadocapi.storage.ElementLoader.LoadResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FuzzyElementQuery implements QueryApi<FuzzyQueryResult> {

  @Override
  public Collection<String> autocomplete(ElementLoader loader, String prompt) {
    return loader.autocomplete(prompt);
  }

  @Override
  public List<FuzzyQueryResult> query(ElementLoader queryApi, String queryString) {
    Query query = Query.fromString(queryString);

    if (query == null) {
      return List.of();
    }

    if (query.getClassName() == null) {
      return findNonClassElementByName(queryApi, query);
    }

    return findElementsFromClass(queryApi, query);
  }

  private List<FuzzyQueryResult> findNonClassElementByName(ElementLoader queryApi, Query query) {
    Query normalizedQuery = query.normalized();
    Collection<LoadResult<QualifiedName>> potentialElements = queryApi
        .findElementByName(normalizedQuery.getElementName())
        .stream()
        .filter(it -> !(it.getResult() instanceof JavadocType))
        .map(it -> it.map(JavadocElement::getQualifiedName))
        .collect(Collectors.toList());

    if (normalizedQuery.getParameters() == null) {
      return potentialElements.stream()
          .map(it -> toResult(
              query,
              it,
              it.getResult().isMethod() ? ElementType.METHOD : ElementType.FIELD)
          )
          .collect(Collectors.toList());
    }

    return potentialElements.stream()
        .filter(it -> fuzzyMatchParameters(normalizedQuery.getParameters(), it.getResult()))
        .map(it -> toResult(query, it, ElementType.METHOD))
        .collect(Collectors.toList());
  }

  private List<FuzzyQueryResult> findElementsFromClass(ElementLoader queryApi, Query query) {
    Query normalizedQuery = query.normalized();
    Collection<LoadResult<JavadocType>> potentialClasses = queryApi.findClassByName(
        normalizedQuery.getClassName()
    );

    if (normalizedQuery.getElementName() == null) {
      return potentialClasses
          .stream()
          .map(it -> toResult(
              query,
              it.map(JavadocType::getQualifiedName),
              ElementType.fromType(it.getResult().getType()).orElseThrow()
          ))
          .collect(Collectors.toList());
    }

    List<LoadResult<QualifiedName>> results = new ArrayList<>();

    for (var potentialClass : potentialClasses) {
      var enclosed = potentialClass.getResult().getMembers()
          .stream()
          .map(it -> new LoadResult<>(it, potentialClass.getLoader()))
          .filter(
              it -> it.getResult()
                  .getSimpleName()
                  .toUpperCase(Locale.ROOT)
                  .endsWith(normalizedQuery.getElementName())
          )
          .filter(it -> {
            // Filter out methods if user specified a "(", ignore it otherwise
            if (normalizedQuery.getParameters() != null) {
              return it.getResult().isMethod();
            }
            return true;
          })
          .collect(Collectors.toList());

      if (normalizedQuery.getParameters() == null) {
        results.addAll(enclosed);
        continue;
      }

      enclosed.stream()
          .filter(it -> fuzzyMatchParameters(normalizedQuery.getParameters(), it.getResult()))
          .forEach(results::add);
    }

    return results
        .stream()
        .map(name -> toResult(
            query,
            name,
            name.getResult().isMethod() ? ElementType.METHOD : ElementType.FIELD
        ))
        .collect(Collectors.toList());
  }

  private static boolean fuzzyMatchParameters(List<String> query, QualifiedName actual) {
    List<String> actualParameterTypes = Query
        .fromString(actual.asString().toUpperCase(Locale.ROOT))
        .getParameters();

    if (actualParameterTypes == null || query.size() > actualParameterTypes.size()) {
      return false;
    }
    for (int i = 0; i < query.size(); i++) {
      String queryParam = query.get(i);
      String actualParam = actualParameterTypes.get(i);

      if (!actualParam.endsWith(queryParam)) {
        return false;
      }
    }

    return true;
  }

  private FuzzyQueryResult toResult(
      Query query,
      LoadResult<QualifiedName> element,
      ElementType type
  ) {
    Query elementQuery = Query.fromString(element.getResult().asString());
    Query normalizedElementQuery = elementQuery.normalized();
    Query normalizedQuery = query.normalized();

    boolean exact = normalizedQuery.exactToReference(normalizedElementQuery);
    boolean caseSensitiveExact = query.exactToReference(elementQuery);

    return new FuzzyQueryResult(
        exact,
        caseSensitiveExact,
        element.getResult(),
        element.getLoader(),
        type
    );
  }

  private static class Query {

    private static final Pattern CONSTRUCTOR_PATTERN = Pattern.compile(
        "^([\\w.$]+)(\\(.*\\)?)$", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern FIELD_PATTERN = Pattern.compile(
        "^([\\w.$]+)#([\\w$]+|<INIT>)$", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "^([\\w.$]+)#([\\w$]+|<INIT>)\\((.*)\\)?$", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PARAMETER_PATTERN = Pattern.compile(
        "([\\w.$\\[\\]]+)( [\\w$]+)?(, *)?", Pattern.CASE_INSENSITIVE
    );

    private final String className;
    private final String elementName;
    private final List<String> parameters;

    private Query(String className, String elementName, List<String> parameters) {
      this.className = className;
      this.elementName = elementName;
      this.parameters = parameters;
    }

    public String getClassName() {
      return className;
    }

    public String getElementName() {
      return elementName;
    }

    public List<String> getParameters() {
      return parameters;
    }

    public static Query fromString(String queryString) {
      String query = queryString.strip().replaceAll("\\s+", " ");

      if (query.startsWith("#")) {
        query = query.substring(1);
        if (!query.contains("(")) {
          return new Query(null, query, null);
        }
        String methodName = query.substring(0, query.indexOf("("));
        String parameterString = query.substring(methodName.length())
            .replace("(", "")
            .replace(")", "");

        return new Query(null, methodName, extractParameters(parameterString));
      }

      if (query.matches("^[\\w.$]+$")) {
        return new Query(query, null, null);
      }
      Matcher matcher = FIELD_PATTERN.matcher(query);
      if (matcher.matches()) {
        String className = matcher.group(1).strip();
        String fieldName = matcher.group(2).strip();

        fieldName = adjustForConstructor(fieldName, className);

        return new Query(className, fieldName, null);
      }

      // Shorthand: String(String other) for String#String(String other)
      matcher = CONSTRUCTOR_PATTERN.matcher(query);
      if (matcher.matches()) {
        String classname = matcher.group(1);
        String rest = matcher.group(2);
        query = classname
            + "#"
            + classname.substring(Math.max(0, classname.lastIndexOf('.') + 1))
            + rest;
      }

      matcher = METHOD_PATTERN.matcher(query);
      if (!matcher.matches()) {
        return null;
      }
      String className = matcher.group(1).strip();
      String methodName = matcher.group(2).strip();
      String parameterString = matcher.group(3).strip();

      methodName = adjustForConstructor(methodName, className);

      return new Query(className, methodName, extractParameters(parameterString));
    }

    private static String adjustForConstructor(String fieldName, String className) {
      String simpleClassname = new QualifiedName(className).getSimpleName();
      if (fieldName.toLowerCase(Locale.ROOT).equals(simpleClassname.toLowerCase(Locale.ROOT))) {
        fieldName = "<init>".toUpperCase(Locale.ROOT);
      }
      return fieldName;
    }

    private static List<String> extractParameters(String parameterString) {
      if (parameterString.isEmpty()) {
        return List.of();
      }
      Matcher matcher = PARAMETER_PATTERN.matcher(parameterString);
      List<String> parameters = new ArrayList<>();

      while (matcher.find()) {
        parameters.add(matcher.group(1).strip());
      }

      return parameters;
    }

    public boolean exactToReference(Query reference) {
      if ((getParameters() == null) != (reference.getParameters() == null)) {
        return false;
      }
      if ((getElementName() == null) != (reference.getElementName() == null)) {
        return false;
      }
      if (getClassName() != null) {
        if (!classMatchWithReference(getClassName(), reference.getClassName())) {
          return false;
        }
      }

      if (getElementName() == null) {
        return true;
      }
      if (!reference.getElementName().equals(getElementName())) {
        return false;
      }

      if (getParameters() == null) {
        return true;
      }
      if (getParameters().size() != reference.getParameters().size()) {
        return false;
      }
      List<String> strings = getParameters();
      for (int i = 0; i < strings.size(); i++) {
        String parameter = strings.get(i);
        if (!classMatchWithReference(parameter, reference.getParameters().get(i))) {
          return false;
        }
      }
      return true;
    }

    private boolean classMatchWithReference(String myName, String reference) {
      boolean classExactMatch = myName.equals(reference);
      boolean classUnqualifiedExactMatch = reference.endsWith("." + myName);
      return classExactMatch || classUnqualifiedExactMatch;
    }

    @Override
    public String toString() {
      return "Query{" +
          "className='" + className + '\'' +
          ", elementName='" + elementName + '\'' +
          ", parameters=" + parameters +
          '}';
    }

    public Query normalized() {
      return new Query(
          className != null ? className.toUpperCase(Locale.ROOT) : null,
          elementName != null ? elementName.toUpperCase(Locale.ROOT) : null,
          parameters == null
              ? null
              : parameters.stream()
                  .map(it -> it.toUpperCase(Locale.ROOT))
                  .collect(Collectors.toList())
      );
    }
  }
}
