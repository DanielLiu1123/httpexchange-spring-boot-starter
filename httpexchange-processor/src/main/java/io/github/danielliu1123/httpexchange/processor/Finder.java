package io.github.danielliu1123.httpexchange.processor;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * @author Freeman
 */
@UtilityClass
class Finder {

    /**
     * Find the target file by searching the parent dir recursively.
     *
     * @param file           the file to start searching
     * @param targetFileName the target file name
     * @return the target file if found, otherwise null
     */
    public static File findFile(File file, String targetFileName) {
        return findFileRecursively(file, 0, 50, targetFileName);
    }

    private static File findFileRecursively(File file, int currentDepth, int maxDepth, String targetFileName) {
        if (file == null || currentDepth > maxDepth) {
            return null;
        }

        // Check if the current file (or directory) is the target file
        if (file.isFile() && Objects.equals(file.getName(), targetFileName)) {
            return file;
        }

        // If it's a directory, check if the target file is directly inside it
        if (file.isDirectory()) {
            File targetFile = Optional.ofNullable(file.listFiles((dir, name) -> Objects.equals(name, targetFileName)))
                    .filter(files -> files.length > 0)
                    .map(files -> files[0])
                    .filter(File::isFile)
                    .orElse(null);
            if (targetFile != null) {
                return targetFile;
            }
        }

        // Recursively search in the parent directory
        return findFileRecursively(file.getParentFile(), currentDepth + 1, maxDepth, targetFileName);
    }
}
