package de.ialistannen.javadocapi.spoon;

import static de.ialistannen.javadocapi.spoon.JavadocElementExtractor.getModuleName;

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
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import spoon.javadoc.internal.Javadoc;
import spoon.javadoc.internal.JavadocDescriptionElement;
import spoon.javadoc.internal.JavadocInlineTag;
import spoon.javadoc.internal.JavadocSnippet;
import spoon.reflect.code.CtJavaDoc;
import spoon.reflect.code.CtJavaDocTag;
import spoon.reflect.code.CtJavaDocTag.TagType;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtImportKind;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

public class JavadocParser {

  private static final Pattern LINK_PATTERN = Pattern.compile(
      "^([\\w$.]*)(#([\\w.$]*)(\\((.*?)\\))?)?( .+)?$"
  );

  public JavadocComment fromCtJavadoc(CtJavaDoc javadoc) {
    List<JavadocCommentFragment> shortDescription = fromJavadoc(
        javadoc, Javadoc.parse(javadoc.getShortDescription())
    );
    List<JavadocCommentFragment> longDescription = fromJavadoc(
        javadoc, Javadoc.parse(javadoc.getLongDescription())
    );

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

    return new JavadocComment(tags, shortDescription, longDescription);
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

      QualifiedName qualifiedName = qualifyTypeName(reference, className);
      if (elementName != null) {
        String memberAppendix = "#" + elementName;
        if (parameters != null) {
          memberAppendix += "(";
          if (!parameters.isBlank()) {
            memberAppendix += Arrays.stream(parameters.split(","))
                .map(it -> it.strip().split(" ")[0])
                .map(it -> qualifyTypeName(reference, it))
                .map(QualifiedName::asString)
                .collect(Collectors.joining(","));
          }
          memberAppendix += ")";
        }

        qualifiedName = new QualifiedName(
            qualifiedName.asString() + memberAppendix,
            qualifiedName.getModuleName().orElse(null)
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

  private QualifiedName qualifyTypeName(CtJavaDoc element, String name) {
    QualifiedName qualifiedName = qualifyTypeNameNoArray(element, name.replace("[]", ""));
    if (!name.contains("[]")) {
      return qualifiedName;
    }
    return new QualifiedName(
        qualifiedName.asString() + "[]",
        qualifiedName.getModuleName().orElse(null)
    );
  }

  private QualifiedName qualifyTypeNameNoArray(CtJavaDoc element, String name) {
    CtType<?> parentType = element.getParent(CtType.class);
    if (parentType != null && !name.isBlank()) {
      Optional<CtTypeReference<?>> type = parentType.getReferencedTypes()
          .stream()
          .filter(it -> it.getSimpleName().equals(name) || it.getQualifiedName().equals(name))
          .findAny();
      if (type.isPresent()) {
        return new QualifiedName(
            type.get().getQualifiedName(),
            getModuleName(type.get().getTypeDeclaration())
        );
      }

      CtType<?> siblingType = parentType.getPackage().getType(name);
      if (siblingType != null) {
        return new QualifiedName(
            siblingType.getQualifiedName(),
            getModuleName(siblingType)
        );
      }
    }
    if (parentType != null && name.isBlank()) {
      return new QualifiedName(
          parentType.getQualifiedName(),
          getModuleName(parentType)
      );
    }

    CtCompilationUnit parentUnit = element.getPosition().getCompilationUnit();
    return parentUnit.getImports()
        .stream()
        .filter(it -> it.getImportKind() != CtImportKind.UNRESOLVED)
        .filter(it -> it.getReference().getSimpleName().equals(name))
        .findAny()
        .flatMap(ctImport ->
            ctImport.getReferencedTypes()
                .stream()
                .filter(it -> it.getSimpleName().equals(name))
                .findFirst()
                .map(ctTypeReference -> new QualifiedName(
                    ctTypeReference.getQualifiedName(),
                    getModuleName(ctTypeReference.getDeclaration())
                ))
        )
        .orElse(new QualifiedName(name, getModuleName(element)));
  }
}
