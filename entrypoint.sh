#!/bin/bash

# $NSS_WRAPPER_PASSWD and $NSS_WRAPPER_GROUP have been set by the Dockerfile
export USER_ID=$(id -u)
export GROUP_ID=$(id -g)
envsubst < /passwd.template > ${NSS_WRAPPER_PASSWD}
export LD_PRELOAD=libnss_wrapper.so
if [ -d /etc/ecr-git-config ]; then
  cp -Rf /etc/ecr-git-config /opt/.ssh
  chmod 400 /opt/.ssh/id_rsa
fi
exec $@