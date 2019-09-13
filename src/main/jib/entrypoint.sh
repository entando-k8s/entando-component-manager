#!/bin/sh
if [ "$DB_VENDOR" = "oracle" ] ; then
    pushd /app/entando-common/oracle-driver-installer
    ./install.sh || exit 1
    popd
fi

java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -cp /app/resources/:/app/classes/:/app/libs/* "org/entando/kubernetes/EntandoKubernetesJavaApplication"  "$@"