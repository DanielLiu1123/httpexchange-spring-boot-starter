package io.github.danielliu1123.httpexchange.processor;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * {@link Finder} tester.
 */
class FinderTest {

    /**
     * {@link Finder#findFile(File, String)}
     */
    @Test
    @SneakyThrows
    void testFindFile() {
        ClassPathResource resource = new ClassPathResource("META-INF/services/javax.annotation.processing.Processor");
        File file = Finder.findFile(resource.getFile(), "build.gradle");

        assertNotNull(file);
    }
}
