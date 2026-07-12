#!/usr/bin/env bash

if [[ -n "${BASH_SOURCE:-}" ]]; then
  script_path="${BASH_SOURCE[0]}"
elif [[ -n "${ZSH_VERSION:-}" ]]; then
  script_path="${(%):-%N}"
else
  script_path="$0"
fi

script_dir="$(cd "$(dirname "$script_path")" && pwd)"
defaults_file="${script_dir}/envsetup.defaults"

if [[ -f "${defaults_file}" ]]; then
  while IFS='=' read -r key value || [[ -n "${key}" ]]; do
    [[ -z "${key}" || "${key}" == \#* ]] && continue
    if [[ ! "${key}" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
      echo "Skipping invalid env key in ${defaults_file}: ${key}" >&2
      continue
    fi

    eval "is_set=\${${key}+x}"
    if [[ -z "${is_set}" ]]; then
      export "${key}=${value}"
    fi
  done < "${defaults_file}"
fi

export SQLSERVER_JDBC_URL="${SQLSERVER_JDBC_URL:-jdbc:sqlserver://localhost:1433;databaseName=${SQLSERVER_DATABASE};encrypt=true;trustServerCertificate=true}"
