FROM gigiozzz/entando-java-base:17.0.6 as builder

USER 0
COPY target/entando-component-manager.jar /opt/app.jar
# unpack layered jar
RUN cd /opt \
    && java -Djarmode=layertools -jar app.jar extract

USER 1001

FROM gigiozzz/entando-java-base:17.0.6
ARG VERSION
### Required OpenShift Labels
LABEL name="Entando Component Manager" \
      maintainer="dev@entando.com" \
      vendor="Entando Inc." \
      version="v{VERSION}" \
      release="7.1" \
      summary="Entando Component Manager for Entando Component Repository" \
      description="The component manager provides apis and infrastructure to support the deployment and development of bundles to an Entando Application."

#COPY target/generated-resources/licenses /licenses

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

RUN cd /opt
#copy layered jar from builder stage
COPY --from=builder /opt/dependencies ./
COPY --from=builder /opt/snapshot-dependencies ./
COPY --from=builder /opt/spring-boot-loader ./
COPY --from=builder /opt/application ./

WORKDIR /opt
ENTRYPOINT ["/entrypoint.sh"]
#CMD ["java", "-XX:MaxRAMPercentage=80.0", "-XshowSettings:vm", "-jar", "app.jar"]
CMD ["java", "-XX:MaxRAMPercentage=80.0", "-XshowSettings:vm","-cp",".", "org.springframework.boot.loader.JarLauncher"]