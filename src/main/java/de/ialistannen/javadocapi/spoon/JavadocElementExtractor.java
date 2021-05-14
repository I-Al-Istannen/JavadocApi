package de.ialistannen.javadocapi.spoon;

import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.comment.JavadocComment;
import de.ialistannen.javadocapi.model.types.JavadocAnnotation;
import de.ialistannen.javadocapi.model.types.JavadocField;
import de.ialistannen.javadocapi.model.types.JavadocMethod;
import de.ialistannen.javadocapi.model.types.JavadocMethod.Parameter;
import de.ialistannen.javadocapi.model.types.JavadocType;
import de.ialistannen.javadocapi.model.types.JavadocType.PossiblyGenericSupertype;
import de.ialistannen.javadocapi.model.types.JavadocType.Type;
import de.ialistannen.javadocapi.model.types.JavadocTypeParameter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import spoon.reflect.code.CtJavaDoc;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtFormalTypeDeclarer;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtIntersectionTypeReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

public class JavadocElementExtractor extends CtScanner {

  private final List<JavadocElement> foundElements;

  public JavadocElementExtractor() {
    this.foundElements = new ArrayList<>();
  }

  public List<JavadocElement> getFoundElements() {
    return foundElements;
  }

  @Override
  public <T> void visitCtInterface(CtInterface<T> ctInterface) {
    foundElements.add(forCtType(ctInterface, Type.INTERFACE));
    super.visitCtInterface(ctInterface);
  }

  @Override
  public <A extends Annotation> void visitCtAnnotationType(CtAnnotationType<A> annotationType) {
    foundElements.add(forCtType(annotationType, Type.ANNOTATION));
    super.visitCtAnnotationType(annotationType);
  }

  @Override
  public <T extends Enum<?>> void visitCtEnum(CtEnum<T> ctEnum) {
    foundElements.add(forCtType(ctEnum, Type.ENUM));
    super.visitCtEnum(ctEnum);
  }

  @Override
  public <T> void visitCtClass(CtClass<T> ctClass) {
    foundElements.add(forCtType(ctClass, Type.CLASS));
    super.visitCtClass(ctClass);
  }

  @Override
  public <T> void visitCtMethod(CtMethod<T> m) {
    List<Parameter> parameters = m.getParameters()
        .stream()
        .map(it -> new Parameter(
            new QualifiedName(it.getType().getQualifiedName()),
            it.getSimpleName())
        )
        .collect(Collectors.toList());

    List<QualifiedName> thrownTypes = m.getThrownTypes()
        .stream()
        .map(it -> new QualifiedName(it.getQualifiedName()))
        .collect(Collectors.toList());

    foundElements.add(new JavadocMethod(
        signatureToQualifiedName(m.getDeclaringType(), m.getSignature()),
        new QualifiedName(m.getType().getQualifiedName()),
        getModifiers(m),
        parameters,
        thrownTypes,
        getAnnotations(m),
        getTypeParameters(m),
        getComment(m)
    ));
    super.visitCtMethod(m);
  }

  @Override
  public <T> void visitCtField(CtField<T> f) {
    foundElements.add(new JavadocField(
        new QualifiedName(f.getDeclaringType().getQualifiedName() + "#" + f.getSimpleName()),
        getModifiers(f),
        new QualifiedName(f.getType().getQualifiedName()),
        getComment(f)
    ));
    super.visitCtField(f);
  }

  private List<String> getModifiers(CtModifiable m) {
    return m.getModifiers()
        .stream()
        .sorted()
        .map(ModifierKind::toString)
        .collect(Collectors.toList());
  }

  private List<JavadocAnnotation> getAnnotations(CtElement element) {
    return element.getAnnotations()
        .stream()
        .map(annotation -> {
          Map<String, String> values = annotation.getValues()
              .entrySet()
              .stream()
              .collect(Collectors.toMap(
                  Entry::getKey,
                  it -> it.getValue().toString()
              ));
          return new JavadocAnnotation(
              new QualifiedName(annotation.getType().getQualifiedName()),
              values
          );
        })
        .collect(Collectors.toList());
  }

  private <T> JavadocType forCtType(CtType<T> ctType, JavadocType.Type type) {
    List<QualifiedName> memberNames = ctType.getAllExecutables()
        .stream()
        .map(CtExecutableReference::getSignature)
        .map(sig -> signatureToQualifiedName(ctType, sig))
        .collect(Collectors.toCollection(ArrayList::new));

    ctType.getAllFields()
        .stream()
        .map(it -> ctType.getQualifiedName() + "#" + it.getSimpleName())
        .map(QualifiedName::new)
        .forEach(memberNames::add);

    List<PossiblyGenericSupertype> superInterfaces = ctType.getSuperInterfaces()
        .stream()
        .map(this::fromSuperTypeReference)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    PossiblyGenericSupertype superClass = fromSuperTypeReference(ctType.getSuperclass());

    return new JavadocType(
        new QualifiedName(ctType.getQualifiedName()),
        getModifiers(ctType),
        memberNames,
        getComment(ctType),
        getAnnotations(ctType),
        getTypeParameters(ctType),
        type,
        superInterfaces,
        superClass
    );
  }

  private PossiblyGenericSupertype fromSuperTypeReference(CtTypeReference<?> reference) {
    if (reference == null) {
      return null;
    }
    return new PossiblyGenericSupertype(
        new QualifiedName(reference.getQualifiedName()),
        getTypeParameters(reference.getTypeDeclaration())
    );
  }

  private List<JavadocTypeParameter> getTypeParameters(CtFormalTypeDeclarer m) {
    if (m == null) {
      return Collections.emptyList();
    }
    return m.getFormalCtTypeParameters()
        .stream()
        .map(Object::toString)
        .map(JavadocTypeParameter::new)
        .collect(Collectors.toList());
  }

  private JavadocComment getComment(CtElement ctType) {
    return ctType.getComments()
        .stream()
        .filter(it -> it instanceof CtJavaDoc)
        .findFirst()
        .map(it -> new JavadocParser().fromCtJavadoc((CtJavaDoc) it))
        .orElse(null);
  }

  private static QualifiedName signatureToQualifiedName(CtType<?> owner, String signature) {
    // convert <fqn>() (i.e. a constructor) to just SimpleName()
    if (signature.startsWith(owner.getQualifiedName())) {
      signature = signature.replaceFirst(owner.getQualifiedName(), owner.getSimpleName());
    }
    return new QualifiedName(owner.getQualifiedName() + "#" + signature);
  }
}
