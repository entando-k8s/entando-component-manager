# Entando K8s Service
This service serves as an abstraction layer to the Kubernetes Plugin custom resource. It is meant to be used by `entando-core` to deploy and check status of the plugins on the cluster.

## Install

You'll need a Kubernetes cluster running, configure the environments described down below and execute the project.

## Environment Variables
>- `KUBERNETES_NAMESPACE`: The kubernetes namespace that this service is in. Defaults to `entando`.
>- `KUBERNETES_ENTANDO_APP_NAME`: The entando app name that this service is in. Defaults to `entando-dev`.

