FROM entando/entando-java-base:11.0.2
ARG VERSION
### Required OpenShift Labels
LABEL name="Entando Component Manager" \
      maintainer="dev@entando.com" \
      vendor="Entando Inc." \
      version="v{VERSION}" \
      release="7.1" \
      summary="Entando Component Manager for Entando Component Repository" \
      description="The component manager provides apis and infrastructure to support the deployment and development of bundles to an Entando Application."

COPY target/generated-resources/licenses /licenses

### start git section -- copy and install
USER 0
RUN touch /tmp/passwd /tmp/group \
 && chgrp 0 /tmp/passwd /tmp/group \
 && chmod g+rw /tmp/passwd /tmp/group \
 && microdnf update \
 && microdnf install -y yum git git-lfs gettext nss_wrapper tar \
 && git lfs install \
 && curl -s https://packagecloud.io/install/repositories/github/git-lfs/script.rpm.sh | bash \
 && rpm -e yum \
 && microdnf clean -y all \
 && chmod -Rf g+rw /opt
USER 1001
### end git section --

### start crane section -- copy and install
ENV ENTANDO_CRANE_VERSION=v0.10.0
USER 0
RUN mkdir /tmp/crane; \
    cd /tmp/crane; \
    curl -OL https://github.com/google/go-containerregistry/releases/download/v0.10.0/go-containerregistry_Linux_i386.tar.gz; \
    tar -zxvf go-containerregistry_Linux_i386.tar.gz; \
    install -m 755 /tmp/crane/crane /usr/local/bin/crane; \
    rm -R /tmp/crane;
USER 1001
### end crane section --

### start certs section --
USER 0
RUN mkdir /opt/certs; \
    touch /opt/certs/ca-certs-custom.pem; \
    chown -R root:root /opt/certs; \
    chmod -R ug+rwx /opt/certs; 
USER 1001
### end certs section --

ENV PORT=8080 \
    CLASSPATH=/opt/lib \
    USER_NAME=root \
    NSS_WRAPPER_PASSWD=/tmp/passwd \
    NSS_WRAPPER_GROUP=/tmp/group

COPY passwd.template entrypoint.sh /

EXPOSE 8080


# copy pom.xml and wildcards to avoid this command failing if there's no target/lib directory
COPY pom.xml target/lib* /opt/lib/

# NOTE we assume there's only 1 jar in the target dir
# but at least this means we don't have to guess the name
# we could do with a better way to know the name - or to always create an app.jar or something
COPY target/entando-component-manager.jar /opt/app.jar
WORKDIR /opt
ENTRYPOINT ["/entrypoint.sh"]
CMD ["java", "-XX:MaxRAMPercentage=80.0", "-XshowSettings:vm", "-jar", "app.jar"]
