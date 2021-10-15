#!/bin/bash

#cd /Users/firegloves/workspace/firegloves/entando-process-driven-plugin-bundle
#kubectl delete -f CR.yml -n fire

cd /Users/firegloves/workspace/firegloves/mysqlbundle/
kubectl delete -f CR.yaml -n fire
#
#cd /Users/firegloves/workspace/firegloves/mys/
#kubectl delete -f CR.yaml -n fire

#cd /Users/firegloves/workspace/firegloves/entando-cms-quickstart-bundle
#kubectl delete -f CR.yml -n fire
#
cd /Users/firegloves/workspace/firegloves/the-lucas/
kubectl delete -f CRD.yaml -n fire

cd /Users/firegloves/workspace/firegloves/standard-demo-bundle/
kubectl delete -f CR.yaml -n fire
#
#cd /Users/firegloves/workspace/firegloves/seed-cms-bundle/
#kubectl delete -f CR.yaml -n fire
#
cd /Users/firegloves/workspace/firegloves/sixthree/
kubectl delete -f CR.yaml -n fire
#
## noovle pg bundle
#cd /Users/firegloves/workspace/firegloves/
#kubectl delete -f CR.yaml -n fire
#
#cd /Users/firegloves/workspace/firegloves/latest-version-bug/
#kubectl delete -f CR.yaml -n fire
#
#cd /Users/firegloves/workspace/firegloves/extracted-standard-demo-bundle/
#kubectl delete -f CR.yaml -n fire
#
#cd /Users/firegloves/workspace/firegloves/swaggertest-bundle/
#kubectl delete -f CR.yaml -n fire
#
#cd /Users/firegloves/workspace/firegloves
#kubectl delete -f eng1754.yml -n fire
