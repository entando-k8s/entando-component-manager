#!/bin/bash
DIR=${1:-bundle}
F_OUT=${2:-bundle.tgz}
WD=$(pwd)
OUTPUT="$WD/$F_OUT"

echo "Compressing folder $DIR into $F_OUT"

cd "$DIR" || exit
find . -type f -print0 | xargs -0 tar czf "$OUTPUT"
cd ..
