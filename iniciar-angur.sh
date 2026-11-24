#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
if ! command -v java >/dev/null 2>&1; then
    printf '%s\n' 'Instala un JDK 21 o posterior y vuelve a ejecutar este archivo.' >&2
    exit 1
fi
exec java scripts/IniciarAngur.java "$@"
