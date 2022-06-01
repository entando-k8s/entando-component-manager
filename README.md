[![Build Status](https://img.shields.io/endpoint?url=https%3A%2F%2Fstatusbadge-jx.apps.serv.run%2Fentando-k8s%2Fentando-component-manager)](https://github.com/entando-k8s/devops-results/tree/logs/jenkins-x/logs/entando-k8s/entando-component-manager/master)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=entando-k8s_entando-component-manager&metric=alert_status)](https://sonarcloud.io/dashboard?id=entando-k8s_entando-component-manager)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=entando-k8s_entando-component-manager&metric=coverage)](https://entando-k8s.github.io/devops-results/entando-component-manager/master/jacoco/index.html)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=entando-k8s_entando-component-manager&metric=vulnerabilities)](https://entando-k8s.github.io/devops-results/entando-component-manager/master/dependency-check-report.html)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=entando-k8s_entando-component-manager&metric=code_smells)](https://sonarcloud.io/dashboard?id=entando-k8s_entando-component-manager)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=entando-k8s_entando-component-manager&metric=security_rating)](https://sonarcloud.io/dashboard?id=entando-k8s_entando-component-manager)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=entando-k8s_entando-component-manager&metric=sqale_index)](https://sonarcloud.io/dashboard?id=entando-k8s_entando-component-manager)
# NOTE

This branch implements the env variables in the plugin descriptor that was developed (here)[https://github.com/entando-k8s/entando-component-manager/tree/22015270f11cfdc75500f15f61d046db6e7181f4]

You need to compile the project with `mvn -U clean package` and then run `docker build --tag entandodemo/entando-component-manager:6.3.26-ps.backport.1 -f Dockerfile .`

# Entando Component Manager
This service serves as an abstraction layer for using Digital-exchange functionalities.
EntandoDeBundles served in the cluster can be installed in an EntandoApp using this service. This service relies on the Entando K8S service for interaction with the K8S cluster

## Install

You'll need a Kubernetes cluster running, configure the environments described down below and execute the project.

## Environment Variables
| Env variable                                                  | Description                                                                                                   |
| :---                                                          | :---                                                                                                          |
| SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_OIDC_ISSUER-URI        | The issuer of the token, e.g http://insecure-keycloak-cacms.apps.serv.run/auth/realms/entando                 |
| SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_OIDC_CLIENT-ID     | The client id for the service                                                                                 |
| SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_OIDC_CLIENT-SECRET | The client secret                                                                                             |
| DB_VENDOR                                                     | Which database will be used. Default `postgres`                                                               |
| DB_HOST                                                       | Database host. Default `localhost`                                                                            |
| DB_PORT                                                       | Database port. Default `5432`                                                                                 |
| DB_NAME                                                       | Database name. Default `digital_exchange`                                                                     |
| DB_OPTIONS                                                    | Database options. Default `useSSL=false`                                                                      |
| DB_USER                                                       | Database user. Default `admin`                                                                                |
| DB_PASS                                                       | Database password. Default `admin`                                                                            |
| ENTANDO_URL                                                   | The URL to access the Entando App instance.                                                                   |
| ENTANDO_APP_NAMESPACE                                         | The kubernetes namespace where the entando app is running. Default to `test-namespace`;                       |
| ENTANDO_APP_NAME                                              | The entando app name that this service is in. Defaults to `test-entando`.                                     |
| ENTANDO_BUNDLE_TYPE                                           | The bundle type that should be handled by this service. It can be `git` or `npm`. The default value is `git`. |

