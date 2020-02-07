#!/bin/bash

find bundle -type f -print0 | xargs -0 tar czf bundle.tgz