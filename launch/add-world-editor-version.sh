#!/bin/bash

### Moves the world editor from ./editor/build/...  to ./static/editors/... if the editor.jar is new

./launch/require-env-vars.sh EDITOR_JAR_FILE SOURCE_BUILD_NUMBER || exit 1

editor_checksum_file="${EDITOR_JAR_FILE}.md5"

editor_jar_path="./editor/build/libs/${EDITOR_JAR_FILE}"
if ! [ -e "${editor_jar_path}" ]; then
    echo "ERROR: File '${editor_jar_path}' does not exist!"
    exit 1
fi

echo 'Adding a new editor version if necessary...'
new_checksum=$(md5sum "${editor_jar_path}" | awk '{print $1;}')
echo "Current checksum of ${EDITOR_JAR_FILE}: ${new_checksum}"
if [ -e "${editor_checksum_file}" ] && grep "${new_checksum}" <"${editor_checksum_file}"; then
    echo "The old ${EDITOR_JAR_FILE} is the same as the new one (checksum: $(cat "${editor_checksum_file}"))"
    exit 0
fi

target_dir="./static/editors/${SOURCE_BUILD_NUMBER}"
echo "New version detected. Moving it into ${target_dir}"
if ! [ -e "${target_dir}" ]; then
    mkdir -p "${target_dir}" || exit 1
fi

mv "${editor_jar_path}" "${target_dir}/rabbit-world-editor.jar" || exit 1

executable_dir="./editor/build/executable"

linux_tar_path="${executable_dir}/linux/editor-lin64.tar.gz"
mv "${linux_tar_path}" "${target_dir}/rabbit-world-editor-v${SOURCE_BUILD_NUMBER}-lin64.tar.gz" || exit 1

windows_zip_path="${executable_dir}/windows/editor-win64.zip"
mv "${windows_zip_path}" "${target_dir}/Rabbit World Editor v${SOURCE_BUILD_NUMBER} - win64.zip" || exit 1

echo "Done moving the new version into ${target_dir}"
echo "${new_checksum}" >"${editor_checksum_file}"
