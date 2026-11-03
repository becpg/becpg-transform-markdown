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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Transformer that uses PyMuPDF4LLM to convert PDF documents to Markdown.
 */
@Component
public class Pymupdf4llmTransformer extends AbstractMarkdownScriptTransformer {

	private static final String TRANSFORMER_NAME = "pymupdf4llm";
	private static final String TOOL_LABEL = "PyMuPDF4LLM";

	/**
	 * Creates the PyMuPDF4LLM transformer from its configuration.
	 *
	 * @param scriptPath the absolute path of the PyMuPDF4LLM wrapper script
	 * @param timeoutMs the maximum duration of one conversion, in milliseconds
	 */
	public Pymupdf4llmTransformer(@Value("${transform.core.pymupdf4llm.script.path}") String scriptPath,
			@Value("${transform.core.pymupdf4llm.timeout.ms:900000}") long timeoutMs) {
		super(scriptPath, timeoutMs, TOOL_LABEL);
	}

	@Override
	public String getTransformerName() {
		return TRANSFORMER_NAME;
	}

}
