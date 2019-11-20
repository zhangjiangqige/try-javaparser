package org.zhangjiangqige;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAccessModifiers;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.utils.ParserCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * This program minimizes Java source files in a project by removing comments and method bodies.
 */
public class Minimizer {

    public static void main(String[] args) {
        String dir = "/path/to/some/java/project";
        Path root = Paths.get(dir);

        ParserConfiguration conf = new ParserConfiguration();
        // This removes all comments, by not parsing them at all
        conf.setAttributeComments(false);
        MinimizationVisitor sm = new MinimizationVisitor();

        ProjectRoot projectRoot = new ParserCollectionStrategy().collect(root);
        projectRoot.getSourceRoots().forEach(sourceRoot -> {
            try {
                sourceRoot.parse("", conf, new SourceRoot.Callback() {
                    @Override
                    public Result process(Path localPath, Path absolutePath, ParseResult<CompilationUnit> result) {
                        System.out.printf("Minimizing %s%n", absolutePath);
                        Optional<CompilationUnit> opt = result.getResult();
                        if (opt.isPresent()) {
                            CompilationUnit cu = opt.get();
                            sm.visit(cu, null);
                        }
                        return Result.SAVE;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static class MinimizationVisitor extends ModifierVisitor<Void> {
        @Override
        public ClassOrInterfaceDeclaration visit(ClassOrInterfaceDeclaration cid, Void arg) {
            super.visit(cid, arg);
            removeIfPrivateOrPkgPrivate(cid);
            return cid;
        }

        @Override
        public ConstructorDeclaration visit(ConstructorDeclaration cd, Void arg) {
            super.visit(cd, arg);
            if (!removeIfPrivateOrPkgPrivate(cd)) {
                // ConstructorDeclaration has to have a body
                cd.setBody(new BlockStmt());
            }
            return cd;
        }

        @Override
        public MethodDeclaration visit(MethodDeclaration md, Void arg) {
            super.visit(md, arg);
            if (!removeIfPrivateOrPkgPrivate(md))
                md.removeBody();
            return md;
        }

        @Override
        public FieldDeclaration visit(FieldDeclaration fd, Void arg) {
            super.visit(fd, arg);
            if (!removeIfPrivateOrPkgPrivate(fd)) {
                fd.getVariables().forEach(v -> v.getInitializer().ifPresent(Node::remove));
            }
            return fd;
        }

        @Override
        public InitializerDeclaration visit(InitializerDeclaration id, Void arg) {
            super.visit(id, arg);
            id.remove();
            return id;
        }

        @Override
        public NormalAnnotationExpr visit(NormalAnnotationExpr nae, Void arg) {
            super.visit(nae, arg);
            NodeList<MemberValuePair> empty = new NodeList<>();
            if (nae.getNameAsString().equals("Deprecated")) {
                nae.setPairs(empty);
            }
            return nae;
        }

        // remove the whole node if it's private or package private
        private boolean removeIfPrivateOrPkgPrivate(NodeWithAccessModifiers node) {
            AccessSpecifier as = node.getAccessSpecifier();
            if (as.equals(AccessSpecifier.PRIVATE) || as.equals(AccessSpecifier.PACKAGE_PRIVATE)) {
                ((Node)node).remove();
                return true;
            }
            return false;
        }
    }
}