package cn.ykccchen.businessutil.util.match;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildContractTest {

    @Test
    void javaSourceDirectoryIsNotPackagedAsAResourceRoot() throws IOException {
        String pom = read(Paths.get("pom.xml"));
        assertFalse(pom.contains("<directory>src/main/java</directory>"));
        assertTrue(pom.contains("<include>**/*.java</include>"));
        assertNull(getClass().getClassLoader().getResource(".idea/workspace.xml"));
        try (Stream<Path> output = Files.walk(Paths.get("target/classes"))) {
            assertFalse(output.filter(Files::isRegularFile)
                    .map(path -> path.toString().toLowerCase())
                    .anyMatch(path -> path.contains(".idea")
                            || path.contains("sonarlint")
                            || path.endsWith(".iml")
                            || path.endsWith(".ds_store")
                            || path.endsWith("workspace.xml")));
        }
    }

    @Test
    void javadocErrorsAreNotIgnoredAndAllTagsAreStandard() throws IOException {
        String pom = read(Paths.get("pom.xml"));
        assertFalse(pom.contains("<failOnError>false</failOnError>"));
        try (Stream<Path> sources = Files.walk(Paths.get("src/main/java"))) {
            assertFalse(sources.filter(path -> path.toString().endsWith(".java"))
                    .map(BuildContractTest::readUnchecked)
                    .anyMatch(source -> source.contains("@date")));
        }
    }

    @Test
    void readmeDependencyVersionMatchesTheProjectVersion() throws IOException {
        String pom = read(Paths.get("pom.xml"));
        String readme = read(Paths.get("README.md"));
        Matcher projectVersion = Pattern.compile(
                "<artifactId>business-util</artifactId>\\s*<version>([^<]+)</version>")
                .matcher(pom);
        Matcher documentedVersion = Pattern.compile(
                "<artifactId>business-util</artifactId>\\s*<version>([^<]+)</version>")
                .matcher(readme);

        assertTrue(projectVersion.find());
        assertTrue(documentedVersion.find());
        assertEquals(projectVersion.group(1), documentedVersion.group(1));
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static String readUnchecked(Path path) {
        try {
            return read(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read " + path, exception);
        }
    }
}
