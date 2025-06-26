mvn clean package

# This is the most inefficient way to do it imaginable. Starting up a clean jvm for each.
# Thing is, if we don't, the memory leaks get us just the same way as they get us for the other runscript.
python3 sort_ontologies.py | while IFS= read -r filepath; do
    java -cp ./target/MinimalConcepts-1.0-SNAPSHOT.jar edu.vuamsterdam.MinimalConcepts.MinimizableCounter $filepath 2>&1 | tee -a "logs/output_ontology_sizes.jsonl"
done
