#!/bin/bash

### Moves the world editor from ./editor/build/...  to ./static/editors/... if the editor.jar is new

./launch/require-env-vars.sh EDITOR_JAR_PATH EDITOR_CHECKSUM_FILE SOURCE_BUILD_NUMBER || exit 1

if ! [ -e "${EDITOR_JAR_PATH}" ]; then
    echo "ERROR: File '${EDITOR_JAR_PATH}' does not exist!"
    exit 1
fi

echo 'Adding a new editor version if necessary...'
new_checksum=$(md5sum "${EDITOR_JAR_PATH}" | awk '{print $1;}')
echo "Current checksum of ${EDITOR_JAR_PATH}: ${new_checksum}"
if [ -e "${EDITOR_CHECKSUM_FILE}" ] && grep "${new_checksum}" <"${EDITOR_CHECKSUM_FILE}"; then
    echo "The old ${EDITOR_JAR_PATH} is the same as the new one (checksum: $(cat "${EDITOR_CHECKSUM_FILE}"))"
    exit 0
fi

target_dir="./static/editors/${SOURCE_BUILD_NUMBER}"
echo "New version detected. Moving it into ${target_dir}"
if ! [ -e "${target_dir}" ]; then
    mkdir -p "${target_dir}" || exit 1
fi

mv "${EDITOR_JAR_PATH}" "${target_dir}/rabbit-world-editor.jar" || exit 1

executable_dir="./editor/build/executable"

linux_tar_path="${executable_dir}/linux/editor-lin64.tar.gz"
mv "${linux_tar_path}" "${target_dir}/rabbit-world-editor-v${SOURCE_BUILD_NUMBER}-lin64.tar.gz" || exit 1

windows_zip_path="${executable_dir}/windows/editor-win64.zip"
mv "${windows_zip_path}" "${target_dir}/Rabbit World Editor v${SOURCE_BUILD_NUMBER} - win64.zip" || exit 1

echo "Done moving the new version into ${target_dir}"
echo "${new_checksum}" >"${EDITOR_CHECKSUM_FILE}"
