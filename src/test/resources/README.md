# How to update the bundle for testing purposes

This folder provides a `build-test-bundle.sh` shell-script.

Purpose of this script is to tar zip a folder in the way ECR will expect the bundle to be created.

**If you need to add/remove components from the bundle.tgz file, do your changes to the `src/test/resources/bundle` structure, and then simply run from `src/test/resources` the build-test-bundle.sh` script.**

Here an example on how to run the script
```
./build-test-bundle.sh [bundle-directory] [output-file]
```
*Note: both bundle-directory and output-file parameters are optional. By default they use the `bundle` folder and output a `bundle.tgz` file*



