#!/bin/bash

if [ -z "$1" ]; then
  echo "Please provide the working directory" >&2
  exit 1
fi

if [ -z "$2" ]; then
  echo "Please provide the scopeo version" >&2
  exit 1
fi

set -e

WORK_DIR="$1"
cd "$WORK_DIR"
ENTANDO_SKOPEO_VERSION="$2"


echo ""
echo "> Checking out skopeo version: ${ENTANDO_SKOPEO_VERSION}.."
echo ""

mkdir -p "$WORK_DIR"
git clone "https://github.com/containers/skopeo.git" "$WORK_DIR/src/github.com/containers/skopeo"
export GOPATH="$WORK_DIR"
cd "$WORK_DIR/src/github.com/containers/skopeo"
git checkout "$ENTANDO_SKOPEO_VERSION"

echo ""
echo "> Compiling skopeo.."
echo ""

export CGO_ENABLED=0

cd "$WORK_DIR/src/github.com/containers/skopeo"
DISABLE_DOCS=1 make BUILDTAGS=containers_image_openpgp GO_DYN_FLAGS=

./bin/skopeo -v

echo ""
echo "> build-skopeo completed"
echo ""
