package io.github.htshame;

import org.gradle.api.GradleException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * YAML integration tests for {@link ValidateChangeLogTask}.
 */
public class ValidateChangeLogTaskYamlIntegrationTest {

    private ValidateChangeLogTask task;

    /**
     * Initialise a fresh task instance before each test.
     *
     * @throws URISyntaxException if a resource URL cannot be converted to a URI.
     */
    @BeforeEach
    public void setUp() throws URISyntaxException {
        task = ProjectBuilder.builder().build()
                .getTasks().create("validateLiquibaseChangeLog", ValidateChangeLogTask.class);
        task.getPathToRulesFile().set(resourceFile("rules.xml"));
        task.getPathToExclusionsFile().set(resourceFile("exclusions.xml"));
        task.getChangeLogDirectory().set(resourceFile("db/yaml"));
        task.getShouldFailBuild().set(true);
        task.getShouldGenerateExclusions().set(true);
        task.getChangeLogFormat().set("yaml");
        task.getPluginVersion().set("1.0");
    }

    /**
     * Integration test for {@link ValidateChangeLogTask#validateChangeLogs()}.
     * Expects 23 violations to be found.
     */
    @Test
    public void testExecute() {
        final GradleException exception =
                assertThrows(GradleException.class, task::validateChangeLogs);
        assertEquals("Validation failed: 23 violation(s) found.", exception.getMessage());
    }

    /**
     * Integration test for {@link ValidateChangeLogTask#validateChangeLogs()}.
     * Build should not fail when shouldFailBuild is false.
     */
    @Test
    public void testExecuteFailureShouldNotFailBuildFalse() {
        task.getShouldFailBuild().set(false);
        boolean isExceptionThrown = false;

        try {
            task.validateChangeLogs();
        } catch (GradleException e) {
            isExceptionThrown = true;
        }

        assertFalse(isExceptionThrown);
    }

    private File resourceFile(final String path) throws URISyntaxException {
        final URL url = getClass().getClassLoader().getResource(path);
        if (url == null) {
            return new File(path);
        }
        return new File(url.toURI());
    }
}
