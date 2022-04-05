# Description

This script automates component-manager's data migration from Entando 6 to Entando 7.
It must be run after the update of the Entando app to v7+.
It copies data from Entando 6 tables to Entando 7 tables.
It fetches, where possible, the bundle repository URL from the cluster and copies it inside the relative database field.

### Supported DBMS

- Postgresql
- MySQL

### Prerequisites

The CLI tool to interact with your desired DBMS must be installed on your workstation and it must be reachable on the path.
To interact with Postgresql `psql` will be used.
To interact with MySQL `mysql` will be used.

The target DB must be reachable.

The `kubectl` CLI must be installed on your workstation and it must be reachable on the path.

The cluster where your Entando is installed must be reachable.

### Usage

`cm-e6-to-e7-migration.sh [-v <db vendor> (mysql|postgresql)] [-u <db username>] [-p <db password>] [-h <db hostname>] [-o <db port>] [-n <kube namespace>]`
