import sys
import json
from pathlib import Path

import matplotlib.pyplot as plt

FIGDIR = Path("figures")
FIGDIR.mkdir(exist_ok=True)

lines = Path(sys.argv[1]).read_text().split("\n")
json_objects = [json.loads(line) for line in lines if line]
values = [obj["data"]["sizeTBox"] for obj in json_objects]
minimizable = [obj["data"]["minimizable"] for obj in json_objects]

x = list(range(len(json_objects)))
colors = ["tab:orange" if i < 100 else "tab:blue" for i in range(len(values))]

fig, axs = plt.subplots(2, 1, figsize=(12, 10), sharex=True)

axs[0].bar(x, values, color=colors)
axs[0].set_yscale("log")
axs[0].set_ylabel("TBox size (log scale)")
axs[0].grid(True, which="both", linestyle="--", linewidth=0.5)
axs[0].tick_params(axis="x", bottom=False, labelbottom=False)

axs[1].bar(x, minimizable, color=colors)
axs[1].set_yscale("log")
axs[1].set_ylabel("Nr. Minimizable Expressions (log scale)")
axs[1].grid(True, which="both", linestyle="--", linewidth=0.5)
axs[1].tick_params(axis="x", bottom=False, labelbottom=False)

fig.suptitle(
    "TBox size and numberr of minimizable expressions (Orange ontologies (first 100) were used for expriments)"
)

plt.tight_layout(rect=[0, 0.05, 1, 1])
plt.savefig(FIGDIR / "ontology_sizes.png")
