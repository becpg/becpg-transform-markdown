# Image provides a container in which to run Docling transformations for Alfresco Content Services.

# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-java
FROM alfresco/alfresco-base-java:jre17-rockylinux9

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG USERNAME=alfresco
ARG USERID=33002

RUN yum install -y python3 python3-pip && \
    pip3 install --no-cache-dir docling && \
    yum clean all

COPY src/main/docker/docling-convert.sh /usr/local/bin/docling-convert.sh

RUN chmod +x /usr/local/bin/docling-convert.sh

COPY target/*.jar /usr/bin/app.jar

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${USERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/app.jar

EXPOSE 8095

USER ${USERNAME}

ARG JAVA_OPTS
ENV JAVA_OPTS=${JAVA_OPTS}

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/app.jar
