/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
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
package org.alfresco.transform.docling;

import static org.alfresco.transform.base.logging.StandardMessages.COMMUNITY_LICENCE;

import java.util.Collections;

import org.alfresco.transform.base.TransformEngine;
import org.alfresco.transform.base.probes.ProbeTransform;
import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.config.reader.TransformConfigResourceReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DoclingTransformEngine implements TransformEngine {
	@Autowired
	private TransformConfigResourceReader transformConfigResourceReader;

	@Override
	public String getTransformEngineName() {
		return "docling";
	}

	@Override
	public String getStartupMessage() {
		return COMMUNITY_LICENCE + "This transformer uses Docling";
	}

	@Override
	public TransformConfig getTransformConfig() {
		return transformConfigResourceReader.read("classpath:docling_engine_config.json");
	}

	@Override
	public ProbeTransform getProbeTransform() {
		return new ProbeTransform("probe.pdf", "application/pdf", "text/markdown", Collections.emptyMap(), 212568, 1024, 150, 1024, 60 * 15L + 1,
				60 * 15L);
	}
}
