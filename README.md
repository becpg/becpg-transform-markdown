# beCPG Markdown Transformer for Alfresco

[![Java 17](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-green.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker)](https://www.docker.com/)
[![License: LGPL v3](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)](https://www.gnu.org/licenses/lgpl-3.0)

> An Alfresco T-Engine that converts PDF, Office documents, HTML, and images to Markdown using [MarkItDown](https://github.com/microsoft/markitdown), [pymupdf4llm](https://github.com/pymupdf/PyMuPDF), and Markdown to PDF using [md2pdf](https://github.com/jmaupetit/md2pdf).

---

## Table of Contents

- [Features](#features)
- [Supported Transformations](#supported-transformations)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Docker Deployment](#docker-deployment)
- [Testing](#testing)
- [Contributing](#contributing)
- [Security](#security)
- [License](#license)

---

## Features

- **Bidirectional conversion**: PDF/Office/HTML/Images to Markdown and Markdown to PDF
- **Dockerized**: Ready-to-use Docker container with all dependencies
- **Alfresco integration**: Native T-Engine for Alfresco Content Services
- **Lightweight**: Uses MarkItDown and pymupdf4llm instead of heavy ML-based pipelines
- **Image support**: Convert images to Markdown descriptions
- **Extensible**: Easy to add new transformation capabilities

---

## Overview

This project provides an Alfresco Content Services (ACS) transformer that converts a wide variety of document types into Markdown format using three lightweight Python libraries:

- **[MarkItDown](https://github.com/microsoft/markitdown)** (MIT License) - Converts Office documents, HTML, CSV, and images to Markdown
- **[pymupdf4llm](https://github.com/pymupdf/PyMuPDF)** (AGPL-3.0) - Converts PDF documents to Markdown
- **[md2pdf](https://github.com/jmaupetit/md2pdf)** (MIT License) - Converts Markdown to PDF

The transformer runs inside a Docker container and can be integrated into Alfresco as a local transformer.

---

## Supported Transformations

### MarkItDown: Documents and Images to Markdown

The following source formats are converted to `text/markdown` (or `text/x-markdown`):

**Document Types**

| Source | MIME Type |
|--------|-----------|
| Word (.docx) | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` |
| Excel (.xlsx) | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| PowerPoint (.pptx) | `application/vnd.openxmlformats-officedocument.presentationml.presentation` |
| HTML | `text/html` |
| XHTML | `application/xhtml+xml` |
| CSV | `text/csv` |

**Image Types**

| Source | MIME Type |
|--------|-----------|
| PNG | `image/png` |
| JPEG | `image/jpeg` |
| TIFF | `image/tiff` |
| BMP | `image/bmp` |

### pymupdf4llm: PDF to Markdown

| Source | Target | Description |
|--------|--------|-------------|
| `application/pdf` | `text/markdown` | Convert PDF documents to Markdown |

### md2pdf: Markdown to PDF

| Source | Target | Description |
|--------|--------|-------------|
| `text/markdown` | `application/pdf` | Convert Markdown documents to PDF |

---

## Getting Started

### Build and run the Docker Image

To create the transformer Docker image, run:

```bash
./run.sh build
```

This uses `alfresco-base-java` and installs Python 3.10 with MarkItDown, pymupdf4llm, and md2pdf via pip.

To run the image:

```
./run.sh start
```

* Port 8090 is for transformations

To enable remote debugging locally, start the container with:

```bash
docker compose -f target/docker-compose.yml run -p 8099:8099 -e JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:8099" becpg-transform-markdown
```

Example of request to transform a PDF file to Markdown:

```shell
curl --location --request POST 'http://localhost:8090/transform' \
--form 'file=@"/path/to/sample.pdf"' \
--form 'sourceMimetype="application/pdf"' \
--form 'targetMimetype="text/markdown"'
```

### Register with Alfresco

You can declare the Docker service as follow in a docker-compose.yml file:

```yaml
  becpg-transform-markdown:
    image: becpg-transform-markdown:1.0.0
    ports:
      - "8090:8090"
    environment:
      - SERVER_PORT=8090
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8090/live"]
      interval: 30s
      timeout: 10s
      retries: 5
```

Add the following JVM property to your Alfresco instance:

```
-DlocalTransform.becpg-transform-markdown.url=http://localhost:8090/
```

This allows Alfresco to discover and use the transformer.

---

## API Reference

### Health Check

```bash
GET http://localhost:8090/live
```

### Transform Document

```bash
POST http://localhost:8090/transform
```

**Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | Yes | Source file to transform |
| `sourceMimetype` | String | Yes | MIME type of source file |
| `targetMimetype` | String | Yes | Desired output MIME type |

**Example:**
```bash
curl --location --request POST 'http://localhost:8090/transform' \
  --form 'file=@"/path/to/document.pdf"' \
  --form 'sourceMimetype="application/pdf"' \
  --form 'targetMimetype="text/markdown"'
```
---

## Testing

### Unit / Integration tests

Run integration tests (requires MarkItDown and pymupdf4llm installed locally):

```bash
mvn test
```

Run a specific test:

```bash
mvn test -Dtest=MarkitdownTransformerIT
```

### Docker integration tests

The `DockerTransformIT` builds a Docker image, starts a container, and verifies every supported transformation end-to-end. It is a parameterized test covering all 24 source/target combinations from the config (12 unique transformations, each with both `text/markdown` and `text/x-markdown` targets, plus MD-to-PDF and PDF-to-MD).

```bash
mvn package -DskipTests
mvn test -Dtest=DockerTransformIT
```

Output files are written to `src/test/resources/output/` for manual inspection.

---

## Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) and [Code of Conduct](CODE_OF_CONDUCT.md) for details.

---

## Security

For security-related issues, please see our [Security Policy](SECURITY.md).

---

## License

- This project is licensed under the **GNU Lesser General Public License v3.0** - see the [LICENSE](LICENSE) file for details.
- This project uses **MarkItDown** and **md2pdf**, licensed under the MIT License.
- This project uses **pymupdf4llm**, licensed under the AGPL-3.0 License.
- Base image from [Alfresco Docker Base Java](https://github.com/Alfresco/alfresco-docker-base-java)

---

## Acknowledgments

- [beCPG](https://www.becpg.fr/) - The open source PLM solution
- [MarkItDown](https://github.com/microsoft/markitdown) - Microsoft's lightweight document-to-Markdown converter
- [pymupdf4llm](https://github.com/pymupdf/PyMuPDF) - PDF to Markdown conversion
- [md2pdf](https://github.com/jmaupetit/md2pdf) - Markdown to PDF conversion
- [Alfresco](https://www.alfresco.com/) - The open content services platform

---

<p align="center">Made with care by the beCPG team</p>
