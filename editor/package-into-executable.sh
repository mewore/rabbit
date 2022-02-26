#!/bin/bash

if [[ "${LINUX_JAVA_PATH}" == '' ]]; then
    echo 'There is no Linux JDK path environment variable set! (LINUX_JAVA_PATH)'
    exit 1
fi

if [[ "${WINDOWS_JAVA_PATH}" == '' ]]; then
    echo 'There is no Windows JDK path environment variable set! (WINDOWS_JAVA_PATH)'
    exit 1
fi

if [[ "${LAUNCH4J_PATH}" == '' ]]; then
    echo 'There is no Launch4j path environment variable set! (LAUNCH4J_PATH)'
    exit 1
fi

WORKING_DIR="$(pwd)"
PATH_TO_JAR="./build/libs/editor.jar"
if ! [ -f "${PATH_TO_JAR}" ]; then
    echo "The .jar does not exist at ${PATH_TO_JAR}"
    exit 1
fi

WORKING_PATH="./build/executable-jar"
mkdir -p WORKING_PATH || exit 1

JAR_CHECKSUM_FILE="${WORKING_PATH}/editor.jar.md5"
jar_checksum=$(md5sum < "${PATH_TO_JAR}")
if [ -f "${JAR_CHECKSUM_FILE}" ] && [[ "${jar_checksum}" == "$(cat "${JAR_CHECKSUM_FILE}")" ]]; then
    echo "The .jar is exactly the same as before"
    exit 0
fi

MAIN_DIR_NAME="rabbit-world-editor"
LINUX_PATH="${WORKING_PATH}/linux64/${MAIN_DIR_NAME}"
WINDOWS_PATH="${WORKING_PATH}/windows64/${MAIN_DIR_NAME}"
mkdir -p LINUX_PATH || exit 1
mkdir -p WINDOWS_PATH || exit 1

DEPENDENCIES_CHECKSUM_FILE="${WORKING_PATH}/dependencies.md5"
dependencies=$("${LINUX_JAVA_PATH}/bin/jdeps" "${PATH_TO_JAR}" | awk '{print $4}' | sort | uniq | grep 'java.')
dependencies_checksum=$(echo "${dependencies}" | md5sum)
if [ -f "${DEPENDENCIES_CHECKSUM_FILE}" ] &&
    [[ "${dependencies_checksum}" == "$(cat "${DEPENDENCIES_CHECKSUM_FILE}")" ]]; then
    echo "The dependencies are the same so no need to make new JREs"
else
    comma_separated_dependencies=$(echo "${dependencies}" | sed -z 's/\n/,/g;s/,$//')
    echo "Generating JREs with the following dependencies: ${comma_separated_dependencies}"

    echo "Generating Linux JRE..."
    LINUX_JRE_PATH="${LINUX_PATH}/jre"
    rm -rf "${LINUX_JRE_PATH}" || exit 1
    "${LINUX_JAVA_PATH}/bin/jlink" --add-modules "${comma_separated_dependencies}" \
        --strip-debug --no-man-pages --no-header-files --output "${LINUX_JRE_PATH}" || exit 1

    echo "Generating Windows JRE..."
    WINDOWS_JRE_PATH="${WINDOWS_PATH}/jre"
    rm -rf "${WINDOWS_JRE_PATH}" || exit 1
    wine "${WINDOWS_JAVA_PATH}/bin/jlink.exe" --add-modules "${comma_separated_dependencies}" \
        --strip-debug --no-man-pages --no-header-files --output "${WINDOWS_JRE_PATH}" || exit 1

    echo "Done"
    echo -n "${dependencies_checksum}" >"${DEPENDENCIES_CHECKSUM_FILE}"
fi

echo "Creating editor.exe"
"${LAUNCH4J_PATH}/launch4jc" ./launch4j-config.xml || exit 1

echo "Creating editor-win64.zip"
cd "${WINDOWS_PATH}/.." && zip -r editor-win64.zip "./${MAIN_DIR_NAME}" && cd "${WORKING_DIR}" || exit 1

echo "Copying editor.sh and editor.jar"
pwd
cp ./editor.sh "${LINUX_PATH}" || exit 1
cp "${PATH_TO_JAR}" "${LINUX_PATH}" || exit 1

echo "Creating editor-lin64.tar.gz"
cd "${LINUX_PATH}/.." && tar -zcvf editor-lin64.tar.gz "${MAIN_DIR_NAME}" && cd "${WORKING_DIR}" || exit 1

echo -n "${jar_checksum}" >"${JAR_CHECKSUM_FILE}"
