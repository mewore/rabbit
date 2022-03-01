#!/bin/bash

tasks_to_run=('frontend:frontendCheckDisabledLintRules' 'frontend:frontendLint' 'editor:jar' \
    'jar' 'frontend:frontendTest')
for java_module in 'core' 'backend' 'editor'; do
    tasks_to_run+=("${java_module}:spotbugsMain" "${java_module}:test")
done

echo "Running tasks: ${tasks_to_run[*]}"
# shellcheck disable=SC2086
./gradlew --parallel ${tasks_to_run[*]}
result=$?

echo "Result: ${result}"
exit $result
