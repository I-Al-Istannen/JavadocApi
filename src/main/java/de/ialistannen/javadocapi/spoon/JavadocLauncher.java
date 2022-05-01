package de.ialistannen.javadocapi.spoon;

import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import spoon.Launcher;
import spoon.SpoonModelBuilder;
import spoon.reflect.factory.Factory;
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler;
import spoon.support.compiler.jdt.JDTTreeBuilder;

import java.util.Arrays;

public class JavadocLauncher extends Launcher {

    @Override
    protected SpoonModelBuilder getCompilerInstance(Factory factory) {
        return new Compiler(factory);
    }

    static class Compiler extends JDTBasedSpoonCompiler {
        private final JDTTreeBuilder treeBuilder;
        public Compiler(Factory factory) {
            super(factory);
            this.treeBuilder = new JDTTreeBuilder(factory) {

                @Override
                public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
                    methodDeclaration.statements = null;
                    return super.visit(methodDeclaration, scope);
                }
            };
        }

        @Override
        protected void traverseUnitDeclaration(JDTTreeBuilder builder, CompilationUnitDeclaration unitDeclaration) {
            super.traverseUnitDeclaration(this.treeBuilder, unitDeclaration);
            unitDeclaration.comments = reduceComments(unitDeclaration);
        }
    }

    static int[][] reduceComments(CompilationUnitDeclaration declaration) {
        int[][] comments = declaration.comments;
        if (comments == null) {
            return null;
        }
        return Arrays.stream(comments)
                .filter(comment -> comment[0] >= 0 && comment[1] >= 0)
                .toArray(int[][]::new);
    }
}
