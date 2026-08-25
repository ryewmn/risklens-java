#!/usr/bin/env python3
from __future__ import annotations

import json
import unittest
from pathlib import Path

from training.score_artifact import score

ROOT = Path(__file__).resolve().parents[1]


class ParityTest(unittest.TestCase):
    def test_reference_cases(self) -> None:
        artifact = json.loads((ROOT / "src/main/resources/models/logistic-v1.json").read_text())
        cases = json.loads((ROOT / "src/test/resources/parity-cases.json").read_text())
        for case in cases:
            with self.subTest(case=case["name"]):
                self.assertAlmostEqual(score(artifact, case["input"]), case["expectedProbability"], places=12)


if __name__ == "__main__":
    unittest.main()
