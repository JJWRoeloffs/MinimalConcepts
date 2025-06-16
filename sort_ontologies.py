import csv
from pathlib import Path

FILE = Path("pool_sample/pure_dl/classification/metadata.csv")
DIR = Path("pool_sample/files/")


def main():
    with FILE.open(newline="") as f:
        files = list(csv.DictReader(f))
    for file in sorted(files, key=lambda x: int(x["tbox_size"])):
        print(DIR / file["ore2015_filename"])


if __name__ == "__main__":
    main()
