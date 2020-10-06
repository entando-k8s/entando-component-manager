FROM openjdk:8-jdk-slim
ENV PORT=8080 \
    CLASSPATH=/opt/lib \
    USER_NAME=root \
    NSS_WRAPPER_PASSWD=/tmp/passwd \
    NSS_WRAPPER_GROUP=/tmp/group

COPY passwd.template entrypoint.sh /

RUN chmod -Rf g+rw /opt && \
    touch ${NSS_WRAPPER_PASSWD} ${NSS_WRAPPER_GROUP} && \
    chgrp 0 ${NSS_WRAPPER_PASSWD} ${NSS_WRAPPER_GROUP} && \
    chmod g+rw ${NSS_WRAPPER_PASSWD} ${NSS_WRAPPER_GROUP} && \
    apt-get update && apt-get upgrade -y && \
    apt-get install -y git curl gpg tar gettext-base libnss-wrapper && \
    curl -s https://packagecloud.io/install/repositories/github/git-lfs/script.deb.sh | bash && \
    apt-get install -y git-lfs && git lfs install && \
    rm -rf /var/lib/apt/lists/*

EXPOSE 8080

# copy pom.xml and wildcards to avoid this command failing if there's no target/lib directory
COPY pom.xml target/lib* /opt/lib/

# NOTE we assume there's only 1 jar in the target dir
# but at least this means we don't have to guess the name
# we could do with a better way to know the name - or to always create an app.jar or something
COPY target/entando-component-manager.jar /opt/app.jar
WORKDIR /opt
ENTRYPOINT ["/entrypoint.sh"]
CMD ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-jar", "app.jar"]