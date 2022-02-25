#!/bin/bash

./gradlew core:jar
result=$?

process_ids=()
for backend_module in 'core' 'backend' 'editor'; do
    ./gradlew "${backend_module}:spotbugsMain" "${backend_module}:test" &
    process_ids+=($!)
done
./gradlew frontend:frontendLint frontend:frontendTest &
process_ids+=($!)
./gradlew editor:jar &
process_ids+=($!)
./gradlew jar &
process_ids+=($!)

for pid in ${process_ids[*]}; do
    wait "${pid}"
    new_result=$?
    if [[ "${result}" == '0' ]]; then
        result="${new_result}";
    fi
done

echo "Result: ${result}"
exit $result
