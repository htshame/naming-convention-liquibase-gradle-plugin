package io.github.htshame;

import org.gradle.api.GradleException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Failure scenarios for {@link ValidateChangeLogTask}.
 */
public class ValidateChangeLogTaskFailureTest {

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
        task.getShouldFailBuild().set(true);
        task.getShouldGenerateExclusions().set(true);
        task.getChangeLogFormat().set("xml");
        task.getPluginVersion().set("1.0");
        task.getChangeLogDirectory().set(resourceFile("db/xml"));
        task.getPathToRulesFile().set(resourceFile("rules.xml"));
        task.getPathToExclusionsFile().set(resourceFile("exclusions.xml"));
    }

    /**
     * Integration test for {@link ValidateChangeLogTask#validateChangeLogs()}.
     * Should fail rule parsing.
     *
     * @throws URISyntaxException if a resource URL cannot be converted to a URI.
     */
    @Test
    public void testExecuteRuleParseFailure() throws URISyntaxException {
        task.getPathToRulesFile().set(resourceFile("io/github/htshame/failure/rules_failure.xml"));

        final GradleException exception =
                assertThrows(GradleException.class, task::validateChangeLogs);
        assertEquals(
                "Error parsing ruleset XML file. Message: Content is not allowed in prolog.",
                exception.getMessage());
    }

    /**
     * Integration test for {@link ValidateChangeLogTask#validateChangeLogs()}.
     * Should fail exclusion parsing.
     *
     * @throws URISyntaxException if a resource URL cannot be converted to a URI.
     */
    @Test
    public void testExecuteExclusionParseFailure() throws URISyntaxException {
        task.getPathToExclusionsFile().set(
                resourceFile("io/github/htshame/failure/exclusions_failure.xml"));

        final GradleException exception =
                assertThrows(GradleException.class, task::validateChangeLogs);
        assertEquals("Error parsing exclusion XML file", exception.getMessage());
    }

    /**
     * Integration test for {@link ValidateChangeLogTask#validateChangeLogs()}.
     * Should fail changeLog parsing and produce 3 violations.
     *
     * @throws URISyntaxException if a resource URL cannot be converted to a URI.
     */
    @Test
    public void testExecuteChangeLogParseFailure() throws URISyntaxException {
        task.getChangeLogDirectory().set(resourceFile("io/github/htshame/failure/changeLog"));

        final GradleException exception =
                assertThrows(GradleException.class, task::validateChangeLogs);
        assertEquals("Validation failed: 3 violation(s) found.", exception.getMessage());
    }

    /**
     * Integration test for {@link ValidateChangeLogTask#validateChangeLogs()}.
     * Should fail because of an unsupported changeLog format.
     */
    @Test
    public void testExecuteWrongFormatFailure() {
        task.getChangeLogFormat().set("xml1");

        final GradleException exception =
                assertThrows(GradleException.class, task::validateChangeLogs);
        assertEquals("ChangeLog format [xml1] is not supported", exception.getMessage());
    }

    /**
     * Integration test for {@link ValidateChangeLogTask#validateChangeLogs()}.
     * Should fail because the changeLog directory does not exist.
     */
    @Test
    public void testExecuteWrongChangeLogDirectoryFailure() {
        task.getChangeLogDirectory().set(new File("nonexistent/db/xml1"));

        final GradleException exception =
                assertThrows(GradleException.class, task::validateChangeLogs);
        assertTrue(exception.getMessage().startsWith("Invalid path:"));
        assertTrue(exception.getMessage().contains("xml1"));
    }

    /**
     * Integration test for {@link ValidateChangeLogTask#validateChangeLogs()}.
     * Should fail because the rules file path does not exist.
     */
    @Test
    public void testExecuteWrongRuleFilePathFailure() {
        task.getPathToRulesFile().set(new File("nonexistent/rules.xml111"));

        final GradleException exception =
                assertThrows(GradleException.class, task::validateChangeLogs);
        assertTrue(exception.getMessage().startsWith("Invalid path:"));
        assertTrue(exception.getMessage().contains("rules.xml111"));
    }

    /**
     * Integration test for {@link ValidateChangeLogTask#validateChangeLogs()}.
     * Should fail because the exclusions file path does not exist.
     */
    @Test
    public void testExecuteWrongExclusionFilePathFailure() {
        task.getPathToExclusionsFile().set(new File("nonexistent/exclusions.xml111"));

        final GradleException exception =
                assertThrows(GradleException.class, task::validateChangeLogs);
        assertTrue(exception.getMessage().startsWith("Invalid path:"));
        assertTrue(exception.getMessage().contains("exclusions.xml111"));
    }

    /**
     * Integration test for {@link ValidateChangeLogTask#validateChangeLogs()}.
     * Should not fail because the rule set is empty.
     *
     * @throws URISyntaxException if a resource URL cannot be converted to a URI.
     */
    @Test
    public void testExecuteEmptyRulesSuccess() throws URISyntaxException {
        final ValidateChangeLogTask freshTask = ProjectBuilder.builder().build()
                .getTasks().create("task2", ValidateChangeLogTask.class);
        freshTask.getShouldFailBuild().set(true);
        freshTask.getShouldGenerateExclusions().set(false);
        freshTask.getChangeLogFormat().set("xml");
        freshTask.getPluginVersion().set("1.0");
        freshTask.getPathToRulesFile().set(resourceFile("io/github/htshame/failure/rules_empty.xml"));
        freshTask.getChangeLogDirectory().set(resourceFile("db/xml"));

        assertDoesNotThrow(freshTask::validateChangeLogs);
    }

    private File resourceFile(final String path) throws URISyntaxException {
        final URL url = getClass().getClassLoader().getResource(path);
        if (url == null) {
            return new File(path);
        }
        return new File(url.toURI());
    }
}
