# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [ 1.0.0 ] - mer. 15 avril 2026 11:50:18 CEST

* [Setup] Initial release of beCPG Markdown Transformer
* [Feature] Docling-based PDF/Office/HTML to Markdown/JSON conversion
* [Feature] md2pdf-based Markdown to PDF conversion
* [Feature] PyMuPDF4LLM transformer for alternative PDF processing
* [Feature] Docker containerization with Alfresco T-Engine support
* [Setup] Integration tests with Spring Boot Test framework


## [ 1.0.1 ] - jeu. 16 avril 2026 14:08:21 CEST

* [Release] prepare release 1.0.1
* [Feature] Replace Docling with MarkItDown; refactor Docker integration test to cover all supported transformations and update README
* Fix security issues and add tests
* [skip ci][Release]: Update CHANGELOG.md for 1.0.0 release
* [skip ci] Prepare for next development iteration

## [ 1.0.2 ] - ven. 17 avril 2026 11:13:27 CEST

* [Release] prepare release 1.0.2
* [Setup] Fix probe transform
* [skip ci][Release]: Update CHANGELOG.md for 1.0.1 release
* [skip ci] Prepare for next development iteration

## [ 1.0.3 ] - mar. 23 juin 2026 17:39:45 CEST

* [Release] prepare release 1.0.3
* [Setup] Fix probe transform
* [skip ci][Release]: Update CHANGELOG.md for 1.0.2 release
* [skip ci] Prepare for next development iteration

## [ 1.0.4 ] - mar. 01 sept 2026 18:21:46 CEST

* [Release] prepare release 1.0.4
* [Setup] Run the markdown release in batch mode so it does not stall unattended
* Fix #36292 - [Setup] Deliver the markdown transformer into the becpg-dev cell of becpg-test1
* [skip-ci][Setup] Update changelog
* [skip ci][Release]: Update CHANGELOG.md for 1.0.3 release
* [skip ci] Prepare for next development iteration

## [ 1.0.5 ] - lun. 02 nov 2026 14:01:44 CET

* [Release] prepare release 1.0.5
* [Bug] - Find the previous tag with git describe in release.sh so x.y.0 releases get their changelog
* Merge pull request #1 from becpg/security/brotli-ghsa-9v8h-qqgm-4m35
* InProgress #37977 - [Security] Require brotli 1.2.1 to fix GHSA-9v8h-qqgm-4m35 in the Markdown to PDF path
* [Setup] - Give the project its technical name and a description for SonarQube
* [Cleanup] - Remove the repository contributing guide to inherit the organization one
* [Setup] - Publish the full LGPL v3 text as LICENSE and keep the beCPG copyright notice in NOTICE
* InProgress #36252 - [Security] Deploy through the Connect Gateway, the control plane has no public IP
* [skip ci][Release]: Update CHANGELOG.md for 1.0.4 release
* [skip ci] Prepare for next development iteration

## [ 1.0.6 ] - mar. 03 nov 2026 11:27:43 CET

* [Release] prepare release 1.0.6
* [Setup] - Update README and MIGRATION for 1.0.6
* [Setup] - Pin markitdown to 0.1.9 in the Docker image of becpg-transform-markdown
* [Bug] - Stop advertising TIFF to Markdown, which MarkItDown cannot convert
* [Cleanup] - Inject the script path, timeout and label through the constructor to remove duplication in the Markdown script transformers
* [Setup] - Import JaCoCo coverage into SonarQube in becpg-transform-markdown
* [Bug] - Pin pymupdf4llm to 1.28.2 because 2.x is an empty package that breaks PDF to Markdown on Python 3.10
* [Cleanup] - Remove trailing whitespace from the CI build definition of becpg-transform-markdown
* [Setup] - Declare sonar-maven-plugin and use sonar.token so the CI Sonar step runs in becpg-transform-markdown
* [Setup] - Stop tracking the Eclipse .classpath and .project files already listed in .gitignore
* Merge branch 'sonar/2026-W44' into main
* [skip ci][Release]: Update CHANGELOG.md for 1.0.5 release
* [skip ci] Prepare for next development iteration
* [Cleanup] - Fix SonarQube issues of week 44 in becpg-transform-markdown
* [Cleanup] - Update dependencies of week 44 in becpg-transform-markdown

