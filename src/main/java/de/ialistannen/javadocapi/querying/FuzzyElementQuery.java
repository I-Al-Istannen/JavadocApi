package de.ialistannen.javadocapi.querying;

import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.types.JavadocType;
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
  public List<FuzzyQueryResult> query(ElementLoader queryApi, String queryString) {
    queryString = queryString.toUpperCase(Locale.ROOT);
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
    Collection<LoadResult<QualifiedName>> potentialElements = queryApi
        .findElementByName(query.getElementName())
        .stream()
        .filter(it -> !(it.getResult() instanceof JavadocType))
        .map(it -> it.map(JavadocElement::getQualifiedName))
        .collect(Collectors.toList());

    if (query.getParameters() == null) {
      return potentialElements.stream()
          .map(it -> toResult(query, it))
          .collect(Collectors.toList());
    }

    return potentialElements.stream()
        .filter(it -> fuzzyMatchParameters(query.getParameters(), it.getResult()))
        .map(it -> toResult(query, it))
        .collect(Collectors.toList());
  }

  private List<FuzzyQueryResult> findElementsFromClass(ElementLoader queryApi, Query query) {
    Collection<LoadResult<JavadocType>> potentialClasses = queryApi.findClassByName(
        query.getClassName()
    );

    if (query.getElementName() == null) {
      return potentialClasses
          .stream()
          .map(it -> it.map(JavadocType::getQualifiedName))
          .map(name -> toResult(query, name))
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
                  .endsWith(query.getElementName())
          )
          .filter(it -> {
            // Filter out methods if user specified a "(", ignore it otherwise
            if (query.getParameters() != null) {
              return it.getResult().isMethod();
            }
            return true;
          })
          .collect(Collectors.toList());

      if (query.getParameters() == null) {
        results.addAll(enclosed);
        continue;
      }

      enclosed.stream()
          .filter(it -> fuzzyMatchParameters(query.getParameters(), it.getResult()))
          .forEach(results::add);
    }

    return results
        .stream()
        .map(name -> toResult(query, name))
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

  private FuzzyQueryResult toResult(Query query, LoadResult<QualifiedName> element) {
    Query elementQuery = Query.fromString(element.getResult().asString().toUpperCase(Locale.ROOT));
    boolean exact = query.exactToReference(elementQuery);

    return new FuzzyQueryResult(exact, element.getResult(), element.getLoader());
  }

  private static class Query {

    private static final Pattern FIELD_PATTERN = Pattern.compile("^([\\w.$]+)#([\\w$]+)$");
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "^([\\w.$]+)#([\\w$]+)\\((.*)\\)?$"
    );
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("([\\w.$]+)( [\\w$]+)?(, *)?");

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
        return new Query(matcher.group(1).strip(), matcher.group(2).strip(), null);
      }

      matcher = METHOD_PATTERN.matcher(query);
      if (!matcher.matches()) {
        return null;
      }
      String className = matcher.group(1).strip();
      String methodName = matcher.group(2).strip();
      String parameterString = matcher.group(3).strip();

      return new Query(className, methodName, extractParameters(parameterString));
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
      boolean classUnqualifiedExactMatch = reference.contains("." + myName);
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
  }
}
