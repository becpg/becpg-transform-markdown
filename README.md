# Alfresco Transformer: Docling-based PDF/Office/HTML/Image ➜ Markdown & JSON

This project provides an Alfresco Content Services (ACS) transformer that converts a wide variety of document types into Markdown and JSON formats using [Docling](https://github.com/docling/docling), a lightweight document conversion library licensed under the MIT License.

The transformer runs inside a Docker container and can be integrated into Alfresco as a local transformer.

---

## 🚀 Supported Transformations

The transformer supports the following **source formats**, converted into:

- `text/markdown`
- `text/x-markdown`
- `application/json`

### 📄 Document Types

- PDF (`application/pdf`)
- Word (.docx) (`application/vnd.openxmlformats-officedocument.wordprocessingml.document`)
- Excel (.xlsx) (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`)
- PowerPoint (.pptx) (`application/vnd.openxmlformats-officedocument.presentationml.presentation`)
- Markdown (`text/markdown`)
- AsciiDoc (`text/asciidoc`)
- HTML (`text/html`, `application/xhtml+xml`)
- CSV (`text/csv`)
- Docling JSON (`application/vnd.docling+json`)

### 🖼️ Image Types (Markdown only)

- PNG (`image/png`)
- JPEG (`image/jpeg`)
- TIFF (`image/tiff`)
- BMP (`image/bmp`)

### 🧾 Specialized Formats

- JATS XML (`application/vnd.jats+xml`)
- USPTO XML (`application/vnd.uspto+xml`)

---

## 🛠️ Getting Started

### 🧱 Build the Docker Image

To create the transformer Docker image, run:

```bash
docker build -t "becpg-transform-markdown:1.0.0" .

```

This uses `alfresco-base-java` and installs Python 3 and the `docling` package via pip.

```bash
docker run -p 8095:8095 -p 8099:8099 becpg-transform-markdown:1.0.0
```

- Port `8095`: Main API endpoint for transformation requests.
- Port `8099`: Optional Java remote debugger.

### 🔗 Register with Alfresco

Add the following JVM property to your Alfresco instance:

```
-DlocalTransform.becpg-transform-markdown.url=http://becpg-transform-markdown:8095/
```

This allows Alfresco to discover and use the transformer.

## License

- This project uses **Docling**, licensed under the MIT License.

- Base image from [Alfresco Docker Base Java](https://github.com/Alfresco/alfresco-docker-base-java)

  

## 🙌 Acknowledgments

- [Docling](https://github.com/docling/docling) — for doing the heavy lifting
- [Alfresco](https://www.alfresco.com/) — for the open content services platform
- Community contributors and open-source maintainers

Feel free to fork, contribute, or open issues. Happy transforming!
