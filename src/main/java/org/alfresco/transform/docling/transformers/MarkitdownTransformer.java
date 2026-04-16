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

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.executors.AbstractCommandExecutor;
import org.alfresco.transform.base.executors.RuntimeExec;
import org.alfresco.transform.base.executors.RuntimeExec.ExecutionResult;
import org.alfresco.transform.base.util.CustomTransformerFileAdaptor;
import org.alfresco.transform.exceptions.TransformException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Transformer that uses MarkItDown (Microsoft) to convert various document
 * formats (Office, HTML, CSV, images, etc.) to Markdown.
 */
@Component
public class MarkitdownTransformer extends AbstractCommandExecutor implements CustomTransformerFileAdaptor {

	private static final Log logger = LogFactory.getLog(MarkitdownTransformer.class);

	@Value("${transform.core.markitdown.script.path}")
	private String convertScriptPath;

	@Value("${transform.core.markitdown.timeout.ms:900000}")
	private long timeoutMs;

	@PostConstruct
	private void createCommands() {
		super.transformCommand = createTransformCommand();
		super.checkCommand = createCheckCommand();
	}

	@Override
	public String getTransformerName() {
		return "markitdown";
	}

	@Override
	protected RuntimeExec createTransformCommand() {
		RuntimeExec runtimeExec = new RuntimeExec();
		Map<String, String[]> commandsAndArguments = new HashMap<>();
		commandsAndArguments.put(".*", new String[] { convertScriptPath, "${source}", "${target}", "${options}" });
		runtimeExec.setCommandsAndArguments(commandsAndArguments);
		runtimeExec.setErrorCodes("1,2,255");
		return runtimeExec;
	}

	@Override
	public void run(Map<String, String> properties, File targetFile, Long timeout) {
		timeout = timeout != null && timeout > 0 ? timeout : 0;
		final ExecutionResult result = transformCommand.execute(properties, timeout);
		if (logger.isDebugEnabled()) {
			logger.debug("MarkItDown command finished with exit code " + result.getExitValue());
		}
		if (result.getExitValue() != 0) {
			if (logger.isWarnEnabled() && result.getStdErr() != null && !result.getStdErr().isBlank()) {
				logger.warn("MarkItDown command failed: " + abbreviate(result.getStdErr()));
			}
			throw new TransformException(BAD_REQUEST, "Transformer failed with exit code " + result.getExitValue());
		}
		if (!targetFile.exists() || targetFile.length() == 0) {
			throw new TransformException(INTERNAL_SERVER_ERROR, "Transformer failed to create an output file");
		}
	}

	@Override
	protected RuntimeExec createCheckCommand() {
		RuntimeExec runtimeExec = new RuntimeExec();
		Map<String, String[]> commandsAndArguments = new HashMap<>();
		commandsAndArguments.put(".*", new String[] { "markitdown", "--version" });
		runtimeExec.setCommandsAndArguments(commandsAndArguments);
		return runtimeExec;
	}

	@Override
	public void transform(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions,
			File sourceFile, File targetFile, TransformManager transformManager) throws TransformException {
		String pageRange = "";
		Long timeout = timeoutMs;
		run("", sourceFile, pageRange, targetFile, timeout);
	}

	/**
	 * Limits command output stored in logs when a conversion fails.
	 *
	 * @param output the command output to truncate
	 * @return a bounded output string suitable for logs
	 */
	private String abbreviate(String output) {
		if (output.length() <= 1000) {
			return output;
		}
		return output.substring(0, 1000) + "...";
	}

}
