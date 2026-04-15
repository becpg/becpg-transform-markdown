# AI Assistant Guidelines

This document provides guidelines for AI assistants (like Claude) working on this project. It ensures consistency with our established coding conventions and development practices.

## Role and Expertise

**You are a Java/Spring Boot Developer** with expertise in:
- **Spring Boot** microservice development
- **Docker** containerization
- **Python integration** for document processing
- **Transformer engine architecture** (Alfresco T-Engine)
- **Document conversion** (PDF, Office, Markdown)

## Project Overview

This is a **beCPG Markdown Transformer** - an Alfresco T-Engine that:
- Converts PDF, Office documents, images to Markdown/JSON using Docling
- Converts Markdown to PDF using md2pdf
- Runs as a Docker containerized Spring Boot service
- Integrates with Alfresco Content Services

## Coding Guidelines

### 1. Java Version & Features
- Use **Java 17** (LTS) features
- Leverage modern Java capabilities:
  - Pattern matching for `instanceof`
  - Switch expressions
  - Text blocks for multi-line strings

### 2. Code Style Principles
- **4 spaces indentation**, no tabs
- **Braces on new lines** for classes and methods
- Use explicit types (avoid `var`)
- Use **lambdas** and **method references** for cleaner code
- Keep methods **short and focused** (≤ 30 lines recommended)

### 3. Dependency Management
- Minimize external dependencies
- Prefer Spring Boot starter dependencies
- Document any new dependency additions with justification

### 4. Project Structure
```
src/main/java/org/alfresco/transform/docling/
├── MarkdownTransformEngine.java       # Main transform engine
└── transformers/
    ├── DoclingTransformer.java        # Docling-based transformations
    ├── Md2pdfTransformer.java         # Markdown to PDF
    └── Pymupdf4llmTransformer.java    # PyMuPDF-based transformations
```

## Git Commit Message Format

Follow this **strict format** for all commits:

```
[Optional: InProgress/Fix #ticket] - [Type] Description
```

### Commit Types
- `[Feature]` - new features or enhancements
- `[Bug]` - bug fixes
- `[Setup]` - project setup or packaging changes
- `[Cleanup]` - code cleanup and refactoring
- `[Docker]` - Docker configuration changes
- `[Security]` - security-related fixes

### Examples
```
[Feature] Add support for TIFF image conversion
[Bug] Fix memory leak in DoclingTransformer
[Docker] Update base image to Rocky Linux 9
Fix #123 - [Bug] Fix PDF rendering issues
```

### Rules
- **One line only** - no multi-line commit messages
- **English descriptions** - clear and concise
- Include ticket references when applicable

## AI Assistant Best Practices

### When Writing Code
1. **Follow Spring Boot patterns** - use dependency injection via `@Autowired`
2. **Use explicit types** - no `var` declarations for readability
3. **Add proper error handling** with try-catch blocks and meaningful messages
4. **Use meaningful variable names** with explicit types

### Code Documentation
1. **Generate or update Javadoc** for all new/modified methods and classes
2. **Use proper Javadoc format** with `@param`, `@return`, `@throws` tags
3. **Avoid inline comments** - let code be self-documenting through clear naming

### Logging Standards
1. **Logger declaration pattern**:
   ```java
   private static final Log logger = LogFactory.getLog(ClassName.class);
   ```
2. **Always check log level** before expensive operations:
   ```java
   if (logger.isDebugEnabled()) {
       logger.debug("Transformation result: " + result);
   }
   ```
3. **Use appropriate log levels**:
   - `logger.error()` - for exceptions and critical failures
   - `logger.warn()` - for recoverable issues
   - `logger.info()` - for important business events
   - `logger.debug()` - for detailed tracing

### Testing Standards
1. **Integration tests** use Spring Boot Test framework
2. **Test naming**: `*IT.java` suffix for integration tests
3. **Test annotations**:
   ```java
   @SpringBootTest(classes = { Transformer.class, TransformManagerImpl.class })
   @ActiveProfiles("it")
   ```
4. Use `assertDoesNotThrow()` for operations that should not fail

### Docker Development
1. **Build command**: `./run.sh build`
2. **Start command**: `./run.sh start`
3. **Docker compose**: Located in `target/docker-compose.yml` after build

### Transformer Development
1. Extend `AbstractTransformer` for new transformers
2. Implement `getTransformerName()` with unique identifier
3. Add transform configuration in `becpg_transform_markdown_config.json`
4. Handle source/target mimetype mappings properly

## Security Guidelines

1. **Never log sensitive data** (file contents, paths with sensitive info)
2. **Validate file inputs** - check file existence and permissions
3. **Use temp files safely** - prefer `File.createTempFile()`
4. **Clean up resources** - use try-with-resources or finally blocks

## Performance Considerations

- Process files in chunks for large documents
- Use streaming where possible
- Implement proper resource cleanup
- Monitor Docker container resource usage
- Consider timeout settings for long-running conversions

## Error Handling Patterns

```java
/**
 * Transforms source file to target format.
 *
 * @param source the source file
 * @param target the target file
 * @throws TransformException if transformation fails
 */
public void transform(File source, File target) {
    if (logger.isDebugEnabled()) {
        logger.debug("Starting transformation: " + source.getName());
    }
    
    try {
        performTransformation(source, target);
        
        if (logger.isDebugEnabled()) {
            logger.debug("Transformation completed successfully");
        }
    } catch (IOException e) {
        logger.error("Transformation failed for: " + source.getName(), e);
        throw new TransformException("Could not transform file", e);
    }
}
```

---

**Remember**: This project values **clarity and maintainability** over complexity. When in doubt, choose the simpler, more readable solution.
