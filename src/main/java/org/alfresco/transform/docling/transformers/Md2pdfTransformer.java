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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.executors.AbstractCommandExecutor;
import org.alfresco.transform.base.executors.RuntimeExec;
import org.alfresco.transform.base.util.CustomTransformerFileAdaptor;
import org.alfresco.transform.exceptions.TransformException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class Md2pdfTransformer extends AbstractCommandExecutor implements CustomTransformerFileAdaptor {

	@Value("${transform.core.md2pdf.script.path}")
	private String convertScriptPath;

	@PostConstruct
	private void createCommands() {
		super.transformCommand = createTransformCommand();
		super.checkCommand = createCheckCommand();
	}

	@Override
	public String getTransformerName() {
		return "md2pdf";
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
	protected RuntimeExec createCheckCommand() {
		RuntimeExec runtimeExec = new RuntimeExec();
		Map<String, String[]> commandsAndArguments = new HashMap<>();
		commandsAndArguments.put(".*", new String[] { "md2pdf", "--version" });
		runtimeExec.setCommandsAndArguments(commandsAndArguments);
		return runtimeExec;
	}

	@Override
	public void transform(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions, File sourceFile, File targetFile,
			TransformManager transformManager) throws TransformException {
		final String options = "";
		String pageRange = "";
		Long timeout = null;
		run(options, sourceFile, pageRange, targetFile, timeout);
	}

}
