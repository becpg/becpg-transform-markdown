package org.alfresco.transform.docling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Docker integration test that verifies every supported transformation
 * declared in {@code becpg_transform_markdown_config.json}.
 *
 * <p>The test builds a Docker image from {@code target/}, starts a single
 * container, and runs one parameterized test per source/target combination.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DockerTransformIT
{

    private static final Path DOCKER_CONTEXT = Path.of("target");
    private static final Path RESOURCES = Path.of("src/test/resources");
    private static final Path OUTPUT_DIRECTORY = RESOURCES.resolve("output");
    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(8);

    private String imageName;
    private String containerName;
    private int mappedPort;

    @BeforeAll
    void startContainer() throws Exception
    {
        assertTrue(Files.exists(DOCKER_CONTEXT.resolve("Dockerfile")),
                "Expected target/Dockerfile. Run the Maven package phase before integration tests.");

        imageName = "becpg-transform-markdown-it:" + UUID.randomUUID();
        containerName = "becpg-transform-markdown-it-" + UUID.randomUUID();

        buildImage(imageName);
        runContainer(imageName, containerName);
        mappedPort = resolveMappedPort(containerName);
        waitForLiveEndpoint(containerName, mappedPort);

        Files.createDirectories(OUTPUT_DIRECTORY);
    }

    @AfterAll
    void stopContainer() throws Exception
    {
        removeContainer(containerName);
        removeImage(imageName);
    }

    /**
     * Provides all supported transformation combinations.
     *
     * <p>Each argument set contains: fixture filename, source mimetype,
     * target mimetype, and an output label for the result file.</p>
     *
     * @return stream of test arguments covering every config entry
     */
    static Stream<Arguments> transformations()
    {
        return Stream.of(
                // markitdown: Office documents -> Markdown
                Arguments.of("sample.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "text/markdown", "docx-to-md"),
                Arguments.of("sample.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "text/x-markdown", "docx-to-xmd"),
                Arguments.of("probe.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "text/markdown", "xlsx-to-md"),
                Arguments.of("probe.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "text/x-markdown", "xlsx-to-xmd"),
                Arguments.of("sample.pptx",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "text/markdown", "pptx-to-md"),
                Arguments.of("sample.pptx",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "text/x-markdown", "pptx-to-xmd"),

                // markitdown: Web formats -> Markdown
                Arguments.of("sample.html", "text/html",
                        "text/markdown", "html-to-md"),
                Arguments.of("sample.html", "text/html",
                        "text/x-markdown", "html-to-xmd"),
                Arguments.of("sample.xhtml", "application/xhtml+xml",
                        "text/markdown", "xhtml-to-md"),
                Arguments.of("sample.xhtml", "application/xhtml+xml",
                        "text/x-markdown", "xhtml-to-xmd"),

                // markitdown: CSV -> Markdown
                Arguments.of("sample.csv", "text/csv",
                        "text/markdown", "csv-to-md"),
                Arguments.of("sample.csv", "text/csv",
                        "text/x-markdown", "csv-to-xmd"),

                // markitdown: Images -> Markdown
                Arguments.of("sample.png", "image/png",
                        "text/markdown", "png-to-md"),
                Arguments.of("sample.png", "image/png",
                        "text/x-markdown", "png-to-xmd"),
                Arguments.of("sample.jpeg", "image/jpeg",
                        "text/markdown", "jpeg-to-md"),
                Arguments.of("sample.jpeg", "image/jpeg",
                        "text/x-markdown", "jpeg-to-xmd"),
                Arguments.of("sample.tiff", "image/tiff",
                        "text/markdown", "tiff-to-md"),
                Arguments.of("sample.tiff", "image/tiff",
                        "text/x-markdown", "tiff-to-xmd"),
                Arguments.of("sample.bmp", "image/bmp",
                        "text/markdown", "bmp-to-md"),
                Arguments.of("sample.bmp", "image/bmp",
                        "text/x-markdown", "bmp-to-xmd"),

                // md2pdf: Markdown -> PDF
                Arguments.of("sample.md", "text/markdown",
                        "application/pdf", "md-to-pdf"),
                Arguments.of("sample.md", "text/x-markdown",
                        "application/pdf", "xmd-to-pdf"),

                // pymupdf4llm: PDF -> Markdown
                Arguments.of("sample.pdf", "application/pdf",
                        "text/markdown", "pdf-to-md"),
                Arguments.of("sample.pdf", "application/pdf",
                        "text/x-markdown", "pdf-to-xmd")
        );
    }

    /**
     * Tests a single transformation by posting a multipart request to the
     * running container and verifying a successful, non-empty response.
     *
     * @param fixture        the test fixture filename in {@code src/test/resources}
     * @param sourceMimetype the source MIME type
     * @param targetMimetype the target MIME type
     * @param outputLabel    label used for the output file name
     * @throws Exception if the HTTP request or assertion fails
     */
    @ParameterizedTest(name = "{3}")
    @MethodSource("transformations")
    void shouldTransform(String fixture, String sourceMimetype, String targetMimetype,
                         String outputLabel) throws Exception
    {
        Path fixturePath = RESOURCES.resolve(fixture);
        assertTrue(Files.exists(fixturePath), "Missing fixture: " + fixturePath);

        HttpResponse<byte[]> response = postTransform(mappedPort, fixturePath,
                fixture, sourceMimetype, targetMimetype);

        assertEquals(200, response.statusCode(),
                "Transform " + outputLabel + " failed: " + new String(response.body(), StandardCharsets.UTF_8));
        assertTrue(response.body().length > 0,
                "Expected non-empty output for " + outputLabel);

        String extension = targetMimetype.contains("pdf") ? ".pdf" : ".md";
        Files.write(OUTPUT_DIRECTORY.resolve(outputLabel + extension), response.body());
    }

    // ---- Docker lifecycle helpers ----

    private void buildImage(String image) throws IOException, InterruptedException
    {
        execute(List.of("docker", "build", "-t", image, DOCKER_CONTEXT.toString()));
    }

    private void runContainer(String image, String container) throws IOException, InterruptedException
    {
        execute(List.of("docker", "run", "-d", "--rm", "--name", container, "-P", image));
    }

    private int resolveMappedPort(String container) throws IOException, InterruptedException
    {
        String portMapping = execute(List.of("docker", "port", container, "8090/tcp")).trim();
        String[] hostPort = portMapping.split(":");
        return Integer.parseInt(hostPort[hostPort.length - 1]);
    }

    private void waitForLiveEndpoint(String container, int port) throws Exception
    {
        HttpClient client = HttpClient.newHttpClient();
        URI liveUri = URI.create("http://localhost:" + port + "/live");
        Instant deadline = Instant.now().plus(STARTUP_TIMEOUT);

        while (Instant.now().isBefore(deadline))
        {
            try
            {
                HttpResponse<String> response = client.send(
                        HttpRequest.newBuilder(liveUri).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() == 200)
                {
                    return;
                }
            }
            catch (IOException ignored)
            {
                // Container may still be starting; keep polling.
            }
            Thread.sleep(1000);
        }

        String logs = execute(List.of("docker", "logs", container), true);
        throw new IllegalStateException(
                "Container did not become healthy within " + STARTUP_TIMEOUT + ". Logs:\n" + logs);
    }

    private void removeContainer(String container) throws IOException, InterruptedException
    {
        execute(List.of("docker", "rm", "-f", container), true);
    }

    private void removeImage(String image) throws IOException, InterruptedException
    {
        execute(List.of("docker", "rmi", "-f", image), true);
    }

    // ---- HTTP helpers ----

    private HttpResponse<byte[]> postTransform(int port, Path file, String filename,
                                               String sourceMimetype, String targetMimetype)
            throws IOException, InterruptedException
    {
        String boundary = "----becpg-transform-boundary-" + System.nanoTime();
        URI transformUri = URI.create("http://localhost:" + port + "/transform");
        byte[] requestBody = buildMultipartBody(boundary, Files.readAllBytes(file),
                filename, sourceMimetype, targetMimetype);

        HttpRequest request = HttpRequest.newBuilder(transformUri)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .timeout(Duration.ofMinutes(5))
                .build();

        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private byte[] buildMultipartBody(String boundary, byte[] fileBytes,
                                      String filename, String sourceMimetype, String targetMimetype)
    {
        List<byte[]> parts = new ArrayList<>();
        parts.add(formField(boundary, "sourceMimetype", sourceMimetype));
        parts.add(formField(boundary, "targetMimetype", targetMimetype));
        parts.add(fileField(boundary, "file", filename, sourceMimetype, fileBytes));
        parts.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        int totalLength = parts.stream().mapToInt(p -> p.length).sum();
        byte[] body = new byte[totalLength];
        int offset = 0;
        for (byte[] part : parts)
        {
            System.arraycopy(part, 0, body, offset, part.length);
            offset += part.length;
        }
        return body;
    }

    private byte[] formField(String boundary, String name, String value)
    {
        String field = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n";
        return field.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] fileField(String boundary, String name, String filename,
                             String contentType, byte[] content)
    {
        byte[] header = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] footer = "\r\n".getBytes(StandardCharsets.UTF_8);
        byte[] field = new byte[header.length + content.length + footer.length];

        System.arraycopy(header, 0, field, 0, header.length);
        System.arraycopy(content, 0, field, header.length, content.length);
        System.arraycopy(footer, 0, field, header.length + content.length, footer.length);

        return field;
    }

    // ---- Process helpers ----

    private String execute(List<String> command) throws IOException, InterruptedException
    {
        return execute(command, false);
    }

    private String execute(List<String> command, boolean ignoreExitCode)
            throws IOException, InterruptedException
    {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0 && !ignoreExitCode)
        {
            throw new IllegalStateException(
                    "Command failed: " + String.join(" ", command) + "\n" + output);
        }
        return output;
    }
}
