package org.alfresco.transform.docling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PdfMarkdownDockerIT {

	private static final Path DOCKER_CONTEXT = Path.of("target");
	private static final Path PDF_FIXTURE = Path.of("src/test/resources/sucre.pdf");
	private static final Path OUTPUT_DIRECTORY = Path.of("src/test/resources/output");
	private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(8);

	@Test
	void shouldTransformPdfToMarkdownUsingContainerImage() throws Exception {
		assertTrue(Files.exists(DOCKER_CONTEXT.resolve("Dockerfile")), "Expected target/Dockerfile. Run the Maven package phase before integration tests.");
		assertTrue(Files.exists(PDF_FIXTURE), "Missing PDF fixture: src/test/resources/sucre.pdf");

		String imageName = "becpg-transform-markdown-it:" + UUID.randomUUID();
		String containerName = "becpg-transform-markdown-it-" + UUID.randomUUID();

		buildImage(imageName);

		try {
			runContainer(imageName, containerName);
			int mappedPort = resolveMappedPort(containerName);
			waitForLiveEndpoint(containerName, mappedPort);

			HttpResponse<String> response = transformPdf(mappedPort);

			assertEquals(200, response.statusCode(), response.body());
			assertFalse(response.body().trim().isEmpty(), "Expected non-empty markdown output");
			writeTransformedOutput(response.body());
		} finally {
			removeContainer(containerName);
			removeImage(imageName);
		}
	}

	private void writeTransformedOutput(String markdown) throws IOException {
		Files.createDirectories(OUTPUT_DIRECTORY);
		Files.writeString(OUTPUT_DIRECTORY.resolve("sample.md"), markdown, StandardCharsets.UTF_8);
	}

	private void buildImage(String imageName) throws IOException, InterruptedException {
		execute(List.of("docker", "build", "-t", imageName, DOCKER_CONTEXT.toString()));
	}

	private void runContainer(String imageName, String containerName) throws IOException, InterruptedException {
		execute(List.of("docker", "run", "-d", "--rm", "--name", containerName, "-P", imageName));
	}

	private int resolveMappedPort(String containerName) throws IOException, InterruptedException {
		String portMapping = execute(List.of("docker", "port", containerName, "8090/tcp")).trim();
		String[] hostPort = portMapping.split(":");
		return Integer.parseInt(hostPort[hostPort.length - 1]);
	}

	private void waitForLiveEndpoint(String containerName, int mappedPort) throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		URI liveUri = URI.create("http://localhost:" + mappedPort + "/live");
		Instant deadline = Instant.now().plus(STARTUP_TIMEOUT);

		while (Instant.now().isBefore(deadline)) {
			try {
				HttpResponse<String> response = client.send(HttpRequest.newBuilder(liveUri).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
				if (response.statusCode() == 200) {
					return;
				}
			} catch (IOException ignored) {
				// The container may still be starting; keep polling until timeout.
			}
			Thread.sleep(1000);
		}

		String logs = execute(List.of("docker", "logs", containerName), true);
		throw new IllegalStateException("Container did not become healthy within " + STARTUP_TIMEOUT + ". Logs:\n" + logs);
	}

	private HttpResponse<String> transformPdf(int mappedPort) throws IOException, InterruptedException {
		String boundary = "----becpg-transform-boundary-" + System.nanoTime();
		URI transformUri = URI.create("http://localhost:" + mappedPort + "/transform");
		byte[] requestBody = buildMultipartBody(boundary, Files.readAllBytes(PDF_FIXTURE));

		HttpRequest request = HttpRequest.newBuilder(transformUri)
			.header("Accept", "text/markdown")
			.header("Content-Type", "multipart/form-data; boundary=" + boundary)
			.POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
			.build();

		return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	private void removeContainer(String containerName) throws IOException, InterruptedException {
		execute(List.of("docker", "rm", "-f", containerName), true);
	}

	private void removeImage(String imageName) throws IOException, InterruptedException {
		execute(List.of("docker", "rmi", "-f", imageName), true);
	}

	private String execute(List<String> command) throws IOException, InterruptedException {
		return execute(command, false);
	}

	private String execute(List<String> command, boolean ignoreExitCode) throws IOException, InterruptedException {
		Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		int exitCode = process.waitFor();
		if (exitCode != 0 && !ignoreExitCode) {
			throw new IllegalStateException("Command failed: " + String.join(" ", command) + "\n" + output);
		}
		return output;
	}

	private byte[] buildMultipartBody(String boundary, byte[] pdfBytes) {
		List<byte[]> bodyParts = new ArrayList<>();

		bodyParts.add(formField(boundary, "sourceMimetype", "application/pdf"));
		bodyParts.add(formField(boundary, "targetMimetype", "text/markdown"));
		bodyParts.add(fileField(boundary, "file", "sample.pdf", "application/pdf", pdfBytes));
		bodyParts.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

		int totalLength = bodyParts.stream().mapToInt(part -> part.length).sum();
		byte[] requestBody = new byte[totalLength];
		int offset = 0;
		for (byte[] part : bodyParts) {
			System.arraycopy(part, 0, requestBody, offset, part.length);
			offset += part.length;
		}

		return requestBody;
	}

	private byte[] formField(String boundary, String name, String value) {
		String field = "--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
				+ value + "\r\n";
		return field.getBytes(StandardCharsets.UTF_8);
	}

	private byte[] fileField(String boundary, String name, String filename, String contentType, byte[] content) {
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
}
