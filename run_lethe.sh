#!/usr/bin/env bash
COMMAND="java -jar -Xms1G -Xmx20G ./target/MinimalConcepts-1.0-SNAPSHOT.jar"
CURRENT_DATETIME="$(date +'%Y-%m-%d_%H:%M:%S')"
mkdir -p logs

cleanup() {
    echo "Interupted. Killing all $COMMAND processes"
    pkill -f "$COMMAND"
    exit 1
}

trap cleanup SIGINT

mvn clean package

python3 sort_ontologies.py | while IFS= read -r filepath; do
    java --add-opens java.base/java.lang=ALL-UNNAMED -cp ./LETHE/lethe-standalone-0.6/lethe-standalone-0.6.jar uk.ac.man.cs.lethe.internal.application.ForgettingConsoleApplication --owlFile $filepath --timeOut 60 --interpolate --sigSize 10
    timeout 15m $COMMAND result.owl $filepath 2>&1 | tee -a "logs/output_LETHE_$CURRENT_DATETIME.jsonl"
done
