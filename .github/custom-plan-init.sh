#!/bin/bash


ENTANDO_SKOPEO_VERSION="$(
  grep "ENV\s*ENTANDO_SKOPEO_VERSION" Dockerfile | cut -d'=' -f 2
)"

chmod a+x build-skopeo.sh
mkdir -p "$HOME/.entando/skopeo";
./build-skopeo.sh "$HOME/.entando/skopeo" "$ENTANDO_SKOPEO_VERSION";

#cp "$HOME/.entando/skopeo/src/github.com/containers/skopeo/bin/skopeo" "/usr/local/bin"
#chmod a+x "/usr/local/bin/skopeo"

sudo install -m 755 \
  "$HOME/.entando/skopeo/src/github.com/containers/skopeo/bin/skopeo" \
  /usr/local/bin/skopeo

if [ "$ENTANDO_IN_REAL_PIPELINE" = "true" ]; then
  sudo install -m 644 \
    "$HOME/.entando/skopeo/src/github.com/containers/skopeo/default-policy.json" \
    /etc/containers/policy.json
  sudo install -m 644 \
    "$HOME/.entando/skopeo/src/github.com/containers/skopeo/default.yaml" \
    /etc/containers/registries.d/default.yaml
else
  echo ""
  echo "############################################################################"
  echo "> WARNING: skopeo installation only partially complete"
  echo "> reason: not allowed to install system files during non-pipeline executions"
  echo "############################################################################"
  echo ""
fi

rm -rf "$HOME/.entando/skopeo"

skopeo -v
