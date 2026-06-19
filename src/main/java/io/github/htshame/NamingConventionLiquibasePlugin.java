package io.github.htshame;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

/**
 * Gradle plugin entry point for the Naming Convention Liquibase Gradle Plugin.
 * <p>
 * Registers the {@code validateLiquibaseChangeLog} extension and task,
 * wires the task to the {@code check} lifecycle task (when the {@code base} plugin is present),
 * and sets default configuration values.
 */
public class NamingConventionLiquibasePlugin implements Plugin<Project> {

    /**
     * Applies the plugin to the given project.
     *
     * @param project the target Gradle project.
     */
    @Override
    public void apply(final Project project) {
        final ValidateChangeLogExtension extension = project.getExtensions()
                .create("validateLiquibaseChangeLog", ValidateChangeLogExtension.class);

        extension.getShouldFailBuild().convention(true);
        extension.getChangeLogFormat().convention("xml");
        extension.getShouldGenerateExclusions().convention(false);

        final TaskProvider<ValidateChangeLogTask> validateTask =
                project.getTasks().register(
                        "validateLiquibaseChangeLog",
                        ValidateChangeLogTask.class,
                        task -> {
                            task.setGroup("verification");
                            task.setDescription(
                                    "Validates Liquibase changeLog files against naming-convention rules.");
                            task.getPathToRulesFile().set(extension.getPathToRulesFile());
                            task.getPathToExclusionsFile().set(extension.getPathToExclusionsFile());
                            task.getChangeLogDirectory().set(extension.getChangeLogDirectory());
                            task.getShouldFailBuild().set(extension.getShouldFailBuild());
                            task.getChangeLogFormat().set(extension.getChangeLogFormat());
                            task.getShouldGenerateExclusions().set(extension.getShouldGenerateExclusions());
                            final Package pluginPackage = NamingConventionLiquibasePlugin.class.getPackage();
                            final String pluginVersion =
                                    pluginPackage != null ? pluginPackage.getImplementationVersion() : null;
                            task.getPluginVersion().set(pluginVersion != null ? pluginVersion : "unknown");
                        });

        project.getPlugins().withId("base", plugin ->
                project.getTasks().named("check").configure(check -> check.dependsOn(validateTask))
        );
    }
}
