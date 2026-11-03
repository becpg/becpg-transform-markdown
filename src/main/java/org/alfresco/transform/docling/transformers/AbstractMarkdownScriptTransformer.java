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
import org.alfresco.transform.base.fs.FileManager;
import org.alfresco.transform.base.executors.AbstractCommandExecutor;
import org.alfresco.transform.base.executors.RuntimeExec;
import org.alfresco.transform.base.executors.RuntimeExec.ExecutionResult;
import org.alfresco.transform.base.util.CustomTransformerFileAdaptor;
import org.alfresco.transform.exceptions.TransformException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import jakarta.annotation.PostConstruct;

/**
 * Base class of the transformers that convert a document to Markdown by
 * running a Python wrapper script, then check that the script produced a
 * non-empty output file.
 */
public abstract class AbstractMarkdownScriptTransformer extends AbstractCommandExecutor implements CustomTransformerFileAdaptor {

	private static final String ANY_MIMETYPE = ".*";
	private static final String TRANSFORM_ERROR_CODES = "1,2,255";
	private static final String VERSION_OPTION = "--version";
	private static final String NO_OPTIONS = "";
	private static final String NO_PAGE_RANGE = "";
	private static final String ABBREVIATION_SUFFIX = "...";
	private static final int MAX_LOGGED_OUTPUT_LENGTH = 1000;

	private final Log logger = LogFactory.getLog(getClass());

	/**
	 * Returns the path of the wrapper script that runs the conversion.
	 *
	 * @return the absolute path of the conversion script
	 */
	protected abstract String getScriptPath();

	/**
	 * Returns the maximum duration of one conversion.
	 *
	 * @return the timeout in milliseconds
	 */
	protected abstract long getTimeoutMs();

	/**
	 * Returns the tool name used in log messages.
	 *
	 * @return the human-readable name of the underlying tool
	 */
	protected abstract String getToolLabel();

	@PostConstruct
	private void createCommands() {
		super.transformCommand = createTransformCommand();
		super.checkCommand = createCheckCommand();
	}

	@Override
	protected RuntimeExec createTransformCommand() {
		RuntimeExec runtimeExec = new RuntimeExec();
		Map<String, String[]> commandsAndArguments = new HashMap<>();
		commandsAndArguments.put(ANY_MIMETYPE, new String[] { getScriptPath(), "${source}", "${target}", "${options}" });
		runtimeExec.setCommandsAndArguments(commandsAndArguments);
		runtimeExec.setErrorCodes(TRANSFORM_ERROR_CODES);
		return runtimeExec;
	}

	/**
	 * Builds the health check command, which runs the tool executable named after
	 * the transformer with its version option.
	 *
	 * @return the check command
	 */
	@Override
	protected RuntimeExec createCheckCommand() {
		RuntimeExec runtimeExec = new RuntimeExec();
		Map<String, String[]> commandsAndArguments = new HashMap<>();
		commandsAndArguments.put(ANY_MIMETYPE, new String[] { getTransformerName(), VERSION_OPTION });
		runtimeExec.setCommandsAndArguments(commandsAndArguments);
		return runtimeExec;
	}

	/**
	 * Runs the conversion script and fails when it exits with an error or leaves
	 * no output.
	 *
	 * @param properties the command substitution properties
	 * @param targetFile the file the script must write
	 * @param timeout the timeout in milliseconds, none when null or not positive
	 * @throws TransformException if the target file is outside the temporary directory, or if
	 *                            the script fails or produces an empty file
	 */
	@Override
	public void run(Map<String, String> properties, File targetFile, Long timeout) {
		FileManager.assertWithinTempDir(targetFile);
		long effectiveTimeout = timeout != null && timeout > 0 ? timeout : 0;
		ExecutionResult result = transformCommand.execute(properties, effectiveTimeout);
		if (logger.isDebugEnabled()) {
			logger.debug(getToolLabel() + " command finished with exit code " + result.getExitValue());
		}
		if (result.getExitValue() != 0) {
			logFailure(result.getStdErr());
			throw new TransformException(BAD_REQUEST, "Transformer failed with exit code " + result.getExitValue());
		}
		if (!targetFile.exists() || targetFile.length() == 0) {
			throw new TransformException(INTERNAL_SERVER_ERROR, "Transformer failed to create an output file");
		}
	}

	@Override
	public void transform(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions,
			File sourceFile, File targetFile, TransformManager transformManager) throws TransformException {
		run(NO_OPTIONS, sourceFile, NO_PAGE_RANGE, targetFile, getTimeoutMs());
	}

	private void logFailure(String stdErr) {
		if (logger.isWarnEnabled() && stdErr != null && !stdErr.isBlank()) {
			logger.warn(getToolLabel() + " command failed: " + abbreviate(stdErr));
		}
	}

	/**
	 * Limits command output stored in logs when a conversion fails.
	 *
	 * @param output the command output to truncate
	 * @return a bounded output string suitable for logs
	 */
	private String abbreviate(String output) {
		if (output.length() <= MAX_LOGGED_OUTPUT_LENGTH) {
			return output;
		}
		return output.substring(0, MAX_LOGGED_OUTPUT_LENGTH) + ABBREVIATION_SUFFIX;
	}

}
