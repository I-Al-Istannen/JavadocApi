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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import spoon.reflect.code.CtJavaDoc;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtFormalTypeDeclarer;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtModule;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

public class JavadocElementExtractor extends CtScanner {

  private final JavadocParser parser;
  private final List<JavadocElement> foundElements;
  private final Set<String> packageWhitelist;

  public JavadocElementExtractor(Set<String> packageWhitelist) {
    this.packageWhitelist = packageWhitelist;
    this.foundElements = new ArrayList<>();
    this.parser = new JavadocParser();
  }

  public List<JavadocElement> getFoundElements() {
    return foundElements;
  }

  @Override
  public <T> void visitCtInterface(CtInterface<T> ctInterface) {
    // Skip internal classes
    if (!ctInterface.isPublic()) {
      return;
    }

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
    // Skip internal classes
    if (!annotationType.isPublic()) {
      return;
    }

    foundElements.add(forCtType(annotationType, Type.ANNOTATION));
    reportProgress();
    super.visitCtAnnotationType(annotationType);
  }

  @Override
  public <T extends Enum<?>> void visitCtEnum(CtEnum<T> ctEnum) {
    // Skip internal classes
    if (!ctEnum.isPublic()) {
      return;
    }

    foundElements.add(forCtType(ctEnum, Type.ENUM));
    reportProgress();
    super.visitCtEnum(ctEnum);
  }

  @Override
  public void visitCtPackage(CtPackage ctPackage) {
    if (packageWhitelist.contains(ctPackage.getQualifiedName())) {
      super.visitCtPackage(ctPackage);
      return;
    }

    for (String allowedPackage : packageWhitelist) {
      if (allowedPackage.startsWith(ctPackage.getQualifiedName())) {
        super.visitCtPackage(ctPackage);
        return;
      }
    }
  }

  @Override
  public <T> void visitCtClass(CtClass<T> ctClass) {
    // Skip internal classes
    if (!ctClass.isPublic()) {
      return;
    }

    foundElements.add(forCtType(ctClass, Type.CLASS));
    reportProgress();
    super.visitCtClass(ctClass);
  }

  @Override
  public <T> void visitCtMethod(CtMethod<T> m) {
    if (!m.isPublic() && !m.isProtected()) {
      return;
    }

    List<Parameter> parameters = m.getParameters()
        .stream()
        .map(it -> new Parameter(
            new QualifiedName(it.getType().getQualifiedName(), getModuleName(it)),
            it.getSimpleName())
        )
        .collect(Collectors.toList());

    List<QualifiedName> thrownTypes = m.getThrownTypes()
        .stream()
        .map(it -> new QualifiedName(it.getQualifiedName(), getModuleName(it)))
        .collect(Collectors.toList());

    foundElements.add(new JavadocMethod(
        executableRefToQualifiedName(m.getDeclaringType(), m.getReference()),
        new QualifiedName(m.getType().getQualifiedName(), getModuleName(m)),
        getModifiers(m),
        parameters,
        thrownTypes,
        getAnnotations(m),
        getTypeParameters(m),
        getComment(m)
    ));
    reportProgress();
    super.visitCtMethod(m);
  }

  @Override
  public <T> void visitCtField(CtField<T> f) {
    if (!f.isPublic() && !f.isProtected()) {
      return;
    }

    foundElements.add(new JavadocField(
        new QualifiedName(
            f.getDeclaringType().getQualifiedName() + "#" + f.getSimpleName(),
            getModuleName(f)
        ),
        getModifiers(f),
        new QualifiedName(f.getType().getQualifiedName(), getModuleName(f)),
        getComment(f)
    ));
    reportProgress();
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
              new QualifiedName(annotation.getType().getQualifiedName(), getModuleName(element)),
              values
          );
        })
        .collect(Collectors.toList());
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
        .map(it -> it.getDeclaringType().getQualifiedName() + "#" + it.getSimpleName())
        .map(it -> new QualifiedName(it, getModuleName(ctType)))
        .forEach(memberNames::add);

    List<PossiblyGenericSupertype> superInterfaces = ctType.getSuperInterfaces()
        .stream()
        .map(this::fromSuperTypeReference)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    PossiblyGenericSupertype superClass = fromSuperTypeReference(ctType.getSuperclass());

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
    if (ref.getSimpleName().isEmpty() || ref.getSimpleName().equals("<init>")) {
      // If ref has no name ref surely wasn't important
      // (Should only filter out static initializer blocks)
      // And instance-initializer blocks are called "init". I have no idea why the static ones
      // aren't called "clinit" in spoon, but I maybe don't want to know the answer.
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

  private PossiblyGenericSupertype fromSuperTypeReference(CtTypeReference<?> reference) {
    if (reference == null) {
      return null;
    }
    return new PossiblyGenericSupertype(
        new QualifiedName(reference.getQualifiedName(), getModuleName(reference.getTypeDeclaration())),
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

    return module.getSimpleName();
  }

  private static QualifiedName executableRefToQualifiedName(CtType<?> owner,
      CtExecutableReference<?> ref) {
    String signature = ref.getSignature();
    // convert <fqn>() (i.e. a constructor) to just SimpleName()
    if (signature.startsWith(owner.getQualifiedName())) {
      signature = signature.replaceFirst(owner.getQualifiedName(), owner.getSimpleName());
    }

    String ownerName = ref.getDeclaringType().getQualifiedName();
    return new QualifiedName(ownerName + "#" + signature, getModuleName(owner));
  }
}
