FROM registry.access.redhat.com/ubi8/openjdk-11
ARG VERSION
### Required OpenShift Labels
LABEL name="Entando Component Manager" \
      maintainer="dev@entando.com" \
      vendor="Entando Inc." \
      version="v{VERSION}" \
      release="6.3.0" \
      summary="Entando Component Manager for Entando Component Repository" \
      description="The component manager provides apis and infrastructure to support the deployment and development of bundles to an Entando Application."

COPY target/generated-resources/licenses /licenses

ENV PORT=8080 \
    CLASSPATH=/opt/lib \
    USER_NAME=root \
    NSS_WRAPPER_PASSWD=/tmp/passwd \
    NSS_WRAPPER_GROUP=/tmp/group

COPY passwd.template entrypoint.sh /


USER root
RUN microdnf install -y yum

RUN chmod -Rf g+rw /opt && \
    touch ${NSS_WRAPPER_PASSWD} ${NSS_WRAPPER_GROUP} && \
    chgrp 0 ${NSS_WRAPPER_PASSWD} ${NSS_WRAPPER_GROUP} && \
    chmod g+rw ${NSS_WRAPPER_PASSWD} ${NSS_WRAPPER_GROUP} && \
    yum update -y && yum upgrade -y && \
    yum install -y git curl gpg tar gettext nss_wrapper && \
    curl -s https://packagecloud.io/install/repositories/github/git-lfs/script.rpm.sh | bash && \
    yum install -y git-lfs && git lfs install && \
    rm -rf /var/cache/yum

USER 185

EXPOSE 8080


# copy pom.xml and wildcards to avoid this command failing if there's no target/lib directory
COPY pom.xml target/lib* /opt/lib/

# NOTE we assume there's only 1 jar in the target dir
# but at least this means we don't have to guess the name
# we could do with a better way to know the name - or to always create an app.jar or something
COPY target/entando-component-manager.jar /opt/app.jar
WORKDIR /opt
ENTRYPOINT ["/entrypoint.sh"]
CMD ["java", "-XX:MaxRAMPercentage=80.0", "-jar", "app.jar"]
