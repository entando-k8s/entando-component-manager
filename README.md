[![Development Status](https://github.com/entando-k8s/entando-component-manager/actions/workflows/pr.yml/badge.svg)](https://github.com/entando-k8s/entando-component-manager/actions/workflows/pr.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=entando-k8s_entando-component-manager&metric=alert_status)](https://sonarcloud.io/dashboard?id=entando-k8s_entando-component-manager)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=entando-k8s_entando-component-manager&metric=coverage)](https://entando-k8s.github.io/devops-results/entando-component-manager/master/jacoco/index.html)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=entando-k8s_entando-component-manager&metric=vulnerabilities)](https://entando-k8s.github.io/devops-results/entando-component-manager/master/dependency-check-report.html)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=entando-k8s_entando-component-manager&metric=code_smells)](https://sonarcloud.io/dashboard?id=entando-k8s_entando-component-manager)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=entando-k8s_entando-component-manager&metric=security_rating)](https://sonarcloud.io/dashboard?id=entando-k8s_entando-component-manager)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=entando-k8s_entando-component-manager&metric=sqale_index)](https://sonarcloud.io/dashboard?id=entando-k8s_entando-component-manager)

# Entando Component Manager
This service serves as an abstraction layer for using Digital-exchange functionalities.
EntandoDeBundles served in the cluster can be installed in an EntandoApp using this service. This service relies on the Entando K8S service for interaction with the K8S cluster

## How to run locally

### Prerequisites
 - An Entando cluster already running or the required components reachable
 - `crane` installed locally and in the system path; crane can be installed from the [following URL](https://github.com/google/go-containerregistry/tree/main/cmd/crane).

### Steps

- Rename `application-dev.properties.template` to `application-dev.properties` and change relevant properties
- Fetch k8s JWT token from the pod of the component manager running in your cluster instance inside `/var/run/secrets/kubernetes.io/serviceaccount/token` and place it locally in the same path or eventually set the environment variable SERVICE_ACCOUNT_TOKEN_PATH
- Start Spring application with `dev` profile

## Environment Variables
| Group | Name | Value [default] | Description |
| :---   | :--- | :--- |:--- |
| General | ENTANDO_URL                 |  | The URL to access the Entando App instance                            |
|     | ENTANDO_APP_NAMESPACE                                        | [test-namespace] | The kubernetes namespace where the entando app is running |
|     | ENTANDO_APP_NAME                                             | [test-entando] | The entando app name that this service is in |
|    | ENTANDO_ECR_DEAPP_REQUEST_RETRIES    | [3] |  Number of times the CM retries the component create/update before giving up     |
|    | ENTANDO_ECR_DEAPP_REQUEST_BACKOFF       | [5] |  Seconds to wait before the next attempt is executed     |
|    | ENTANDO_ECR_POSTINIT        |  |  Configuration of the postinit process      |
|    | ENTANDO_CONTAINER_REGISTRY_CREDENTIALS   | [null] | Credentials for each container registry    |
|    | MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE     | [not enabled] |  To enable component-manager /actuator/info     |
|Spring Security| SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_OIDC_ISSUER-URI  | |  The issuer of the token, e.g., http://insecure-keycloak-cacms.apps.serv.run/auth/realms/entando                 |
|     |SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_OIDC_CLIENT-ID   |  | The client id for the service                                                                                 |
|     |SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_OIDC_CLIENT-SECRET| | The client secret         |
| Database | DB_VENDOR             | [postgres] | Which database will be used     |
|    | DB_HOST      | [localhost] | Database host |
|    | DB_PORT  | [5432] | Database port |
|    | DB_NAME    | [digital_exchange] | Database name |
|    | DB_OPTIONS           | [useSSL=false] | Database options |
|    | DB_USER       | [admin] | Database user |
|    | DB_PASS        | [admin] | Database password |
| Bundle Related | ENTANDO_BUNDLE_TAGS_TYPES       | dev, [prod] | To generate EntandoDeBundle CRs using tags to select for dev, prod, or both |
|     | ENTANDO_BUNDLE_TYPE           | npm, [git] | The bundle type that should be handled by this service |
|    | ENTANDO_BUNDLE_DOWNLOAD_TIMEOUT    | [300] | Download timeout in seconds   |
|    | ENTANDO_BUNDLE_DOWNLOAD_RETRIES   | [3] |  Max download attempts         |
|    | ENTANDO_BUNDLE_DECOMPRESS_TIMEOUT | [600] |  Decompress timeout in seconds     |
|    |          | [] |       |

