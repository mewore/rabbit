#!/bin/bash

### Returns 0 if the file is new (judging by the stored checksum) and 10 if it is not

./launch/require-env-vars.sh FILE CHECKSUM_FILE || exit 1
if ! [ -e "${CHECKSUM_FILE}" ]; then
    echo "The file ${FILE} is assumed to be new because the checksum file ${CHECKSUM_FILE} does not exist."
    exit 0
fi

new_checksum=$(md5sum "${FILE}" | awk '{print $1;}')
echo "Current checksum of ${FILE}: ${new_checksum}"
if grep "${new_checksum}" <"${CHECKSUM_FILE}"; then
    echo "The old ${FILE} is the same as the new one (checksum: $(cat "${CHECKSUM_FILE}"))"
    exit 10
fi
echo "The file ${FILE} (checksum: ${new_checksum}) is new. The old checksum is: $(cat "${CHECKSUM_FILE}")"
