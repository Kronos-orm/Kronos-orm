#!/bin/bash

script_path="${BASH_SOURCE[0]}"
script_dir="$(cd "$(dirname "$script_path")" && pwd)"
defaults_file="${script_dir}/envsetup.defaults"

if [[ -f "${defaults_file}" ]]; then
  while IFS='=' read -r key value || [[ -n "${key}" ]]; do
    [[ -z "${key}" || "${key}" == \#* ]] && continue
    if [[ -z "${!key+x}" ]]; then
      export "${key}=${value}"
    fi
  done < "${defaults_file}"
fi

export SQLSERVER_JDBC_URL="${SQLSERVER_JDBC_URL:-jdbc:sqlserver://localhost:1433;databaseName=${SQLSERVER_DATABASE};encrypt=true;trustServerCertificate=true}"
