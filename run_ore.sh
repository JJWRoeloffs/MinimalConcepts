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
    timeout 5h $COMMAND $filepath 2>&1 | tee -a "logs/output_$CURRENT_DATETIME.jsonl"
done
