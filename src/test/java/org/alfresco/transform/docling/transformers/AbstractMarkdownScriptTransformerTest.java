package org.alfresco.transform.docling.transformers;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.alfresco.transform.base.executors.RuntimeExec;
import org.alfresco.transform.base.executors.RuntimeExec.ExecutionResult;
import org.alfresco.transform.exceptions.TransformException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests of the command building and result checks shared by the Markdown
 * script transformers.
 */
class AbstractMarkdownScriptTransformerTest {

	private static final String SCRIPT_PATH = "/opt/test/convert.sh";
	private static final String TRANSFORM_COMMAND_FIELD = "transformCommand";
	private static final String SOURCE_PATH = "/tmp/in.pdf";
	private static final String TARGET_PATH = "/tmp/out.md";
	private static final Map<String, String> NO_PROPERTIES = Map.of();

	@TempDir
	private Path tempDir;

	private Pymupdf4llmTransformer transformer;

	private RuntimeExec transformCommand;

	@BeforeEach
	void setUp() {
		transformer = new Pymupdf4llmTransformer();
		ReflectionTestUtils.setField(transformer, "convertScriptPath", SCRIPT_PATH);
		transformCommand = mock(RuntimeExec.class);
		ReflectionTestUtils.setField(transformer, TRANSFORM_COMMAND_FIELD, transformCommand);
	}

	@Test
	void transformCommandRunsTheConfiguredScript() {
		RuntimeExec command = transformer.createTransformCommand();
		String[] arguments = command.getCommand(Map.of("source", SOURCE_PATH, "target", TARGET_PATH, "options", ""));
		assertEquals(SCRIPT_PATH, arguments[0]);
		assertEquals(SOURCE_PATH, arguments[1]);
		assertEquals(TARGET_PATH, arguments[2]);
	}

	@Test
	void checkCommandAsksTheToolNamedAfterTheTransformerForItsVersion() {
		assertArrayEquals(new String[] { "pymupdf4llm", "--version" }, transformer.createCheckCommand().getCommand());
		MarkitdownTransformer markitdown = new MarkitdownTransformer();
		assertArrayEquals(new String[] { "markitdown", "--version" }, markitdown.createCheckCommand().getCommand());
	}

	@Test
	void runRejectsANonZeroExitCodeAsABadRequest() throws IOException {
		givenExitValue(2);
		File targetFile = writeTarget("# title");
		TransformException exception = assertThrows(TransformException.class,
				() -> transformer.run(NO_PROPERTIES, targetFile, 1000L));
		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
	}

	@Test
	void runRejectsAnEmptyOutputAsAnInternalError() throws IOException {
		givenExitValue(0);
		File targetFile = writeTarget("");
		TransformException exception = assertThrows(TransformException.class,
				() -> transformer.run(NO_PROPERTIES, targetFile, 1000L));
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
	}

	@Test
	void runRejectsAMissingOutputAsAnInternalError() {
		givenExitValue(0);
		File targetFile = tempDir.resolve("missing.md").toFile();
		TransformException exception = assertThrows(TransformException.class,
				() -> transformer.run(NO_PROPERTIES, targetFile, 1000L));
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
	}

	@Test
	void runAcceptsANonEmptyOutputAndTreatsANullTimeoutAsNone() throws IOException {
		givenExitValue(0);
		File targetFile = writeTarget("# title");
		assertDoesNotThrow(() -> transformer.run(NO_PROPERTIES, targetFile, null));
		verify(transformCommand).execute(anyMap(), eq(0L));
	}

	@Test
	void transformForwardsTheConfiguredTimeout() throws IOException {
		ReflectionTestUtils.setField(transformer, "timeoutMs", 1000L);
		givenExitValue(0);
		File targetFile = writeTarget("# title");
		transformer.transform("application/pdf", "text/markdown", Map.of(), targetFile, targetFile, null);
		verify(transformCommand).execute(anyMap(), eq(1000L));
	}

	@Test
	void runRejectsATargetOutsideTheTemporaryDirectory() {
		File targetFile = new File("/etc/out.md");
		assertThrows(TransformException.class, () -> transformer.run(NO_PROPERTIES, targetFile, 1000L));
	}

	private void givenExitValue(int exitValue) {
		ExecutionResult result = mock(ExecutionResult.class);
		when(result.getExitValue()).thenReturn(exitValue);
		when(result.getStdErr()).thenReturn("x".repeat(2000));
		when(transformCommand.execute(anyMap(), eq(0L))).thenReturn(result);
		when(transformCommand.execute(anyMap(), eq(1000L))).thenReturn(result);
	}

	private File writeTarget(String content) throws IOException {
		Path target = tempDir.resolve("out.md");
		Files.writeString(target, content);
		return target.toFile();
	}

}
