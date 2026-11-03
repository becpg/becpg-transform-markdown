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
 * Transformer that uses MarkItDown (Microsoft) to convert various document
 * formats (Office, HTML, CSV, images, etc.) to Markdown.
 */
@Component
public class MarkitdownTransformer extends AbstractMarkdownScriptTransformer {

	private static final String TRANSFORMER_NAME = "markitdown";
	private static final String TOOL_LABEL = "MarkItDown";

	/**
	 * Creates the MarkItDown transformer from its configuration.
	 *
	 * @param scriptPath the absolute path of the MarkItDown wrapper script
	 * @param timeoutMs the maximum duration of one conversion, in milliseconds
	 */
	public MarkitdownTransformer(@Value("${transform.core.markitdown.script.path}") String scriptPath,
			@Value("${transform.core.markitdown.timeout.ms:900000}") long timeoutMs) {
		super(scriptPath, timeoutMs, TOOL_LABEL);
	}

	@Override
	public String getTransformerName() {
		return TRANSFORMER_NAME;
	}

}
