# Image provides a container in which to run Docling transformations for Alfresco Content Services.

# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-java
FROM alfresco/alfresco-base-java:jre17-rockylinux9

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG USERNAME=alfresco
ARG USERID=33002

# Install Python and pip
RUN yum install -y python3 python3-pip && \
    pip3 install --no-cache-dir docling && \
    yum clean all

COPY docling-convert.sh /usr/local/bin/docling-convert.sh

# Make the script executable
RUN chmod +x /usr/local/bin/docling-convert.sh

COPY target/*.jar /usr/bin/app.jar
#COPY decode_and_convert.sh /usr/local/bin/decode_and_convert.sh
#RUN chmod +x /usr/local/bin/decode_and_convert.sh

RUN groupadd -g ${GROUPID} ${GROUPNAME} && \
    useradd -u ${USERID} -G ${GROUPNAME} ${USERNAME} && \
    chgrp -R ${GROUPNAME} /usr/bin/app.jar

EXPOSE 8095

USER ${USERNAME}

ENV JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:8099"

ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/app.jar
