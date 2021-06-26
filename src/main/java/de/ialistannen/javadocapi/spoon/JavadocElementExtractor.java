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
import spoon.SpoonException;
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
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

public class JavadocElementExtractor extends CtScanner {

  private static final Set<String> PACKAGE_WHITELIST = Set.of(
      "java.applet",
      "java.awt",
      "java.awt.color",
      "java.awt.datatransfer",
      "java.awt.dnd",
      "java.awt.event",
      "java.awt.font",
      "java.awt.geom",
      "java.awt.im",
      "java.awt.im.spi",
      "java.awt.image",
      "java.awt.image.renderable",
      "java.awt.print",
      "java.beans",
      "java.beans.beancontext",
      "java.io",
      "java.lang",
      "java.lang.annotation",
      "java.lang.instrument",
      "java.lang.invoke",
      "java.lang.management",
      "java.lang.ref",
      "java.lang.reflect",
      "java.math",
      "java.net",
      "java.nio",
      "java.nio.channels",
      "java.nio.channels.spi",
      "java.nio.charset",
      "java.nio.charset.spi",
      "java.nio.file",
      "java.nio.file.attribute",
      "java.nio.file.spi",
      "java.rmi",
      "java.rmi.activation",
      "java.rmi.dgc",
      "java.rmi.registry",
      "java.rmi.server",
      "java.security",
      "java.security.acl",
      "java.security.cert",
      "java.security.interfaces",
      "java.security.spec",
      "java.sql",
      "java.text",
      "java.text.spi",
      "java.time",
      "java.time.chrono",
      "java.time.format",
      "java.time.temporal",
      "java.time.zone",
      "java.util",
      "java.util.concurrent",
      "java.util.concurrent.atomic",
      "java.util.concurrent.locks",
      "java.util.function",
      "java.util.jar",
      "java.util.logging",
      "java.util.prefs",
      "java.util.regex",
      "java.util.spi",
      "java.util.stream",
      "java.util.zip",
      "javax.accessibility",
      "javax.activation",
      "javax.activity",
      "javax.annotation",
      "javax.annotation.processing",
      "javax.crypto",
      "javax.crypto.interfaces",
      "javax.crypto.spec",
      "javax.imageio",
      "javax.imageio.event",
      "javax.imageio.metadata",
      "javax.imageio.plugins.bmp",
      "javax.imageio.plugins.jpeg",
      "javax.imageio.spi",
      "javax.imageio.stream",
      "javax.jws",
      "javax.jws.soap",
      "javax.lang.model",
      "javax.lang.model.element",
      "javax.lang.model.type",
      "javax.lang.model.util",
      "javax.management",
      "javax.management.loading",
      "javax.management.modelmbean",
      "javax.management.monitor",
      "javax.management.openmbean",
      "javax.management.relation",
      "javax.management.remote",
      "javax.management.remote.rmi",
      "javax.management.timer",
      "javax.naming",
      "javax.naming.directory",
      "javax.naming.event",
      "javax.naming.ldap",
      "javax.naming.spi",
      "javax.net",
      "javax.net.ssl",
      "javax.print",
      "javax.print.attribute",
      "javax.print.attribute.standard",
      "javax.print.event",
      "javax.rmi",
      "javax.rmi.CORBA",
      "javax.rmi.ssl",
      "javax.script",
      "javax.security.auth",
      "javax.security.auth.callback",
      "javax.security.auth.kerberos",
      "javax.security.auth.login",
      "javax.security.auth.spi",
      "javax.security.auth.x500",
      "javax.security.cert",
      "javax.security.sasl",
      "javax.sound.midi",
      "javax.sound.midi.spi",
      "javax.sound.sampled",
      "javax.sound.sampled.spi",
      "javax.sql",
      "javax.sql.rowset",
      "javax.sql.rowset.serial",
      "javax.sql.rowset.spi",
      "javax.swing",
      "javax.swing.border",
      "javax.swing.colorchooser",
      "javax.swing.event",
      "javax.swing.filechooser",
      "javax.swing.plaf",
      "javax.swing.plaf.basic",
      "javax.swing.plaf.metal",
      "javax.swing.plaf.multi",
      "javax.swing.plaf.nimbus",
      "javax.swing.plaf.synth",
      "javax.swing.table",
      "javax.swing.text",
      "javax.swing.text.html",
      "javax.swing.text.html.parser",
      "javax.swing.text.rtf",
      "javax.swing.tree",
      "javax.swing.undo",
      "javax.tools",
      "javax.transaction",
      "javax.transaction.xa",
      "javax.xml",
      "javax.xml.bind",
      "javax.xml.bind.annotation",
      "javax.xml.bind.annotation.adapters",
      "javax.xml.bind.attachment",
      "javax.xml.bind.helpers",
      "javax.xml.bind.util",
      "javax.xml.crypto",
      "javax.xml.crypto.dom",
      "javax.xml.crypto.dsig",
      "javax.xml.crypto.dsig.dom",
      "javax.xml.crypto.dsig.keyinfo",
      "javax.xml.crypto.dsig.spec",
      "javax.xml.datatype",
      "javax.xml.namespace",
      "javax.xml.parsers",
      "javax.xml.soap",
      "javax.xml.stream",
      "javax.xml.stream.events",
      "javax.xml.stream.util",
      "javax.xml.transform",
      "javax.xml.transform.dom",
      "javax.xml.transform.sax",
      "javax.xml.transform.stax",
      "javax.xml.transform.stream",
      "javax.xml.validation",
      "javax.xml.ws",
      "javax.xml.ws.handler",
      "javax.xml.ws.handler.soap",
      "javax.xml.ws.http",
      "javax.xml.ws.soap",
      "javax.xml.ws.spi",
      "javax.xml.ws.spi.http",
      "javax.xml.ws.wsaddressing",
      "javax.xml.xpath",
      "org.ietf.jgss",
      "org.omg.CORBA",
      "org.omg.CORBA_2_3",
      "org.omg.CORBA_2_3.portable",
      "org.omg.CORBA.DynAnyPackage",
      "org.omg.CORBA.ORBPackage",
      "org.omg.CORBA.portable",
      "org.omg.CORBA.TypeCodePackage",
      "org.omg.CosNaming",
      "org.omg.CosNaming.NamingContextExtPackage",
      "org.omg.CosNaming.NamingContextPackage",
      "org.omg.Dynamic",
      "org.omg.DynamicAny",
      "org.omg.DynamicAny.DynAnyFactoryPackage",
      "org.omg.DynamicAny.DynAnyPackage",
      "org.omg.IOP",
      "org.omg.IOP.CodecFactoryPackage",
      "org.omg.IOP.CodecPackage",
      "org.omg.Messaging",
      "org.omg.PortableInterceptor",
      "org.omg.PortableInterceptor.ORBInitInfoPackage",
      "org.omg.PortableServer",
      "org.omg.PortableServer.CurrentPackage",
      "org.omg.PortableServer.POAManagerPackage",
      "org.omg.PortableServer.POAPackage",
      "org.omg.PortableServer.portable",
      "org.omg.PortableServer.ServantLocatorPackage",
      "org.omg.SendingContext",
      "org.omg.stub.java.rmi",
      "org.w3c.dom",
      "org.w3c.dom.bootstrap",
      "org.w3c.dom.events",
      "org.w3c.dom.ls",
      "org.w3c.dom.views",
      "org.xml.sax",
      "org.xml.sax.ext",
      "org.xml.sax.helpers"
  );

  private final JavadocParser parser;
  private final List<JavadocElement> foundElements;

  public JavadocElementExtractor() {
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
    super.visitCtInterface(ctInterface);
  }

  @Override
  public <A extends Annotation> void visitCtAnnotationType(CtAnnotationType<A> annotationType) {
    // Skip internal classes
    if (!annotationType.isPublic()) {
      return;
    }

    foundElements.add(forCtType(annotationType, Type.ANNOTATION));
    super.visitCtAnnotationType(annotationType);
  }

  @Override
  public <T extends Enum<?>> void visitCtEnum(CtEnum<T> ctEnum) {
    // Skip internal classes
    if (!ctEnum.isPublic()) {
      return;
    }

    foundElements.add(forCtType(ctEnum, Type.ENUM));
    super.visitCtEnum(ctEnum);
  }

  @Override
  public void visitCtPackage(CtPackage ctPackage) {
    if (PACKAGE_WHITELIST.contains(ctPackage.getQualifiedName())) {
      super.visitCtPackage(ctPackage);
      return;
    }

    for (String s : PACKAGE_WHITELIST) {
      if (s.startsWith(ctPackage.getQualifiedName())) {
        super.visitCtPackage(ctPackage);
        return;
      }
    }

    super.visitCtPackage(ctPackage);
  }

  @Override
  public <T> void visitCtClass(CtClass<T> ctClass) {
    // Skip internal classes
    if (!ctClass.isPublic()) {
      return;
    }

    try {
      foundElements.add(forCtType(ctClass, Type.CLASS));
      super.visitCtClass(ctClass);
    } catch (SpoonException | NullPointerException e) {
      System.out.println(ctClass.getQualifiedName());
      System.out.println(ctClass);
      e.printStackTrace();
    }
  }

  @Override
  public <T> void visitCtMethod(CtMethod<T> m) {
    if (!m.isPublic()) {
      return;
    }

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
    if (!f.isPublic()) {
      return;
    }

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
        .flatMap(it -> getComment((CtJavaDoc) it, ctType))
        .orElse(null);
  }

  private Optional<JavadocComment> getComment(CtJavaDoc it, CtElement type) {
    try {
      return Optional.ofNullable(parser.fromCtJavadoc(it));
    } catch (Exception e) {
      System.out.println("==== " + type + " ====");
      e.printStackTrace();
      System.out.println(type);
      return Optional.empty();
    }
  }

  private static QualifiedName signatureToQualifiedName(CtType<?> owner, String signature) {
    // convert <fqn>() (i.e. a constructor) to just SimpleName()
    if (signature.startsWith(owner.getQualifiedName())) {
      signature = signature.replaceFirst(owner.getQualifiedName(), owner.getSimpleName());
    }
    return new QualifiedName(owner.getQualifiedName() + "#" + signature);
  }
}
