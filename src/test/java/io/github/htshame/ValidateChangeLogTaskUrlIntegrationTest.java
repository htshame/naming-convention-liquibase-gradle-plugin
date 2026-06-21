package io.github.htshame;

import com.sun.net.httpserver.HttpServer;
import org.gradle.api.GradleException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for the URL-based {@code rulesFileUrl} and {@code exclusionsFileUrl}
 * configuration options of {@link ValidateChangeLogTask}.
 * <p>
 * An embedded HTTP server is started once for the test class so the HTTP-only URL-loading
 * path in the core library can be exercised without an external network.
 * <p>
 * Covered cases: only URL set; both file-path and URL set; neither option set;
 * malformed URL; and URL-based exclusions.
 */
public class ValidateChangeLogTaskUrlIntegrationTest {

    private static final int HTTP_OK = 200;

    private static HttpServer httpServer;
    private static int port;

    private ValidateChangeLogTask task;

    /**
     * Start an embedded HTTP server that serves the rules and exclusions XML fixtures.
     *
     * @throws IOException        if the server socket cannot be bound.
     * @throws URISyntaxException if a classpath resource URL cannot be resolved.
     */
    @BeforeAll
    public static void startHttpServer() throws IOException, URISyntaxException {
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        serveResource(httpServer, "/rules.xml", "rules.xml");
        serveResource(httpServer, "/exclusions.xml", "exclusions.xml");
        httpServer.start();
        port = httpServer.getAddress().getPort();
    }

    /**
     * Stop the embedded HTTP server after all tests have completed.
     */
    @AfterAll
    public static void stopHttpServer() {
        httpServer.stop(0);
    }

    /**
     * Initialise a task configured with {@code rulesFileUrl} (not {@code pathToRulesFile})
     * before each test.
     *
     * @throws URISyntaxException if a classpath resource URL cannot be resolved.
     */
    @BeforeEach
    public void setUp() throws URISyntaxException {
        task = ProjectBuilder.builder().build()
                .getTasks().create("validateLiquibaseChangeLog", ValidateChangeLogTask.class);
        task.getRulesFileUrl().set(rulesUrl());
        task.getPathToExclusionsFile().set(resourceFilePath("exclusions.xml"));
        task.getChangeLogDirectory().set(resourceFilePath("db/xml"));
        task.getShouldFailBuild().set(true);
        task.getShouldGenerateExclusions().set(true);
        task.getChangeLogFormat().set("xml");
        task.getPluginVersion().set("1.0");
    }

    /**
     * Integration test: only {@code rulesFileUrl} is configured (no {@code pathToRulesFile}).
     * The rules XML is fetched over HTTP; expects 32 violations to be found.
     */
    @Test
    public void testRulesFileUrlOnly() {
        final GradleException exception =
                assertThrows(GradleException.class, task::validateChangeLogs);
        assertEquals("Validation failed: 32 violation(s) found.", exception.getMessage());
    }

    /**
     * Integration test: both {@code pathToRulesFile} and {@code rulesFileUrl} are configured.
     * Expects a {@link GradleException} because exactly one must be present.
     *
     * @throws URISyntaxException if a classpath resource URL cannot be resolved.
     */
    @Test
    public void testBothPathToRulesFileAndRulesFileUrlFails() throws URISyntaxException {
        task.getPathToRulesFile().set(resourceFilePath("rules.xml"));

        final GradleException exception =
                assertThrows(GradleException.class, task::validateChangeLogs);
        assertEquals(
                "Exactly one of 'pathToRulesFile' or 'rulesFileUrl' parameters must be present",
                exception.getMessage());
    }

    /**
     * Integration test: neither {@code pathToRulesFile} nor {@code rulesFileUrl} is configured.
     * Expects a {@link GradleException} because exactly one must be present.
     *
     * @throws URISyntaxException if a classpath resource URL cannot be resolved.
     */
    @Test
    public void testNeitherPathToRulesFileNorRulesFileUrlFails() throws URISyntaxException {
        final ValidateChangeLogTask freshTask = ProjectBuilder.builder().build()
                .getTasks().create("task2", ValidateChangeLogTask.class);
        freshTask.getChangeLogDirectory().set(resourceFilePath("db/xml"));
        freshTask.getShouldFailBuild().set(true);
        freshTask.getShouldGenerateExclusions().set(false);
        freshTask.getChangeLogFormat().set("xml");
        freshTask.getPluginVersion().set("1.0");

        final GradleException exception =
                assertThrows(GradleException.class, freshTask::validateChangeLogs);
        assertEquals(
                "Exactly one of 'pathToRulesFile' or 'rulesFileUrl' parameters must be present",
                exception.getMessage());
    }

    /**
     * Integration test: {@code rulesFileUrl} is set to a malformed URL string.
     * Expects a {@link GradleException} reporting the URL parse failure.
     */
    @Test
    public void testInvalidRulesFileUrl() {
        task.getRulesFileUrl().set("not-a-valid-url");

        final GradleException exception =
                assertThrows(GradleException.class, task::validateChangeLogs);
        assertEquals("Error parsing URL: not-a-valid-url", exception.getMessage());
    }

    /**
     * Integration test: both {@code pathToExclusionsFile} and {@code exclusionsFileUrl} are set.
     * Expects a {@link GradleException} because only one may be present at a time.
     */
    @Test
    public void testBothExclusionsFileAndExclusionsFileUrlFails() {
        task.getExclusionsFileUrl().set(exclusionsUrl());

        final GradleException exception =
                assertThrows(GradleException.class, task::validateChangeLogs);
        assertEquals(
                "Only one of 'pathToExclusionsFile' or 'exclusionsFileUrl' parameters must be present",
                exception.getMessage());
    }

    /**
     * Integration test: only {@code exclusionsFileUrl} is configured (no {@code pathToExclusionsFile}).
     * Both rules and exclusions are loaded via HTTP; expects 32 violations to be found.
     *
     * @throws URISyntaxException if a classpath resource URL cannot be resolved.
     */
    @Test
    public void testExclusionsFileUrlOnly() throws URISyntaxException {
        final ValidateChangeLogTask freshTask = ProjectBuilder.builder().build()
                .getTasks().create("task3", ValidateChangeLogTask.class);
        freshTask.getRulesFileUrl().set(rulesUrl());
        freshTask.getExclusionsFileUrl().set(exclusionsUrl());
        freshTask.getChangeLogDirectory().set(resourceFilePath("db/xml"));
        freshTask.getShouldFailBuild().set(true);
        freshTask.getShouldGenerateExclusions().set(true);
        freshTask.getChangeLogFormat().set("xml");
        freshTask.getPluginVersion().set("1.0");

        final GradleException exception =
                assertThrows(GradleException.class, freshTask::validateChangeLogs);
        assertEquals("Validation failed: 32 violation(s) found.", exception.getMessage());
    }

    private static void serveResource(final HttpServer server, final String contextPath,
            final String classpathResource) throws IOException, URISyntaxException {
        final byte[] content = Files.readAllBytes(resourceFilePath(classpathResource).toPath());
        server.createContext(contextPath, exchange -> {
            exchange.sendResponseHeaders(HTTP_OK, content.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
        });
    }

    private static File resourceFilePath(final String path) throws URISyntaxException {
        final URL url = ValidateChangeLogTaskUrlIntegrationTest.class
                .getClassLoader().getResource(path);
        if (url == null) {
            return new File(path);
        }
        return new File(url.toURI());
    }

    private static String rulesUrl() {
        return "http://localhost:" + port + "/rules.xml";
    }

    private static String exclusionsUrl() {
        return "http://localhost:" + port + "/exclusions.xml";
    }
}
