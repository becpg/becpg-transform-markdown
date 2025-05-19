package org.alfresco.transform.docling;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.transform.TransformManagerImpl;
import org.alfresco.transform.docling.transformers.DoclingTransformer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = { DoclingTransformer.class, TransformManagerImpl.class } )
@ActiveProfiles("it")
class DoclingTransformerIT {

	@Autowired
	private DoclingTransformer transformer;
	
	@Autowired
	private TransformManager transformManager;

	@Test
	void testGetTransformerName() {
		assertEquals("docling", transformer.getTransformerName());
	}

	@Test
    void testTransformMarkdown() {
        File source = new File("src/test/resources/sample.pdf");
        File target = new File("src/test/resources/sample-" + System.currentTimeMillis() + ".md");
        Map<String, String> options = new HashMap<>();
        assertDoesNotThrow(() ->
            transformer.transform("application/pdf", "text/markdow", options, source, target, transformManager)
        );
    }
	
}
