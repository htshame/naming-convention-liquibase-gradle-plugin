package io.github.htshame;

import io.github.htshame.core.PluginConfig;
import io.github.htshame.core.ValidateChangeLogService;
import io.github.htshame.enums.ChangeLogFormatEnum;
import io.github.htshame.exception.ValidateChangeLogException;
import io.github.htshame.log.PluginLogger;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.io.File;

/**
 * {@code validateLiquibaseChangeLog} task executor.
 * <p>
 * Validates Liquibase changeLog files against a set of naming-convention rules.
 * This task is wired to the {@code check} lifecycle task by
 * {@link NamingConventionLiquibasePlugin}.
 */
public abstract class ValidateChangeLogTask extends DefaultTask {

    private static final String INVALID_PATH = "Invalid path: ";

    /**
     * Path to the XML file with rules. <b>Required.</b>
     *
     * @return file property
     */
    @InputFile
    public abstract RegularFileProperty getPathToRulesFile();

    /**
     * Path to the XML file with exclusions. <b>Optional.</b>
     *
     * @return file property
     */
    @Optional
    @InputFile
    public abstract RegularFileProperty getPathToExclusionsFile();

    /**
     * Path to the directory containing Liquibase changeLog files. <b>Required.</b>
     *
     * @return directory property
     */
    @InputDirectory
    public abstract DirectoryProperty getChangeLogDirectory();

    /**
     * Flag that determines whether the build will fail when violations are found.
     * Default value is {@code true}.
     *
     * @return boolean property
     */
    @Input
    public abstract Property<Boolean> getShouldFailBuild();

    /**
     * ChangeLog files format (xml, yaml, yml, json).
     * Default value is {@code xml}.
     *
     * @return string property
     */
    @Input
    public abstract Property<String> getChangeLogFormat();

    /**
     * Flag that determines whether the exclusions file content will be generated when the build fails.
     * Default value is {@code false}.
     *
     * @return boolean property
     */
    @Input
    public abstract Property<Boolean> getShouldGenerateExclusions();

    /**
     * The version of this plugin, embedded via the JAR manifest at build time.
     *
     * @return string property
     */
    @Input
    public abstract Property<String> getPluginVersion();

    /**
     * Validates the Liquibase changeLog files using the configured rules and exclusions.
     * <p>
     * Fails the build (throws {@link GradleException}) when violations are found and
     * {@code shouldFailBuild} is {@code true}.
     */
    @TaskAction
    public void validateChangeLogs() {
        validateInput();
        configReminder();

        final File exclusionsFile = getPathToExclusionsFile().isPresent()
                ? getPathToExclusionsFile().getAsFile().get() : null;

        final PluginConfig config = new PluginConfig(
                getChangeLogFormat().get(),
                getPathToRulesFile().getAsFile().get(),
                exclusionsFile,
                getChangeLogDirectory().getAsFile().get(),
                getShouldGenerateExclusions().get(),
                getPluginVersion().get());

        final PluginLogger logger = preparePluginLogger();
        final ValidateChangeLogService validateChangeLogService =
                new ValidateChangeLogService(logger, config);

        try {
            validateChangeLogService.execute();
        } catch (ValidateChangeLogException e) {
            if (getShouldFailBuild().get()) {
                throw new GradleException(e.getMessage());
            }
            logger.warn(e.getMessage()
                    + " Build will not fail because shouldFailBuild=false");
        }
    }

    /**
     * Analyze input parameters and remind a user that parameters exist.
     */
    private void configReminder() {
        if (!getPathToExclusionsFile().isPresent()) {
            getLogger().info("Parameter 'pathToExclusionsFile' is not set. Great job!");
        }
        if (getShouldFailBuild().get()) {
            getLogger().info("Parameter 'shouldFailBuild' is set to 'true' (default value) or not defined.");
        }
        if ("xml".equals(getChangeLogFormat().get())) {
            getLogger().info("Parameter 'changeLogFormat' is set to 'xml' (default value) or not defined.");
        }
        if (!getShouldGenerateExclusions().get()) {
            getLogger().info(
                    "Parameter 'shouldGenerateExclusions' is set to 'false' (default value) or not defined.");
        }
    }

    /**
     * Prepare plugin logger.
     *
     * @return plugin logger.
     */
    private PluginLogger preparePluginLogger() {
        return new PluginLogger() {
            @Override
            public void info(final String message) {
                ValidateChangeLogTask.this.getLogger().info(message);
            }

            @Override
            public void warn(final String message) {
                ValidateChangeLogTask.this.getLogger().warn(message);
            }

            @Override
            public void error(final String message) {
                ValidateChangeLogTask.this.getLogger().error(message);
            }

            @Override
            public void error(final String message, final Exception e) {
                ValidateChangeLogTask.this.getLogger().error(message, e);
            }
        };
    }

    /**
     * Validate incoming parameters.
     * <p>
     * - changeLog directory exists;
     * <br>
     * - XML rules file is present;
     * <br>
     * - XML exclusions file exists if provided;
     * <br>
     * - changeLog format is supported;
     */
    private void validateInput() {
        final File changeLogDir = getChangeLogDirectory().getAsFile().get();
        final File rulesFile = getPathToRulesFile().getAsFile().get();

        if (!changeLogDir.isDirectory()) {
            throw new GradleException(INVALID_PATH + changeLogDir);
        }
        if (!rulesFile.exists()) {
            throw new GradleException(INVALID_PATH + rulesFile);
        }
        if (getPathToExclusionsFile().isPresent()) {
            final File exclusionsFile = getPathToExclusionsFile().getAsFile().get();
            if (!exclusionsFile.exists()) {
                throw new GradleException(INVALID_PATH + exclusionsFile);
            }
        }
        try {
            ChangeLogFormatEnum.fromValue(getChangeLogFormat().get().toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new GradleException(
                    "ChangeLog format [" + getChangeLogFormat().get() + "] is not supported");
        }
    }
}
