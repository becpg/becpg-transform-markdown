package org.alfresco.transform.docling.transformers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.alfresco.transform.base.executors.RuntimeExec;
import org.alfresco.transform.base.executors.RuntimeExec.ExecutionResult;
import org.alfresco.transform.exceptions.TransformException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests of the limit on concurrent PyMuPDF4LLM conversions, which protects
 * the container memory from several OCR conversions running at once.
 */
class Pymupdf4llmTransformerTest {

	private static final String SCRIPT_PATH = "/opt/test/convert.sh";
	private static final String TRANSFORM_COMMAND_FIELD = "transformCommand";
	private static final long LONG_TIMEOUT_MS = 10_000L;
	private static final long SHORT_TIMEOUT_MS = 200L;
	private static final int CONVERSIONS = 5;

	@TempDir
	private Path tempDir;

	private RuntimeExec transformCommand;

	private ExecutorService executor;

	@BeforeEach
	void setUp() {
		transformCommand = mock(RuntimeExec.class);
		executor = Executors.newFixedThreadPool(CONVERSIONS);
	}

	@AfterEach
	void tearDown() {
		executor.shutdownNow();
	}

	@Test
	void runsAtMostTheConfiguredNumberOfConversionsAtOnce() throws Exception {
		Pymupdf4llmTransformer transformer = newTransformer(LONG_TIMEOUT_MS, 2);
		AtomicInteger running = new AtomicInteger();
		AtomicInteger peak = new AtomicInteger();
		CountDownLatch release = new CountDownLatch(1);
		ExecutionResult success = result(0);
		when(transformCommand.execute(anyMap(), anyLong())).thenAnswer(invocation -> {
			peak.accumulateAndGet(running.incrementAndGet(), Math::max);
			release.await(LONG_TIMEOUT_MS, TimeUnit.MILLISECONDS);
			running.decrementAndGet();
			return success;
		});

		List<Future<?>> conversions = new ArrayList<>();
		for (int i = 0; i < CONVERSIONS; i++) {
			File target = writeTarget("out" + i + ".md");
			conversions.add(executor.submit(() -> transform(transformer, target)));
		}
		waitUntil(() -> running.get() == 2);
		Thread.sleep(SHORT_TIMEOUT_MS);
		assertEquals(2, running.get(), "Only two conversions must run while the slots are taken");

		release.countDown();
		for (Future<?> conversion : conversions) {
			assertDoesNotThrow(() -> conversion.get(LONG_TIMEOUT_MS, TimeUnit.MILLISECONDS));
		}
		assertEquals(2, peak.get());
	}

	@Test
	void rejectsAConversionWhenNoSlotIsFreedWithinTheTimeout() throws Exception {
		Pymupdf4llmTransformer transformer = newTransformer(SHORT_TIMEOUT_MS, 1);
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		ExecutionResult success = result(0);
		when(transformCommand.execute(anyMap(), anyLong())).thenAnswer(invocation -> {
			started.countDown();
			release.await(LONG_TIMEOUT_MS, TimeUnit.MILLISECONDS);
			return success;
		});
		File firstTarget = writeTarget("first.md");
		Future<?> first = executor.submit(() -> transform(transformer, firstTarget));
		assertTrue(started.await(LONG_TIMEOUT_MS, TimeUnit.MILLISECONDS));

		File secondTarget = writeTarget("second.md");
		TransformException exception = assertThrows(TransformException.class,
				() -> transform(transformer, secondTarget));
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());

		release.countDown();
		assertDoesNotThrow(() -> first.get(LONG_TIMEOUT_MS, TimeUnit.MILLISECONDS));
	}

	@Test
	void freesTheSlotWhenAConversionFails() throws IOException {
		Pymupdf4llmTransformer transformer = newTransformer(SHORT_TIMEOUT_MS, 1);
		ExecutionResult failure = result(2);
		ExecutionResult success = result(0);
		when(transformCommand.execute(anyMap(), anyLong())).thenReturn(failure, success);
		File target = writeTarget("out.md");

		TransformException exception = assertThrows(TransformException.class, () -> transform(transformer, target));
		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		assertDoesNotThrow(() -> transform(transformer, target));
	}

	@Test
	void rejectsANonPositiveConcurrencyLimit() {
		assertThrows(IllegalArgumentException.class, () -> new Pymupdf4llmTransformer(SCRIPT_PATH, LONG_TIMEOUT_MS, 0));
	}

	private Pymupdf4llmTransformer newTransformer(long timeoutMs, int maxConcurrent) {
		Pymupdf4llmTransformer transformer = new Pymupdf4llmTransformer(SCRIPT_PATH, timeoutMs, maxConcurrent);
		ReflectionTestUtils.setField(transformer, TRANSFORM_COMMAND_FIELD, transformCommand);
		return transformer;
	}

	private void transform(Pymupdf4llmTransformer transformer, File target) {
		transformer.transform("image/png", "text/markdown", Map.of(), target, target, null);
	}

	private ExecutionResult result(int exitValue) {
		ExecutionResult result = mock(ExecutionResult.class);
		when(result.getExitValue()).thenReturn(exitValue);
		when(result.getStdErr()).thenReturn("");
		return result;
	}

	private File writeTarget(String name) throws IOException {
		Path target = tempDir.resolve(name);
		Files.writeString(target, "# title");
		return target.toFile();
	}

	private void waitUntil(BooleanSupplier condition) throws InterruptedException {
		long deadline = System.currentTimeMillis() + LONG_TIMEOUT_MS;
		while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
			Thread.sleep(10);
		}
		assertTrue(condition.getAsBoolean(), "Condition not reached in time");
	}

}
