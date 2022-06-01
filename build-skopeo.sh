#!/bin/bash

function execOrExit {
  /bin/bash -c "$@"
  local status=$?
  if (( status != 0 )); then
      echo "error executing '$*' status: '$status'" >&2
      exit $status
  fi
  return $status
}

if [ -z "$ENTANDO_SKOPEO_VERSION" ]; then
  echo "ENTANDO_SKOPEO_VERSION var not found" >&2
  exit 1
fi


echo "Checkout skopeo fixed version: $ENTANDO_SKOPEO_VERSION"
execOrExit "mkdir /root/skopeo"
execOrExit "git clone https://github.com/containers/skopeo.git /root/skopeo/src/github.com/containers/skopeo"
export GOPATH=/root/skopeo
execOrExit "cd /root/skopeo/src/github.com/containers/skopeo; git checkout $ENTANDO_SKOPEO_VERSION"

echo "Compile skopeo static"
export CGO_ENABLED=0
execOrExit "cd /root/skopeo/src/github.com/containers/skopeo; DISABLE_DOCS=1 make BUILDTAGS=containers_image_openpgp GO_DYN_FLAGS= bin/skopeo"

