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

	@Value("${transform.core.markitdown.script.path}")
	private String convertScriptPath;

	@Value("${transform.core.markitdown.timeout.ms:900000}")
	private long timeoutMs;

	@Override
	public String getTransformerName() {
		return "markitdown";
	}

	@Override
	protected String getScriptPath() {
		return convertScriptPath;
	}

	@Override
	protected long getTimeoutMs() {
		return timeoutMs;
	}

	@Override
	protected String getToolLabel() {
		return "MarkItDown";
	}

}
