#!/bin/bash

has_unset=0
pairs=()

for key in "$@"; do
    value="${!key}"
    if [ -z "${value}" ]; then
        has_unset=1
        pairs+=("\t - ${key} = <UNSET>\n")
    else
        pairs+=("\t - ${key} = \"${value}\"\n")
    fi
done

if [[ "${has_unset}" == 1 ]]; then
    echo '=========================================================================================='
    printf "ERROR: ALL of the following environment variables must be set to non-empty values:\n%b" "${pairs[*]}"
    echo '=========================================================================================='
    exit 1
fi
