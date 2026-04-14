# Image provides a container in which to run Docling transformations for Alfresco Content Services.

# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-java
FROM alfresco/alfresco-base-java:jre17-rockylinux9

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG USERNAME=alfresco
ARG USERID=33002

RUN yum install -y python3 python3-pip cairo cairo-devel pango pango-devel gdk-pixbuf2 gdk-pixbuf2-devel libffi-devel && \
    pip3 install --no-cache-dir docling md2pdf && \
    yum clean all

# Copy Python 3.11 source tarball
COPY Python-3.11.15.tar.xz /tmp/Python-3.11.15.tar.xz

# Install build dependencies
RUN yum install -y gcc openssl-devel bzip2-devel libffi-devel zlib-devel make xz

# Extract Python source
RUN tar xJf /tmp/Python-3.11.15.tar.xz -C /tmp && cd /tmp/Python-3.11.15 && \
	./configure --enable-optimizations && make altinstall && \
    /usr/local/bin/python3.11 -m ensurepip && \
    /usr/local/bin/python3.11 -m pip install --upgrade pip && \
	rm -rf /tmp/Python-3.11.15 /tmp/Python-3.11.15.tar.xz && yum clean all

RUN pip3.11 install --no-cache-dir pymupdf4llm

COPY docling-convert.sh /usr/local/bin/docling-convert.sh
COPY md2pdf-convert.sh /usr/local/bin/md2pdf-convert.sh
COPY pymupdf4llm-convert.sh /usr/local/bin/pymupdf4llm-convert.sh
COPY pymupdf4llm-convert.py /usr/local/bin/pymupdf4llm-convert.py

RUN chmod +x /usr/local/bin/docling-convert.sh
RUN chmod +x /usr/local/bin/md2pdf-convert.sh
RUN chmod +x /usr/local/bin/pymupdf4llm-convert.sh
RUN chmod +x /usr/local/bin/pymupdf4llm-convert.py

COPY becpg-transform-markdown-${project.version}.jar /usr/bin/app.jar

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${USERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/app.jar

EXPOSE 8090

USER ${USERNAME}

ARG JAVA_OPTS
ENV JAVA_OPTS=${JAVA_OPTS}

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/app.jar