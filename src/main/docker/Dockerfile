# Multi-stage Dockerfile for beCPG Markdown Transformer
# Optimized for: security, image size, and build performance

# =============================================================================
# STAGE 1: Python Builder
# Builds Python 3.10 from source with optimizations
# =============================================================================
FROM rockylinux:9 AS python-builder

ARG PYTHON_VERSION=3.10.20
ARG PYTHON_URL=https://www.python.org/ftp/python/${PYTHON_VERSION}/Python-${PYTHON_VERSION}.tgz
ARG PYTHON_SHA256=4ff5fd4c5bab803b935019f3e31d7219cebd6f870d00389cea53b88bbe935d1a

# Install build dependencies (cleaned up after build)
RUN dnf install -y --setopt=install_weak_deps=0 \
    gcc \
    openssl \
    openssl-devel \
    openssl-libs \
    bzip2-devel \
    libffi-devel \
    zlib-devel \
    make \
    wget \
    tar \
    gzip \
    && dnf clean all

# Download and extract Python source (TLS verified)
WORKDIR /tmp
RUN wget --progress=dot:giga ${PYTHON_URL} \
    && echo "${PYTHON_SHA256}  Python-${PYTHON_VERSION}.tgz" | sha256sum -c - \
    && tar -xzf Python-${PYTHON_VERSION}.tgz \
    && rm Python-${PYTHON_VERSION}.tgz

# Build and install Python with optimizations and SSL support
WORKDIR /tmp/Python-${PYTHON_VERSION}
RUN LDFLAGS="-L/usr/lib64 -L/usr/lib" \
    CPPFLAGS="-I/usr/include/openssl" \
    ./configure \
    --prefix=/opt/python \
    --enable-optimizations \
    --enable-shared \
    --with-ensurepip=install \
    --disable-test-modules \
    --with-system-ffi \
    --with-openssl=/usr \
    && make -j$(nproc) \
    && make altinstall \
    && ldconfig /opt/python/lib

# Set library path for Python to find its shared libraries
ENV LD_LIBRARY_PATH=/opt/python/lib:$LD_LIBRARY_PATH

# Create virtual environment for isolation
RUN /opt/python/bin/python3.10 -m venv /opt/venv

# =============================================================================
# STAGE 2: Python Dependencies
# Installs markitdown, md2pdf and pymupdf4llm
# =============================================================================
FROM rockylinux:9 AS python-deps

# Copy Python from builder
COPY --from=python-builder /opt/python /opt/python
COPY --from=python-builder /opt/venv /opt/venv

# Set library path and update cache
ENV LD_LIBRARY_PATH=/opt/python/lib:$LD_LIBRARY_PATH
RUN ldconfig /opt/python/lib

ENV PATH="/opt/python/bin:/opt/venv/bin:$PATH"
ENV PYTHONPATH="/opt/venv/lib/python3.10/site-packages"

# Install runtime dependencies needed for pip install (including SSL libs)
RUN dnf install -y --setopt=install_weak_deps=0 \
    openssl \
    openssl-libs \
    ca-certificates \
    cairo \
    cairo-devel \
    pango \
    pango-devel \
    gdk-pixbuf2 \
    gdk-pixbuf2-devel \
    libffi-devel \
    && dnf clean all

# Install markitdown, md2pdf and pymupdf4llm in venv (all versions pinned)
RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir 'markitdown[pptx, docx, xlsx]' md2pdf==0.4.0 pymupdf4llm

# =============================================================================
# STAGE 3: Final Runtime Image
# Minimal image with only runtime dependencies
# =============================================================================
FROM alfresco/alfresco-base-java:jre17-rockylinux9@sha256:f98833508b7be8c4b44a25450f9faac44cacfdc075f2295e02836b93fd05bb9c

# Security: Non-root user configuration
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG USERNAME=alfresco
ARG USERID=33002

# Set environment variables
ENV PATH="/opt/python/bin:/opt/venv/bin:$PATH"
ENV LD_LIBRARY_PATH=/opt/python/lib:$LD_LIBRARY_PATH
ENV PYTHONPATH="/opt/venv/lib/python3.10/site-packages"
ENV MPLCONFIGDIR=/tmp/matplotlib
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

# Create non-root user first (required before COPY --chown)
RUN groupadd -g ${GROUPID} ${GROUPNAME} \
    && useradd -u ${USERID} -g ${GROUPID} -G ${GROUPNAME} ${USERNAME}

# Install only runtime dependencies (curl-minimal already present in base image)
RUN dnf install -y --setopt=install_weak_deps=0 \
    openssl \
    openssl-libs \
    ca-certificates \
    cairo \
    pango \
    gdk-pixbuf2 \
    libffi \
    poppler-utils \
    libjpeg-turbo \
    libpng \
    && dnf clean all \
    && rm -rf /var/cache/dnf /var/lib/dnf/history* /var/log/dnf*

# Copy Python installation from builder
COPY --from=python-builder /opt/python /opt/python

# Update shared library cache for Python
RUN ldconfig /opt/python/lib

# Copy virtual environment with installed packages
COPY --from=python-deps /opt/venv /opt/venv

# Set ownership and permissions on Python artifacts
RUN chown -R ${USERNAME}:${GROUPNAME} /opt/venv /opt/python \
    && find /opt/venv /opt/python -type d -exec chmod 755 {} \; \
    && find /opt/venv /opt/python -type f -name "*.py" -exec chmod 644 {} \; \
    && find /opt/venv /opt/python -type f ! -name "*.py" -exec chmod 755 {} \;

# Copy application scripts
COPY --chown=${USERID}:${GROUPID} \
    markitdown-convert.sh \
    md2pdf-convert.sh \
    pymupdf4llm-convert.sh \
    pymupdf4llm-convert.py \
    /usr/local/bin/

# Set executable permissions on shell scripts only
RUN chmod +x /usr/local/bin/markitdown-convert.sh \
    /usr/local/bin/md2pdf-convert.sh \
    /usr/local/bin/pymupdf4llm-convert.sh

# Copy application JAR
COPY --chown=${USERID}:${GROUPID} becpg-transform-markdown-${project.version}.jar /usr/bin/app.jar

# Create writable directories for runtime
RUN mkdir -p /tmp/matplotlib \
    && chown -R ${USERNAME}:${GROUPNAME} /tmp/matplotlib

# Security: Remove setuid/setgid binaries from known locations only
RUN find /usr /bin /sbin /opt -perm /6000 -type f -exec chmod a-s {} \; 2>/dev/null || true

EXPOSE 8090

# Switch to non-root user
USER ${USERNAME}

ARG JAVA_OPTS
ENV JAVA_OPTS=${JAVA_OPTS}

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8090/live || exit 1

# Use shell form to allow $JAVA_OPTS expansion
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /usr/bin/app.jar"]
