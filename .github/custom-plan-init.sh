#!/bin/bash


ENTANDO_CRANE_VERSION="$(
  grep "ENV\s*ENTANDO_CRANE_VERSION" Dockerfile | cut -d'=' -f 2
)"
WORK_DIR="$HOME/.entando/crane"
mkdir -p "$WORK_DIR"
cd "$WORK_DIR"

echo ""
echo "> Download crane version: ${ENTANDO_CRANE_VERSION}.."
echo ""

curl -OL "https://github.com/google/go-containerregistry/releases/download/$ENTANDO_CRANE_VERSION/go-containerregistry_Linux_i386.tar.gz"
echo ""
echo "> Untar crane.."
echo ""
tar -zxvf go-containerregistry_Linux_i386.tar.gz

echo ""
echo "> Install crane.."
echo ""
sudo install -m 755 "$WORK_DIR/crane" /usr/local/bin/crane

rm -rf "$WORK_DIR"

crane version
