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

@Component
public class DoclingTransformer extends AbstractCommandExecutor implements CustomTransformerFileAdaptor {

	private static final Log logger = LogFactory.getLog(DoclingTransformer.class);

	@Value("${transform.core.docling.script.path}")
	private String convertScriptPath;

	@PostConstruct
	private void createCommands() {
		super.transformCommand = createTransformCommand();
		super.checkCommand = createCheckCommand();
	}

	@Override
	public String getTransformerName() {
		return "docling";
	}

	@Override
	protected RuntimeExec createTransformCommand() {
		RuntimeExec runtimeExec = new RuntimeExec();
		Map<String, String[]> commandsAndArguments = new HashMap<>();
		commandsAndArguments.put(".*", new String[] { convertScriptPath, "${source}", "${target}", "${options}"});
		runtimeExec.setCommandsAndArguments(commandsAndArguments);
		runtimeExec.setErrorCodes("1,2,255");
		return runtimeExec;
	}
	
	@Override
	public void run(Map<String, String> properties, File targetFile, Long timeout) {
		timeout = timeout != null && timeout > 0 ? timeout : 0;
		final ExecutionResult result = transformCommand.execute(properties, timeout);
		logger.info(result.getStdOut());
		if (result.getExitValue() != 0 && result.getStdErr() != null && result.getStdErr().length() > 0) {
			throw new TransformException(BAD_REQUEST, "Transformer exit code was not 0: \n" + result.getStdErr());
		}
		if (!targetFile.exists() || targetFile.length() == 0) {
			throw new TransformException(INTERNAL_SERVER_ERROR, "Transformer failed to create an output file");
		}
	}

	@Override
	protected RuntimeExec createCheckCommand() {
		RuntimeExec runtimeExec = new RuntimeExec();
		Map<String, String[]> commandsAndArguments = new HashMap<>();
		commandsAndArguments.put(".*", new String[] { "docling", "--version" });
		runtimeExec.setCommandsAndArguments(commandsAndArguments);
		return runtimeExec;
	}

	@Override
	public void transform(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions, File sourceFile, File targetFile,
			TransformManager transformManager) throws TransformException {
		String doclingTargetFormat = "";
		switch (targetMimetype) {
		case "text/markdow":
			doclingTargetFormat = "md";
			break;
		case "application/json":
			doclingTargetFormat = "json";
			break;
		default:
			doclingTargetFormat = "md";
		}
		final String options = "--no-ocr --to " + doclingTargetFormat 
				+ (Boolean.TRUE.toString().equals(transformOptions.get("skipImages")) ? " --image-export-mode placeholder" : "");
		String pageRange = "";
		Long timeout = null;
		run(options, sourceFile, pageRange, targetFile, timeout);
	}

}
