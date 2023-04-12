#!/bin/bash

if [ $# -gt 1 ] && [ x"$1" = x"/bin/sh" ] && [ x"$2" = x"-c" ]; then
    shift 2
    eval "set -- $1"
fi

# $NSS_WRAPPER_PASSWD and $NSS_WRAPPER_GROUP have been set by the Dockerfile
export USER_ID=$(id -u)
export GROUP_ID=$(id -g)
envsubst < /passwd.template > ${NSS_WRAPPER_PASSWD}
export LD_PRELOAD=libnss_wrapper.so
if [ -d /etc/ecr-git-config ]; then
  cp -Rf /etc/ecr-git-config /opt/.ssh
  chmod 400 /opt/.ssh/id_rsa
fi

if [ ! -z "$JAVA_TOOL_OPTIONS" ]; then
  trustStore=`echo "${JAVA_TOOL_OPTIONS}" | sed -e 's/.*trustStore=\(.*\) .*/\1/'`
  trustStorePassword=`echo "${JAVA_TOOL_OPTIONS}" | sed -e 's/.*trustStorePassword=\(.*\)/\1/'`

  keytool -importkeystore -srckeystore $trustStore -srcstorepass $trustStorePassword -deststorepass $trustStorePassword -destkeystore /tmp/intermediate.p12 -srcstoretype JKS -deststoretype PKCS12
  openssl pkcs12 -in /tmp/intermediate.p12 -out /opt/certs/ca-certs-custom.pem -password "pass:$trustStorePassword"
  rm /tmp/intermediate.p12

  export GIT_SSL_CAINFO=/opt/certs/ca-certs-custom.pem
  export SSL_CERT_DIR=/etc/ssl/certs/:/opt/certs/
fi

exec $@