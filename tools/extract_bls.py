#!/usr/bin/env python3
"""Extract the macro columns we use from the official BLS 4.0 workbook.

BLS 4.0 (Bundeslebensmittelschlüssel) is published by the Max Rubner-Institut
under CC BY 4.0: https://blsdb.de

The workbook ships as BLS_4_0_2025_DE.zip. Download and unzip it, then run:

    python3 tools/extract_bls.py \
        /path/to/BLS_4_0_2025_DE/BLS_4_0_Daten_2025_DE.xlsx \
        app/src/main/assets/bls_foods.tsv

Output columns (tab separated, no header):
    German name, kcal/100 g, protein/100 g, fat/100 g, carbs/100 g

The parser only uses the Python standard library so it works without openpyxl.
"""

import csv
import os
import re
import sys
import zipfile
from xml.etree import ElementTree as ET

NS = "{http://schemas.openxmlformats.org/spreadsheetml/2006/main}"
# A = BLS code, B = German name, G = kcal, M = protein, P = fat, S = carbs.
COLUMNS = {"A": 0, "B": 1, "G": 2, "M": 3, "P": 4, "S": 5}


def shared_strings(zf):
    if "xl/sharedStrings.xml" not in zf.namelist():
        return []
    root = ET.fromstring(zf.read("xl/sharedStrings.xml"))
    return [
        "".join(node.text or "" for node in si.iter(f"{NS}t"))
        for si in root.findall(f"{NS}si")
    ]


def rows(workbook):
    strings = shared_strings(workbook)
    with workbook.open("xl/worksheets/sheet1.xml") as sheet:
        for _, element in ET.iterparse(sheet, events=("end",)):
            if element.tag != f"{NS}row":
                continue
            number = element.get("r")
            if number and int(number) > 1:
                values = {}
                for cell in element.findall(f"{NS}c"):
                    column = re.match(r"[A-Z]+", cell.get("r", "")).group(0)
                    if column not in COLUMNS:
                        continue
                    node = cell.find(f"{NS}v")
                    if node is None or node.text is None:
                        continue
                    value = node.text
                    if cell.get("t") == "s":
                        value = strings[int(value)]
                    values[COLUMNS[column]] = value
                if 0 in values and 1 in values:
                    yield [values.get(index, "") for index in range(6)]
            element.clear()


def number(text):
    try:
        return float(text.replace(",", "."))
    except (TypeError, ValueError):
        return 0.0


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        return 1
    source, target = sys.argv[1], sys.argv[2]
    os.makedirs(os.path.dirname(target), exist_ok=True)

    seen = set()
    written = 0
    with zipfile.ZipFile(source) as workbook, open(target, "w", newline="", encoding="utf-8") as output:
        writer = csv.writer(output, delimiter="\t", lineterminator="\n")
        for code, name, kcal, protein, fat, carbs in rows(workbook):
            name = " ".join(name.split())
            calories = round(number(kcal))
            if not name or calories <= 0 or calories > 1500:
                continue
            key = (name.lower(), calories)
            if key in seen:
                continue
            seen.add(key)
            writer.writerow([
                name,
                calories,
                f"{number(protein):.2f}",
                f"{number(fat):.2f}",
                f"{number(carbs):.2f}",
            ])
            written += 1

    print(f"Wrote {written} foods to {target}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
