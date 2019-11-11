package org.zhangjiangqige;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.utils.ParserCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
                            minimize(cu);
                        }
                        return Result.SAVE;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void minimize(CompilationUnit cu) {
        /**
         * Minimize a compilation unit (a source file) by removing bodies.
         */
        cu.findAll(ClassOrInterfaceDeclaration.class)
                .forEach(cOrI -> cOrI.getMethods().forEach(MethodDeclaration::removeBody));
    }
}