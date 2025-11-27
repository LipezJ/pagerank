#!/usr/bin/env python3
"""
Utility to generate seed data for the PageRank project.

Produces two CSV files with the schema expected by the SQLite tables:
    persons.csv -> id, name, spam_score, last_seen
    follows.csv -> id, src_id, dst_id, quality, last_seen

Person names are created by combining first names, middle names and last names.
For every generated person the script assigns a random spam score and a recent
last_seen timestamp. Then it iterates over the people list again and, using
random numbers, selects how many and which persons follow the current person.

Usage:
    python scripts/generate_data.py --persons-out data/persons.csv --follows-out data/follows.csv
"""

from __future__ import annotations

import argparse
import csv
import random
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Iterable, List, Sequence

FIRST_NAMES: Sequence[str] = (
    "Ana",
    "Luis",
    "Carla",
    "Miguel",
    "Sofia",
    "Javier",
    "Lucia",
    "Pedro",
    "Mariana",
)

MIDDLE_NAMES: Sequence[str] = (
    "Isabel",
    "Antonio",
    "Raul",
    "Elena",
    "Martin",
    "Andres",
)

LAST_NAMES: Sequence[str] = (
    "Lopez",
    "Rivera",
    "Gonzalez",
    "Torres",
    "Ramirez",
    "Fernandez",
    "Martinez",
    "Hernandez",
)


def build_persons() -> List[dict]:
    """Generates Cartesian combinations of names to create person rows."""
    people = []
    now = datetime.now(timezone.utc)
    person_id = 1
    for first in FIRST_NAMES:
        for middle in MIDDLE_NAMES:
            for last in LAST_NAMES:
                name = f"{first} {middle} {last}"
                last_seen = now - timedelta(minutes=random.randint(0, 60))
                people.append(
                    {
                        "id": person_id,
                        "name": name,
                        "spam_score": round(random.uniform(0, 1), 4),
                        "last_seen": last_seen.isoformat(),
                    }
                )
                person_id += 1
    return people


def build_follows(persons: Sequence[dict], max_followers: int) -> List[dict]:
    """Creates follow edges by picking random followers for each destination."""
    follows = []
    follow_id = 1
    ids = [p["id"] for p in persons]
    for person in persons:
        # Everyone except the destination is a candidate.
        candidates = [pid for pid in ids if pid != person["id"]]
        if not candidates:
            continue
        followers_count = random.randint(0, min(len(candidates), max_followers))
        chosen = random.sample(candidates, followers_count)
        for src_id in chosen:
            follows.append(
                {
                    "id": follow_id,
                    "src_id": src_id,
                    "dst_id": person["id"],
                    "quality": round(random.uniform(0.4, 1.0), 4),
                    "last_seen": person["last_seen"],
                }
            )
            follow_id += 1
    return follows


def write_csv(path: Path, rows: Iterable[dict], headers: Sequence[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=headers)
        writer.writeheader()
        writer.writerows(rows)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate sample data for PageRank tables.")
    parser.add_argument(
        "--persons-out",
        default="data/persons.csv",
        metavar="PATH",
        help="Destination CSV for the persons table (default: %(default)s)",
    )
    parser.add_argument(
        "--follows-out",
        default="data/follows.csv",
        metavar="PATH",
        help="Destination CSV for the follows table (default: %(default)s)",
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=42,
        help="Random seed to make the dataset reproducible (default: %(default)s)",
    )
    parser.add_argument(
        "--max-followers",
        type=int,
        default=15,
        help="Upper bound of followers assigned to each person (default: %(default)s)",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    random.seed(args.seed)

    persons = build_persons()
    follows = build_follows(persons, max_followers=max(args.max_followers, 0))

    write_csv(Path(args.persons_out), persons, ("id", "name", "spam_score", "last_seen"))
    write_csv(Path(args.follows_out), follows, ("id", "src_id", "dst_id", "quality", "last_seen"))

    print(f"Generated {len(persons)} persons -> {args.persons_out}")
    print(f"Generated {len(follows)} follows -> {args.follows_out}")


if __name__ == "__main__":
    main()
