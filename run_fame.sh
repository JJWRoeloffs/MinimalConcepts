#!/usr/bin/env bash
COMMAND="java -jar -Xms1G -Xmx20G ./minimiser/target/MinimalConcepts-1.0-SNAPSHOT.jar"
CURRENT_DATETIME="$(date +'%Y-%m-%d_%H:%M:%S')"
mkdir -p logs

cleanup() {
    echo "Interupted. Killing all $COMMAND processes"
    pkill -f "$COMMAND"
    exit 1
}

trap cleanup SIGINT

# mvn clean package

python3 sort_ontologies.py | while IFS= read -r filepath; do
    java -jar -Xms1G -Xmx20G --add-opens java.base/java.lang=ALL-UNNAMED ./fame-wrapper/target/FameWrapper-1.0-SNAPSHOT.jar $filepath
    timeout 1h $COMMAND FAME_ontology.owl 2>&1 | tee -a "logs/output_FAME_$CURRENT_DATETIME.jsonl"
done
