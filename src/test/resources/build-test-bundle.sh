#!/bin/bash

cd test_bundle
find package -type f -print0 | xargs -0 tar czf ../bundle.tgz
cd ..
