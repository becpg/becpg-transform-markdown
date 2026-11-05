/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2023 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * -
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 * -
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * -
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * -
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.transform.docling.transformers;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.exceptions.TransformException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Transformer that uses PyMuPDF4LLM to convert PDF documents and images to
 * Markdown. Pages without a text layer, such as scans, outlined text or images,
 * are read with the RapidOCR engine installed in the Docker image.
 *
 * <p>An OCR conversion uses about 1 GB of memory, so the number of conversions
 * running at the same time is limited: the next requests wait for a free slot.</p>
 */
@Component
public class Pymupdf4llmTransformer extends AbstractMarkdownScriptTransformer {

	private static final Log logger = LogFactory.getLog(Pymupdf4llmTransformer.class);

	private static final String TRANSFORMER_NAME = "pymupdf4llm";
	private static final String TOOL_LABEL = "PyMuPDF4LLM";

	private final Semaphore conversionSlots;

	private final long slotWaitMs;

	/**
	 * Creates the PyMuPDF4LLM transformer from its configuration.
	 *
	 * @param scriptPath the absolute path of the PyMuPDF4LLM wrapper script
	 * @param timeoutMs the maximum duration of one conversion, in milliseconds, also
	 *                  used as the maximum wait for a free conversion slot
	 * @param maxConcurrent the maximum number of conversions running at the same time
	 * @throws IllegalArgumentException if {@code maxConcurrent} is not positive
	 */
	public Pymupdf4llmTransformer(@Value("${transform.core.pymupdf4llm.script.path}") String scriptPath,
			@Value("${transform.core.pymupdf4llm.timeout.ms:900000}") long timeoutMs,
			@Value("${transform.core.pymupdf4llm.max.concurrent:2}") int maxConcurrent) {
		super(scriptPath, timeoutMs, TOOL_LABEL);
		if (maxConcurrent < 1) {
			throw new IllegalArgumentException("transform.core.pymupdf4llm.max.concurrent must be at least 1");
		}
		this.conversionSlots = new Semaphore(maxConcurrent, true);
		this.slotWaitMs = timeoutMs;
	}

	@Override
	public String getTransformerName() {
		return TRANSFORMER_NAME;
	}

	/**
	 * Runs the conversion once a conversion slot is free.
	 *
	 * @throws TransformException if no slot is freed within the conversion timeout,
	 *                            if the wait is interrupted, or if the conversion fails
	 */
	@Override
	public void transform(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions,
			File sourceFile, File targetFile, TransformManager transformManager) throws TransformException {
		acquireConversionSlot();
		try {
			super.transform(sourceMimetype, targetMimetype, transformOptions, sourceFile, targetFile, transformManager);
		} finally {
			conversionSlots.release();
		}
	}

	private void acquireConversionSlot() {
		if (logger.isDebugEnabled() && conversionSlots.availablePermits() == 0) {
			logger.debug("All " + TOOL_LABEL + " conversion slots are busy, waiting for a free one");
		}
		try {
			if (!conversionSlots.tryAcquire(slotWaitMs, TimeUnit.MILLISECONDS)) {
				throw new TransformException(SERVICE_UNAVAILABLE,
						"No " + TOOL_LABEL + " conversion slot freed within " + slotWaitMs + " ms");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new TransformException(INTERNAL_SERVER_ERROR, "Interrupted while waiting for a conversion slot", e);
		}
	}

}
