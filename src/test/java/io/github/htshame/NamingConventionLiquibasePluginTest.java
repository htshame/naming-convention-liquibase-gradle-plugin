package io.github.htshame;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit/integration tests for {@link NamingConventionLiquibasePlugin}.
 * <p>
 * Verifies that the plugin wires the DSL extension and task correctly, applies default
 * convention values, and hooks into the {@code check} lifecycle task when the {@code base}
 * plugin is present.
 */
public class NamingConventionLiquibasePluginTest {

    private Project project;

    /**
     * Build a fresh Gradle project before each test.
     */
    @BeforeEach
    public void setUp() {
        project = ProjectBuilder.builder().build();
    }

    /**
     * Applying the plugin should register a {@link ValidateChangeLogExtension} under the
     * {@code validateLiquibaseChangeLog} name.
     */
    @Test
    public void testApplyCreatesExtension() {
        project.getPluginManager().apply(NamingConventionLiquibasePlugin.class);

        assertNotNull(project.getExtensions().findByName("validateLiquibaseChangeLog"));
        assertNotNull(project.getExtensions().findByType(ValidateChangeLogExtension.class));
    }

    /**
     * Applying the plugin should register a {@code validateLiquibaseChangeLog} task of type
     * {@link ValidateChangeLogTask}.
     */
    @Test
    public void testApplyRegistersTask() {
        project.getPluginManager().apply(NamingConventionLiquibasePlugin.class);

        assertNotNull(project.getTasks().findByName("validateLiquibaseChangeLog"));
        assertNotNull(project.getTasks().withType(ValidateChangeLogTask.class).findByName(
                "validateLiquibaseChangeLog"));
    }

    /**
     * The plugin should set {@code shouldFailBuild=true}, {@code changeLogFormat=xml} and
     * {@code shouldGenerateExclusions=false} as default convention values on the extension.
     */
    @Test
    public void testApplyDefaultConventions() {
        project.getPluginManager().apply(NamingConventionLiquibasePlugin.class);

        final ValidateChangeLogExtension ext =
                project.getExtensions().getByType(ValidateChangeLogExtension.class);
        assertEquals(true, ext.getShouldFailBuild().get());
        assertEquals("xml", ext.getChangeLogFormat().get());
        assertEquals(false, ext.getShouldGenerateExclusions().get());
    }

    /**
     * The task should be placed in the {@code verification} group and have a non-null description.
     */
    @Test
    public void testTaskGroupAndDescription() {
        project.getPluginManager().apply(NamingConventionLiquibasePlugin.class);

        final Task task = project.getTasks().getByName("validateLiquibaseChangeLog");
        assertEquals("verification", task.getGroup());
        assertNotNull(task.getDescription());
        assertFalse(task.getDescription().isEmpty());
    }

    /**
     * Scalar properties set on the extension ({@code shouldFailBuild}, {@code changeLogFormat},
     * {@code shouldGenerateExclusions}) should be lazily forwarded to the task.
     */
    @Test
    public void testTaskScalarPropertiesMappedFromExtension() {
        project.getPluginManager().apply(NamingConventionLiquibasePlugin.class);

        final ValidateChangeLogExtension ext =
                project.getExtensions().getByType(ValidateChangeLogExtension.class);
        ext.getShouldFailBuild().set(false);
        ext.getChangeLogFormat().set("yaml");
        ext.getShouldGenerateExclusions().set(true);

        final ValidateChangeLogTask task = (ValidateChangeLogTask)
                project.getTasks().getByName("validateLiquibaseChangeLog");
        assertEquals(false, task.getShouldFailBuild().get());
        assertEquals("yaml", task.getChangeLogFormat().get());
        assertEquals(true, task.getShouldGenerateExclusions().get());
    }

    /**
     * The {@code rulesFileUrl} and {@code exclusionsFileUrl} extension properties should be
     * forwarded to the task.
     */
    @Test
    public void testTaskUrlPropertiesMappedFromExtension() {
        project.getPluginManager().apply(NamingConventionLiquibasePlugin.class);

        final ValidateChangeLogExtension ext =
                project.getExtensions().getByType(ValidateChangeLogExtension.class);
        ext.getRulesFileUrl().set("http://example.com/rules.xml");
        ext.getExclusionsFileUrl().set("http://example.com/exclusions.xml");

        final ValidateChangeLogTask task = (ValidateChangeLogTask)
                project.getTasks().getByName("validateLiquibaseChangeLog");
        assertEquals("http://example.com/rules.xml", task.getRulesFileUrl().get());
        assertEquals("http://example.com/exclusions.xml", task.getExclusionsFileUrl().get());
    }

    /**
     * The task's {@code pluginVersion} should be set to a non-null, non-empty string.
     * When running from a plain classpath (no JAR manifest) the value is {@code "unknown"}.
     */
    @Test
    public void testPluginVersionIsSet() {
        project.getPluginManager().apply(NamingConventionLiquibasePlugin.class);

        final ValidateChangeLogTask task = (ValidateChangeLogTask)
                project.getTasks().getByName("validateLiquibaseChangeLog");
        assertNotNull(task.getPluginVersion().get());
        assertFalse(task.getPluginVersion().get().isEmpty());
    }

    /**
     * When the {@code base} plugin is applied before the naming-convention plugin the
     * {@code check} task should gain a dependency on {@code validateLiquibaseChangeLog}.
     */
    @Test
    public void testBaseAppliedFirstWiresCheckDependency() {
        project.getPluginManager().apply("base");
        project.getPluginManager().apply(NamingConventionLiquibasePlugin.class);

        final Task checkTask = project.getTasks().getByName("check");
        final Task validateTask = project.getTasks().getByName("validateLiquibaseChangeLog");
        assertTrue(checkTask.getTaskDependencies().getDependencies(checkTask).contains(validateTask));
    }

    /**
     * When the naming-convention plugin is applied before the {@code base} plugin the
     * {@code check} task should still gain a dependency on {@code validateLiquibaseChangeLog}.
     */
    @Test
    public void testNamingConventionAppliedFirstWiresCheckDependency() {
        project.getPluginManager().apply(NamingConventionLiquibasePlugin.class);
        project.getPluginManager().apply("base");

        final Task checkTask = project.getTasks().getByName("check");
        final Task validateTask = project.getTasks().getByName("validateLiquibaseChangeLog");
        assertTrue(checkTask.getTaskDependencies().getDependencies(checkTask).contains(validateTask));
    }

    /**
     * When the {@code base} plugin is not applied there should be no {@code check} task.
     */
    @Test
    public void testWithoutBasePluginNoCheckTask() {
        project.getPluginManager().apply(NamingConventionLiquibasePlugin.class);

        assertNull(project.getTasks().findByName("check"));
    }
}
