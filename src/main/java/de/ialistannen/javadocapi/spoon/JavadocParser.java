package de.ialistannen.javadocapi.spoon;

import static de.ialistannen.javadocapi.spoon.JavadocElementExtractor.getModuleName;

import com.github.benmanes.caffeine.cache.Cache;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.comment.JavadocComment;
import de.ialistannen.javadocapi.model.comment.JavadocCommentFragment;
import de.ialistannen.javadocapi.model.comment.JavadocCommentInlineTag;
import de.ialistannen.javadocapi.model.comment.JavadocCommentInlineTag.Type;
import de.ialistannen.javadocapi.model.comment.JavadocCommentLink;
import de.ialistannen.javadocapi.model.comment.JavadocCommentTag;
import de.ialistannen.javadocapi.model.comment.JavadocCommentText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import spoon.experimental.CtUnresolvedImport;
import spoon.javadoc.internal.Javadoc;
import spoon.javadoc.internal.JavadocDescriptionElement;
import spoon.javadoc.internal.JavadocInlineTag;
import spoon.javadoc.internal.JavadocSnippet;
import spoon.processing.FactoryAccessor;
import spoon.reflect.code.CtJavaDoc;
import spoon.reflect.code.CtJavaDocTag;
import spoon.reflect.code.CtJavaDocTag.TagType;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtImportKind;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeInformation;
import spoon.reflect.factory.TypeFactory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;

public class JavadocParser {

  private static final Pattern LINK_PATTERN = Pattern.compile(
      "^([\\w$.]*)(#([\\w.$]*)(\\((.*?)\\))?)?( .+)?$"
  );

  private final Cache<String, Collection<CtExecutableReference<?>>> cache;

  public JavadocParser(Cache<String, Collection<CtExecutableReference<?>>> executableCache) {
    this.cache = executableCache;
  }

  public JavadocComment fromCtJavadoc(CtJavaDoc javadoc) {
    List<JavadocCommentTag> tags = new ArrayList<>();
    for (CtJavaDocTag tag : javadoc.getTags()) {
      String content = tag.getContent().replaceAll("[\\s+]", " ");
      if (tag.getType() == TagType.SEE) {
        Matcher matcher = LINK_PATTERN.matcher(content);
        content = matcher.replaceFirst("{@link $1$2$6}");
      }

      tags.add(new JavadocCommentTag(
          tag.getRealName(),
          tag.getParam(),
          fromJavadoc(javadoc, Javadoc.parse(content))
      ));
    }

    List<JavadocCommentFragment> content = fromJavadoc(
        javadoc,
        Javadoc.parse(javadoc.getContent())
    );

    return new JavadocComment(tags, content);
  }

  private List<JavadocCommentFragment> fromJavadoc(CtJavaDoc reference, Javadoc doc) {
    List<JavadocCommentFragment> fragments = new ArrayList<>();

    for (JavadocDescriptionElement element : doc.getDescription().getElements()) {
      if (element instanceof JavadocInlineTag) {
        fragments.add(parseInlineTag(reference, (JavadocInlineTag) element));
      } else if (element instanceof JavadocSnippet) {
        fragments.add(new JavadocCommentText(element.toText()));
      } else {
        throw new AssertionError("Unknown javadoc type: " + element.getClass());
      }
    }

    return fragments;
  }

  private JavadocCommentInlineTag parseInlineTag(CtJavaDoc reference, JavadocInlineTag inline) {
    Type type = null;
    try {
      type = Type.valueOf(inline.getType().name());
    } catch (IllegalArgumentException e) {
      CtType<?> parent = reference.getParent(CtType.class);
      if (parent != null) {
        System.out.println(
            "Lookup for " + inline.getType().name() + " failed in " + parent.getQualifiedName()
        );
      } else {
        System.out.println("Lookup for " + inline.getType().name() + " failed");
      }
    }

    if (type == Type.LINK || type == Type.LINKPLAIN) {
      // Normalize newlines to single spaces
      String text = inline.getContent().replaceAll("\\s+", " ");
      if (text.matches("^\\w+\\(.+")) {
        text = "#" + text;
      }

      Matcher matcher = LINK_PATTERN.matcher(text);
      if (!matcher.find()) {
        if (text.contains("<a ")) {
          return new JavadocCommentInlineTag(Type.LINK, text);
        }
        CtType<?> parent = reference.getParent(CtType.class);
        if (parent != null) {
          System.out.println(
              " Link lookup failed in " + parent.getQualifiedName() + " for '" + text + "'"
          );
        } else {
          System.out.println(" Link lookup failed for '" + text + "'");
        }
        return new JavadocCommentInlineTag(Type.LINK, text);
      }
      String className = matcher.group(1);
      String elementName = matcher.group(3);
      String parameters = matcher.group(5);
      String label = matcher.group(6);

      QualifiedName qualifiedName;
      if (elementName == null) {
        qualifiedName = qualifyTypeName(reference, className);
      } else {
        List<QualifiedName> elementParameterTypes = null;

        if (parameters != null) {
          elementParameterTypes = List.of();

          if (!parameters.isBlank()) {
            elementParameterTypes = Arrays.stream(parameters.split(","))
                .map(it -> it.strip().split(" ")[0])
                .map(it -> qualifyTypeName(reference, it))
                .collect(Collectors.toList());
          }
        }

        qualifiedName = qualifyName(
            reference, className, elementName, elementParameterTypes
        );
      }

      return new JavadocCommentLink(
          qualifiedName,
          label,
          type == Type.LINKPLAIN
      );
    }

    return new JavadocCommentInlineTag(
        type,
        inline.getContent()
    );
  }

  private QualifiedName qualifyName(CtJavaDoc reference,
      String className, String elementName, List<QualifiedName> parameters) {
    Optional<CtType<?>> enclosingType = qualifyType(reference, className);

    String memberAppendix = "#" + elementName;
    if (parameters != null) {
      memberAppendix += "(";
      memberAppendix += parameters
          .stream()
          .map(QualifiedName::asString)
          .collect(Collectors.joining(","));
      memberAppendix += ")";
    }

    QualifiedName fallbackName = qualifyTypeName(reference, className).appendToName(memberAppendix);

    if (enclosingType.isEmpty()) {
      return fallbackName;
    }
    CtType<?> type = enclosingType.get();

    if (parameters == null) {
      Optional<QualifiedName> field = type.getAllFields()
          .stream()
          .filter(it -> it.getSimpleName().equals(elementName))
          .findFirst()
          .map(CtFieldReference::getDeclaringType)
          .map(it -> new QualifiedName(
              it.getQualifiedName() + "#" + elementName,
              getModuleName(it.getTypeDeclaration())
          ));

      // Try again as an executable
      return field.orElseGet(() ->
          qualifyTypeNameForExecutable(elementName, List.of(), fallbackName, type)
      );
    }

    return qualifyTypeNameForExecutable(elementName, parameters, fallbackName, type);
  }

  private QualifiedName qualifyTypeNameForExecutable(
      String elementName, List<QualifiedName> parameters, QualifiedName fallbackName,
      CtType<?> type) {
    List<CtExecutableReference<?>> possibleMethods = cache.get(
            type.getQualifiedName(),
            ignored -> type.getAllExecutables()
        )
        .stream()
        .filter(it -> it.getSimpleName().equals(elementName))
        .collect(Collectors.toList());

    Optional<CtExecutableReference<?>> relevantMethod;
    if (possibleMethods.size() == 1) {
      relevantMethod = Optional.of(possibleMethods.get(0));
    } else {
      relevantMethod = possibleMethods
          .stream()
          .filter(it -> it.getParameters().size() == parameters.size())
          .filter(it -> parameterTypesMatch(it.getParameters(), parameters))
          .findFirst();
    }

    return relevantMethod
        .map(it -> {
          String paramString;

          CtExecutable<?> executableDeclaration = it.getExecutableDeclaration();
          if (executableDeclaration != null) {
            paramString = getParameterStringFromExecutableDeclaration(executableDeclaration);
          } else {
            paramString = getParameterStringFromExecutableReference(it);
          }

          String methodName = it.getSimpleName() + "(" + paramString + ")";

          return new QualifiedName(
              it.getDeclaringType().getQualifiedName() + "#" + methodName,
              getModuleName(it.getDeclaringType().getTypeDeclaration())
          );
        })
        .orElse(fallbackName);
  }

  private String getParameterStringFromExecutableDeclaration(CtExecutable<?> declaration) {
    return declaration.getParameters()
        .stream()
        .map(parameter -> {
          if (parameter.isVarArgs()) {
            return qualifyVarargsParameter(parameter);
          }
          return parameter.getType().getQualifiedName();
        })
        .collect(Collectors.joining(","));
  }

  private String qualifyVarargsParameter(CtParameter<?> parameter) {
    String qualifiedName = parameter.getType().getQualifiedName();

    if (qualifiedName.endsWith("[]")) {
      qualifiedName = qualifiedName.substring(0, qualifiedName.lastIndexOf('['));
    }

    qualifiedName += "...";
    return qualifiedName;
  }

  private String getParameterStringFromExecutableReference(CtExecutableReference<?> reference) {
    return reference.getParameters()
        .stream()
        .map(CtTypeInformation::getQualifiedName)
        .collect(Collectors.joining(","));
  }

  private boolean parameterTypesMatch(List<CtTypeReference<?>> actualParams,
      List<QualifiedName> parameters) {
    for (int i = 0; i < parameters.size(); i++) {
      String actualName = actualParams.get(i).getQualifiedName();
      if (!actualName.equals(parameters.get(i).asString())) {
        return false;
      }
    }
    return true;
  }

  private QualifiedName qualifyTypeName(CtJavaDoc element, String name) {
    QualifiedName qualifiedName = qualifyTypeNameNoArray(
        element,
        name.replace("[]", "").replace("...", "")
    );

    if (!name.contains("[]") && !name.endsWith("...")) {
      return qualifiedName;
    }

    String arraySignifier;
    if (name.contains("[")) {
      arraySignifier = name.substring(name.indexOf('['));
    } else {
      arraySignifier = "...";
    }

    return new QualifiedName(
        qualifiedName.asString() + arraySignifier,
        qualifiedName.getModuleName().orElse(null)
    );
  }

  private QualifiedName qualifyTypeNameNoArray(CtJavaDoc element, String name) {
    return qualifyType(element, name)
        .map(it -> new QualifiedName(it.getQualifiedName(), getModuleName(it)))
        .orElse(new QualifiedName(name, getModuleName(element)));
  }

  private Optional<CtType<?>> qualifyType(CtJavaDoc element, String name) {
    CtType<?> parentType = element.getParent(CtType.class);
    if (parentType != null && !name.isBlank()) {
      Optional<CtTypeReference<?>> type = parentType.getReferencedTypes()
          .stream()
          .filter(it -> it.getSimpleName().equals(name) || it.getQualifiedName().equals(name))
          .findAny();
      if (type.isPresent()) {
        return Optional.ofNullable(type.get().getTypeDeclaration());
      }

      CtType<?> siblingType = parentType.getPackage().getType(name);
      if (siblingType != null) {
        return Optional.of(siblingType);
      }
    }
    if (parentType != null && name.isBlank()) {
      return Optional.of(parentType);
    }

    CtCompilationUnit parentUnit = element.getPosition().getCompilationUnit();
    Optional<CtType<?>> importedType = getImportedType(name, parentUnit);
    if (importedType.isPresent()) {
      return importedType;
    }

    // The classes are not imported and not referenced if they are only used in javadoc...
    if (name.startsWith("java.lang")) {
      return tryLoadModelOrReflection(element, name);
    }

    CtType<?> directLookupType = element.getFactory().Type().get(name);
    if (directLookupType != null) {
      return Optional.of(directLookupType);
    }

    return tryLoadModelOrReflection(element, name)
        .or(() -> tryLoadModelOrReflection(element, "java.lang." + name));
  }

  private Optional<CtType<?>> getImportedType(String name, CtCompilationUnit parentUnit) {
    Optional<CtType<?>> referencedImportedType = parentUnit.getImports()
        .stream()
        .filter(it -> it.getImportKind() != CtImportKind.UNRESOLVED)
        .filter(it -> it.getReference().getSimpleName().equals(name))
        .findAny()
        .flatMap(ctImport ->
            ctImport.getReferencedTypes()
                .stream()
                .filter(it -> it.getSimpleName().equals(name))
                .findFirst()
                .map(CtTypeReference::getTypeDeclaration)
        );

    if (referencedImportedType.isPresent()) {
      return referencedImportedType;
    }

    return parentUnit.getImports()
        .stream()
        .filter(it -> it.getImportKind() == CtImportKind.UNRESOLVED)
        .filter(it -> ((CtUnresolvedImport) it).getUnresolvedReference().endsWith("*"))
        .flatMap(it -> {
          String reference = ((CtUnresolvedImport) it).getUnresolvedReference();
          reference = reference.substring(0, reference.length() - 1);

          return tryLoadModelOrReflection(parentUnit, reference + name).stream();
        })
        .findFirst();
  }

  private Optional<CtType<?>> tryLoadModelOrReflection(FactoryAccessor base, String name) {
    TypeFactory typeFactory = base.getFactory().Type();

    CtType<?> inModel = typeFactory.get(name);
    if (inModel != null) {
      return Optional.of(inModel);
    }
    return tryLoadClass(name).map(typeFactory::get);
  }

  private Optional<Class<?>> tryLoadClass(String name) {
    try {
      return Optional.of(Class.forName(name));
    } catch (ClassNotFoundException e) {
      return Optional.empty();
    }
  }
}
