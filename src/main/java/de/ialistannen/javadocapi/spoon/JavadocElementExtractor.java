package de.ialistannen.javadocapi.spoon;

import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.comment.JavadocComment;
import de.ialistannen.javadocapi.model.types.AnnotationValue;
import de.ialistannen.javadocapi.model.types.AnnotationValue.ListAnnotationValue;
import de.ialistannen.javadocapi.model.types.AnnotationValue.PrimitiveAnnotationValue;
import de.ialistannen.javadocapi.model.types.AnnotationValue.QualifiedAnnotationValue;
import de.ialistannen.javadocapi.model.types.JavadocAnnotation;
import de.ialistannen.javadocapi.model.types.JavadocField;
import de.ialistannen.javadocapi.model.types.JavadocMethod;
import de.ialistannen.javadocapi.model.types.JavadocMethod.Parameter;
import de.ialistannen.javadocapi.model.types.JavadocType;
import de.ialistannen.javadocapi.model.types.JavadocType.Type;
import de.ialistannen.javadocapi.model.types.JavadocTypeParameter;
import de.ialistannen.javadocapi.model.types.PossiblyGenericType;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtJavaDoc;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtFormalTypeDeclarer;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtModule;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtVariableReference;
import spoon.reflect.visitor.CtAbstractVisitor;

public class JavadocElementExtractor extends CtAbstractVisitor {

  private final JavadocParser parser;
  private final Collection<JavadocElement> foundElements;

  public JavadocElementExtractor() {
    this.foundElements = new ConcurrentLinkedQueue<>();
    this.parser = new JavadocParser();
  }

  public List<JavadocElement> getFoundElements() {
    return new ArrayList<>(foundElements);
  }

  @Override
  public <T> void visitCtInterface(CtInterface<T> ctInterface) {
    foundElements.add(forCtType(ctInterface, Type.INTERFACE));
    reportProgress();
    super.visitCtInterface(ctInterface);
  }

  private void reportProgress() {
    if (foundElements.size() % 1000 == 0) {
      System.out.println(" Indexed " + foundElements.size() + " elements so far");
    }
  }

  @Override
  public <A extends Annotation> void visitCtAnnotationType(CtAnnotationType<A> annotationType) {
    foundElements.add(forCtType(annotationType, Type.ANNOTATION));
    reportProgress();
    super.visitCtAnnotationType(annotationType);
  }

  @Override
  public <T> void visitCtEnumValue(CtEnumValue<T> enumValue) {
    foundElements.add(new JavadocField(
        new QualifiedName(
            enumValue.getDeclaringType().getQualifiedName() + "#" + enumValue.getSimpleName(),
            getModuleName(enumValue)
        ),
        getModifiers(enumValue),
        getPossiblyGenericType(enumValue.getType()),
        getComment(enumValue)
    ));
    reportProgress();
    super.visitCtEnumValue(enumValue);
  }

  @Override
  public <T extends Enum<?>> void visitCtEnum(CtEnum<T> ctEnum) {
    foundElements.add(forCtType(ctEnum, Type.ENUM));
    reportProgress();
    super.visitCtEnum(ctEnum);
  }

  @Override
  public <T> void visitCtClass(CtClass<T> ctClass) {
    foundElements.add(forCtType(ctClass, Type.CLASS));
    reportProgress();
    super.visitCtClass(ctClass);
  }

  @Override
  public <T> void visitCtConstructor(CtConstructor<T> c) {
    handleExecutable(c, c, c);
    reportProgress();
    super.visitCtConstructor(c);
  }

  @Override
  public <T> void visitCtMethod(CtMethod<T> m) {
    handleExecutable(m, m, m);
    reportProgress();
    super.visitCtMethod(m);
  }

  @Override
  public <T> void visitCtField(CtField<T> f) {
    foundElements.add(new JavadocField(
        new QualifiedName(
            f.getDeclaringType().getQualifiedName() + "#" + f.getSimpleName(),
            getModuleName(f.getDeclaringType())
        ),
        getModifiers(f),
        getPossiblyGenericType(f.getType()),
        getComment(f)
    ));
    reportProgress();
    super.visitCtField(f);
  }

  private void handleExecutable(
      CtExecutable<?> executable, CtFormalTypeDeclarer formalTypeDeclarer, CtModifiable modifiable
  ) {
    List<Parameter> parameters = executable.getParameters()
        .stream()
        .map(it -> new Parameter(
            getPossiblyGenericType(it.getType()),
            it.getSimpleName()
        ))
        .collect(Collectors.toList());

    List<QualifiedName> thrownTypes = executable.getThrownTypes()
        .stream()
        .map(it -> new QualifiedName(it.getQualifiedName(), getModuleName(it)))
        .collect(Collectors.toList());

    foundElements.add(new JavadocMethod(
        executableRefToQualifiedName(formalTypeDeclarer.getDeclaringType(),
            executable.getReference()),
        getPossiblyGenericType(executable.getType()),
        getModifiers(modifiable),
        parameters,
        thrownTypes,
        getAnnotations(executable),
        getTypeParameters(formalTypeDeclarer),
        getComment(executable)
    ));
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
        .filter(it -> {
          CtType<? extends Annotation> type = it.getAnnotationType().getTypeDeclaration();
          return type != null && type.hasAnnotation(Documented.class);
        })
        .map(annotation -> {
          Map<String, AnnotationValue> values = annotation.getValues()
              .entrySet()
              .stream()
              .collect(Collectors.toMap(
                  Entry::getKey,
                  it -> getAnnotationValue(it.getValue())
              ));
          return new JavadocAnnotation(
              new QualifiedName(
                  annotation.getType().getQualifiedName(),
                  getModuleName(annotation.getType().getTypeDeclaration())
              ),
              values
          );
        })
        .collect(Collectors.toList());
  }

  private AnnotationValue getAnnotationValue(CtExpression<?> value) {
    if (value instanceof CtNewArray<?>) {
      return new ListAnnotationValue(
          ((CtNewArray<?>) value).getElements()
              .stream()
              .map(this::getAnnotationValue)
              .collect(Collectors.toList())
      );
    }

    if (value instanceof CtVariableAccess<?>) {
      CtVariableAccess<?> variableAccess = (CtVariableAccess<?>) value;
      CtVariableReference<?> variable = variableAccess.getVariable();
      return new QualifiedAnnotationValue(
          new QualifiedName(
              variableAccess.getType().getQualifiedName() + "#" + variable.getSimpleName(),
              getModuleName(variableAccess.getType().getTypeDeclaration())
          )
      );
    }

    if (value instanceof CtLiteral) {
      return new PrimitiveAnnotationValue(value.toString());
    }

    if (value instanceof CtBinaryOperator) {
      CtBinaryOperator<?> operator = (CtBinaryOperator<?>) value;
      if (operator.getKind() == BinaryOperatorKind.PLUS) {
        return new PrimitiveAnnotationValue(
            operator.getLeftHandOperand().toString() + operator.getRightHandOperand().toString()
        );
      }
    }

    throw new IllegalArgumentException(
        "Unknown annotation value: " + value.getClass() + " " + value
    );
  }

  private <T> JavadocType forCtType(CtType<T> ctType, JavadocType.Type type) {
    List<QualifiedName> memberNames = ctType.getAllExecutables()
        .stream()
        .filter(ref -> executableReferenceIsVisible(ctType, ref))
        .map(ref -> executableRefToQualifiedName(ctType, ref))
        .collect(Collectors.toCollection(ArrayList::new));

    ctType.getAllFields()
        .stream()
        .filter(it -> it.getFieldDeclaration().isProtected() || it.getFieldDeclaration().isPublic())
        .map(it -> new QualifiedName(
            it.getDeclaringType().getQualifiedName() + "#" + it.getSimpleName(),
            getModuleName(it.getDeclaringType().getTypeDeclaration())
        ))
        .forEach(memberNames::add);

    List<PossiblyGenericType> superInterfaces = ctType.getSuperInterfaces()
        .stream()
        .map(this::fromSuperTypeReference)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    PossiblyGenericType superClass = fromSuperTypeReference(ctType.getSuperclass());

    return new JavadocType(
        new QualifiedName(ctType.getQualifiedName(), getModuleName(ctType)),
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

  private <T> boolean executableReferenceIsVisible(CtType<T> ctType, CtExecutableReference<?> ref) {
    CtExecutable<?> executable;
    try {
      executable = ref.getExecutableDeclaration();
    } catch (Exception e) {
      System.out.println(e.getClass() + " " + e.getMessage());
      return false;
    }
    if (ref.getSimpleName().isEmpty()) {
      // If ref has no name ref surely wasn't important
      // Apparently initializer blocks match this.
      return false;
    }
    if (executable == null) {
      System.out.println(
          " Failed to get declaration of " + executableRefToQualifiedName(ctType, ref)
      );
      System.out.println("  Name: " + ref.getSimpleName());
      return false;
    }
    if (executable instanceof CtModifiable) {
      CtModifiable modifiable = (CtModifiable) executable;
      return modifiable.isPublic() || modifiable.isProtected();
    }
    throw new IllegalArgumentException(
        "Unknown executable: " + executable.getClass()
            + " With signature " + executableRefToQualifiedName(ctType, ref)
    );
  }

  private PossiblyGenericType fromSuperTypeReference(CtTypeReference<?> reference) {
    if (reference == null) {
      return null;
    }
    return new PossiblyGenericType(
        new QualifiedName(reference.getQualifiedName(),
            getModuleName(reference.getTypeDeclaration())),
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

  private PossiblyGenericType getPossiblyGenericType(CtTypeReference<?> typeReference) {
    return new PossiblyGenericType(
        new QualifiedName(
            typeReference.getQualifiedName(),
            getModuleName(typeReference.getTypeDeclaration())
        ),
        typeReference.getActualTypeArguments().stream()
            .map(Object::toString)
            .map(JavadocTypeParameter::new)
            .collect(Collectors.toList())
    );
  }

  private JavadocComment getComment(CtElement ctType) {
    return ctType.getComments()
        .stream()
        .filter(it -> it instanceof CtJavaDoc)
        .findFirst()
        .flatMap(it -> getComment((CtJavaDoc) it))
        .orElse(null);
  }

  private Optional<JavadocComment> getComment(CtJavaDoc it) {
    try {
      return Optional.ofNullable(parser.fromCtJavadoc(it));
    } catch (Exception e) {
      System.out.println(" Fetching a JavaDoc comment failed :/");
      e.printStackTrace();
      return Optional.empty();
    }
  }

  static String getModuleName(CtElement element) {
    if (element == null) {
      System.out.println("  Element was null :/");
      return null;
    }
    if (element instanceof CtType) {
      switch (((CtType<?>) element).getSimpleName()) {
        case "boolean":
        case "byte":
        case "char":
        case "short":
        case "int":
        case "long":
        case "float":
        case "double":
        case "void":
          return null;
      }
    }

    CtModule module = element.getParent(CtModule.class);

    if (module == null) {
      System.out.println("Module was null for " + element.getClass());
      String s = element.toString();
      s = s.substring(0, Math.min(s.length(), 100));
      System.out.println("  " + s);
      return null;
    }

    if (module.isUnnamedModule()) {
      return null;
    }

    return module.getSimpleName();
  }

  private static QualifiedName executableRefToQualifiedName(CtType<?> owner,
      CtExecutableReference<?> ref) {
    String signature = ref.getSignature();
    // convert <fqn>() (i.e. a constructor) to just SimpleName()
    if (signature.startsWith(owner.getQualifiedName())) {
      signature = "<init>" + signature.substring(owner.getQualifiedName().length());
    }

    String ownerName = ref.getDeclaringType().getQualifiedName();
    return new QualifiedName(
        ownerName + "#" + signature,
        getModuleName(ref.getDeclaringType().getTypeDeclaration())
    );
  }
}
