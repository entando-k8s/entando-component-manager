#!/bin/sh

dbVendor=""
dbName=""
dbUser=""
dbPwd=""
dbHost=""
dbPort=""
kubeNamespace=""

# EXIT WITH ERROR DUE TO NOT RECOGNIZED DBMS VENDOR
function vendorNotRecognized() {
  echo "Not recognized dbms vendor received: ${dbVendor}. Allowed values are: mysql|postgresql|h2}"
  exit 1
}


# EXECUTE THE RECEIVED QUERY
function executeQuery() {
  query=$1

  case $dbVendor in
    mysql)
      res=$(MYSQL_PWD=${dbPwd} mysql -h "${dbHost}" -P "${dbPort}" -u "${dbUser}" -D "${dbName}" -B --disable-column-names -e "${query}");
      if [ ! -z "${res}" ]; then
        echo "${res}"
      fi
      ;;

    postgresql)
      res=$(psql -h "${dbHost}" -p "${dbPort}" -U "${dbUser}" -d "${dbName}" -t -c ${query})
      if [ ! -z "${res}" ]; then
        echo "${res}"
      fi
      ;;
  esac
}


# RETURNS THE QUERY TO SELECT THE IDs OF THE INSTALLED BUNDLES BASING ON THE DBMS VENDOR
function getQueryInstalledBundleIds() {

  case $dbVendor in
    mysql)
      echo "SELECT ID FROM installed_entando_bundles WHERE repo_url = '';"
      ;;

    postgresql)
      echo "SELECT ID FROM ${dbUser}.\"installed_entando_bundles\" WHERE repo_url = '';"
      ;;
  esac
}


# RETURNS THE QUERY TO UPDATE THE REPO URL BASING ON THE DBMS VENDOR
function getUpdateRepoUrlQuery() {

  repoUrl=$1
  bundleId=$2

  case $dbVendor in
    mysql)
      echo "UPDATE installed_entando_bundles SET repo_url = '${repoUrl}' WHERE ID = '${bundleId}';"
      ;;

    postgresql)
      echo "UPDATE ${dbUser}.\"installed_entando_bundles\" SET repo_url = '${repoUrl}' WHERE ID = '${bundleId}';"
      ;;
  esac
}



# MIGRATE DATA FROM ENTANDO 6 TO ENTANDO 7
function migrateDataToEntando7Tables() {
  case $dbVendor in
    mysql)
      query="INSERT INTO entando_bundle_jobs (id, component_id, component_name, component_version, started_at, finished_at, user_id, status, progress, install_error_message, install_error_code, rollback_error_code, rollback_error_message, install_plan, custom_installation) SELECT id, component_id, component_name, component_version, started_at, finished_at, user_id, status, progress, install_error_message, install_error_code, rollback_error_code, rollback_error_message, install_plan, custom_installation FROM entando_bundle_jobs_E6"
      executeQuery ${query}

      query="INSERT INTO installed_entando_bundles (id, description, image, installed, job_id, last_update, metadata, name, rating, signature, type, version, bundle_type, repo_url) SELECT id, description, image, installed, job_id, last_update, metadata, name, rating, signature, type, version, bundle_type, '' FROM installed_entando_bundles_E6;"
      executeQuery ${query}

      query="INSERT INTO entando_bundle_component_jobs (id, parent_entando_bundle_job_id, component_id, checksum, status, component_type, started_at, finished_at, action, install_error_message, install_error_code, rollback_error_code, rollback_error_message) SELECT id, parent_entando_bundle_job_id, component_id, checksum, status, component_type, started_at, finished_at, action, install_error_message, install_error_code, rollback_error_code, rollback_error_message FROM entando_bundle_component_jobs_E6;"
      executeQuery ${query}
      ;;

    postgresql)
      # entando_bundle_jobs
      query="INSERT INTO ${dbUser}.\"entando_bundle_jobs\" (id, component_id, component_name, component_version, started_at, finished_at, user_id, status, progress, install_error_message, install_error_code, rollback_error_code, rollback_error_message, install_plan, custom_installation) SELECT id, component_id, component_name, component_version, started_at, finished_at, user_id, status, progress, install_error_message, install_error_code, rollback_error_code, rollback_error_message, install_plan, custom_installation FROM ${dbUser}.\"entando_bundle_jobs_E6\"";
      executeQuery ${query}

      # installed_entando_bundles
      query="INSERT INTO ${dbUser}.\"installed_entando_bundles\" (id, description, image, installed, job_id, last_update, metadata, name, rating, signature, type, version, bundle_type, repo_url) SELECT id, description, image, installed, job_id, last_update, metadata, name, rating, signature, type, version, bundle_type, '' FROM ${dbUser}.\"installed_entando_bundles_E6\"";
      executeQuery ${query}

      # entando_bundle_component_jobs
      query="INSERT INTO ${dbUser}.\"entando_bundle_component_jobs\" (id, parent_entando_bundle_job_id, component_id, checksum, status, component_type, started_at, finished_at, action, install_error_message, install_error_code, rollback_error_code, rollback_error_message) SELECT id, parent_entando_bundle_job_id, component_id, checksum, status, component_type, started_at, finished_at, action, install_error_message, install_error_code, rollback_error_code, rollback_error_message FROM ${dbUser}.\"entando_bundle_component_jobs_E6\"";
      executeQuery ${query}
      ;;
  esac
}


# READ DATA FROM K8S AND POPULATE THE repo_url DB FIELD
function populateRepoUrl() {

  select_query=$(getQueryInstalledBundleIds)

  while IFS='' read -r value; do
      bundleId=$(echo "${value}" | xargs)
      echo "...querying repo_url for ${bundleId}"

      repoUrl=$(kubectl get EntandoDeBundle -n "${kubeNamespace}" -o=jsonpath="{.items[?(@.metadata.name==\"${bundleId}\")].spec.tags[0].tarball}")
      echo "...repoUrl ${repoUrl}\n"

      if [ ! -z "${repoUrl}" ]; then
          printf "Setting repository URL for bundle ${bundleId}...\n"
          update_query=$(getUpdateRepoUrlQuery "${repoUrl}" "${bundleId}")
          executeQuery "${update_query}"
          printf "...DONE\n"
      fi
  done <<<"$(executeQuery ${select_query})"
}



# PARSE SCRIPT PARAMS
while getopts v:u:p:h:o:n:d: flag
do
  case "${flag}" in
      v) dbVendor=${OPTARG};;
      d) dbName=${OPTARG};;
      u) dbUser=${OPTARG};;
      p) dbPwd=${OPTARG};;
      h) dbHost=${OPTARG};;
      o) dbPort=${OPTARG};;
      n) kubeNamespace=${OPTARG};;
      *) echo "usage: $0 [-v db vendor (mysql|postgresql|h2)] [-u db username] [-p db password] [-h db hostname] [-o db port] [-n kube namespace]\n" >&2
         exit 1 ;;
  esac
done

echo "##### SUPPLIED VALUES #####"
echo "dbVendor ${dbVendor}"
echo "dbName ${dbName}"
echo "dbUser ${dbUser}"
echo "dbPwd ${dbPwd}"
echo "dbHost ${dbHost}"
echo "dbPort ${dbPort}"
echo "kubeNamespace ${kubeNamespace}\n"


case $dbVendor in
  mysql)
    ;;

  postgresql)
    PGPASSWORD="${dbPwd}"
    ;;

  *)
    vendorNotRecognized
    ;;
esac

# MIGRATE DATA
migrateDataToEntando7Tables

# POPULATE REPO URL
populateRepoUrl

echo "##### MIGRATION COMPLETE #####"
