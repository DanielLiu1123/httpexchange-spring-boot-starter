package io.github.danielliu1123.httpexchange.processor;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

/**
 * @author Freeman
 */
@UtilityClass
public class Finder {

    private static final List<Pattern> PROJECT_DIR_MARKERS = List.of(
            Pattern.compile(".*\\.gradle"), // Gradle
            Pattern.compile(".*\\.gradle\\.kts"), // Gradle Kotlin DSL
            Pattern.compile("pom\\.xml"), // Maven
            Pattern.compile("build\\.xml"), // Ant
            Pattern.compile("build\\.sbt"), // SBT
            Pattern.compile("BUILD"), // Bazel
            Pattern.compile("BUILD\\.bazel"), // Bazel
            Pattern.compile("project\\.clj") // Leiningen
            );

    /**
     * Find the project(module) dir by searching the parent dir recursively.
     *
     * @param file     the file to start searching
     * @param maxDepth the max depth to search
     * @return the project dir, or null if not found
     */
    public static File findProjectDir(File file, int maxDepth) {
        return findProjectDir(file, 0, maxDepth);
    }

    private static File findProjectDir(File file, int currentDepth, int maxDepth) {
        if (file == null || currentDepth > maxDepth) {
            return null;
        }
        if (file.isFile()) {
            return findProjectDir(file.getParentFile(), currentDepth + 1, maxDepth);
        }
        if (containsMarker(file)) {
            return file;
        }
        return findProjectDir(file.getParentFile(), currentDepth + 1, maxDepth);
    }

    private static boolean containsMarker(File dir) {
        return PROJECT_DIR_MARKERS.stream()
                .anyMatch(pattern -> !isEmpty(dir.listFiles(
                        (directory, fileName) -> pattern.matcher(fileName).matches())));
    }
}
