package io.github.htshame;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

/**
 * DSL extension for the Naming Convention Liquibase Gradle Plugin.
 * <p>
 * Configure validation in your {@code build.gradle}:
 * <pre>
 * validateLiquibaseChangeLog {
 *     pathToRulesFile = file("src/main/resources/liquibaseNaming/rules.xml")
 *     changeLogDirectory = file("src/main/resources/db")
 * }
 * </pre>
 */
public abstract class ValidateChangeLogExtension {

    /**
     * Default constructor.
     */
    public ValidateChangeLogExtension() {

    }

    /**
     * Path to the XML file with naming-convention rules. <b>Required.</b>
     *
     * @return file property
     */
    public abstract RegularFileProperty getPathToRulesFile();

    /**
     * Path to the XML file with exclusions. <b>Optional.</b>
     *
     * @return file property
     */
    public abstract RegularFileProperty getPathToExclusionsFile();

    /**
     * Path to the directory containing Liquibase changeLog files. <b>Required.</b>
     *
     * @return directory property
     */
    public abstract DirectoryProperty getChangeLogDirectory();

    /**
     * Whether the build should fail when violations are found.
     * <br>
     * Default: {@code true}.
     *
     * @return boolean property
     */
    public abstract Property<Boolean> getShouldFailBuild();

    /**
     * Liquibase changeLog file format.
     * <br>
     * Supported values: {@code xml}, {@code yaml}, {@code yml}, {@code json}.
     * <br>
     * Default: {@code xml}.
     *
     * @return string property
     */
    public abstract Property<String> getChangeLogFormat();

    /**
     * Whether the contents of the exclusions file should be generated to the log when the build fails.
     * <br>
     * Default: {@code false}.
     *
     * @return boolean property
     */
    public abstract Property<Boolean> getShouldGenerateExclusions();

    /**
     * Get rules file URL.
     *
     * @return url.
     */
    public abstract Property<String> getRulesFileUrl();

    /**
     * Get exclusions file URL.
     *
     * @return url.
     */
    public abstract Property<String> getExclusionsFileUrl();
}
