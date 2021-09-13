package de.ialistannen.javadocapi.spoon;

import java.util.List;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtExecutableReference;
import spoon.support.visitor.SignaturePrinter;

class VarargsCompatibleSignaturePrinter extends SignaturePrinter {

  @Override
  public <T> void writeNameAndParameters(CtExecutableReference<T> reference) {
    CtExecutable<T> executable = reference.getExecutableDeclaration();
    if (executable == null) {
      super.writeNameAndParameters(reference);
      return;
    }

    if (reference.isConstructor()) {
      write(reference.getDeclaringType().getQualifiedName());
    } else {
      write(reference.getSimpleName());
    }

    write("(");
    writeParameters(executable.getParameters());
    write(")");
  }

  private void writeParameters(List<CtParameter<?>> parameters) {
    for (int i = 0; i < parameters.size(); i++) {
      CtParameter<?> parameter = parameters.get(i);
      if (parameter.getType() instanceof CtArrayTypeReference) {
        writeArrayParameter(parameter);
      } else {
        scan(parameter.getType());
      }

      if (i < parameters.size() - 1) {
        write(",");
      }
    }
  }

  private void writeArrayParameter(CtParameter<?> parameter) {
    CtArrayTypeReference<?> arrayRef = (CtArrayTypeReference<?>) parameter.getType();
    scan(arrayRef.getComponentType());

    if (parameter.isVarArgs()) {
      write("[]".repeat(arrayRef.getDimensionCount() - 1));
      write("...");
    } else {
      write("[]".repeat(arrayRef.getDimensionCount()));
    }
  }

  public static String getSignature(CtExecutableReference<?> reference) {
    VarargsCompatibleSignaturePrinter printer = new VarargsCompatibleSignaturePrinter();
    printer.scan(reference);
    return printer.getSignature();
  }
}
