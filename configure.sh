#!/bin/bash

#cd /Users/firegloves/workspace/firegloves/entando-process-driven-plugin-bundle
#kubectl apply -f CR.yml -n fire

cd /Users/firegloves/workspace/firegloves/mysqlbundle/
kubectl apply -f CR.yaml -n fire

#cd /Users/firegloves/workspace/firegloves/mys/
#kubectl apply -f CR.yaml -n fire

#cd /Users/firegloves/workspace/firegloves/entando-cms-quickstart-bundle
#kubectl apply -f CR.yml -n fire
#
cd /Users/firegloves/workspace/firegloves/the-lucas/
kubectl apply -f CRD.yaml -n fire

cd /Users/firegloves/workspace/firegloves/standard-demo-bundle/
kubectl apply -f CR.yaml -n fire
#
#cd /Users/firegloves/workspace/firegloves/seed-cms-bundle/
#kubectl apply -f CR.yaml -n fire
#
cd /Users/firegloves/workspace/firegloves/sixthree/
kubectl apply -f CR.yaml -n fire
#
## noovle pg bundle
#cd /Users/firegloves/workspace/firegloves/
#kubectl apply -f CR.yaml -n fire
#
#cd /Users/firegloves/workspace/firegloves/latest-version-bug/
#kubectl apply -f CR.yaml -n fire
#
#cd /Users/firegloves/workspace/firegloves/extracted-standard-demo-bundle/
#kubectl apply -f CR.yaml -n fire
#
#cd /Users/firegloves/workspace/firegloves/swaggertest-bundle/
#kubectl apply -f CR.yaml -n fire
#
#cd /Users/firegloves/workspace/firegloves
#kubectl apply -f eng1754.yml -n fire
