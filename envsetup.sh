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
local_file="${script_dir}/envsetup.local.properties"

load_env_file() {
  local config_file="$1"
  local override_existing="$2"

  [[ -f "${config_file}" ]] || return 0

  while IFS='=' read -r key value || [[ -n "${key}" ]]; do
    [[ -z "${key}" || "${key}" == \#* ]] && continue
    if [[ ! "${key}" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
      echo "Skipping invalid env key in ${config_file}: ${key}" >&2
      continue
    fi

    if [[ "${override_existing}" == "true" ]]; then
      export "${key}=${value}"
      continue
    fi

    eval "is_set=\${${key}+x}"
    if [[ -z "${is_set}" ]]; then
      export "${key}=${value}"
    fi
  done < "${config_file}"
}

load_env_file "${defaults_file}" false
load_env_file "${local_file}" true

export SQLSERVER_JDBC_URL="${SQLSERVER_JDBC_URL:-jdbc:sqlserver://localhost:1433;databaseName=${SQLSERVER_DATABASE};encrypt=true;trustServerCertificate=true}"
