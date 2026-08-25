#!/usr/bin/env python3
"""Reproducible, dependency-free training/export pipeline for RiskLens.

The default path generates a deterministic synthetic fixture. Pass --uci-csv
only after independently obtaining the UCI Default of Credit Card Clients data
and complying with its current license and terms. This repository does not
download or redistribute that dataset.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
import random
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable

RAW_COLUMNS = (
    "LIMIT_BAL", "AGE", "PAY_0", "PAY_2", "BILL_AMT1", "BILL_AMT2",
    "PAY_AMT1", "PAY_AMT2",
)
TARGET_ALIASES = ("default.payment.next.month", "default payment next month", "DEFAULT")
FEATURES = (
    "age", "logCreditLimit", "repaymentStatusCurrent", "repaymentStatusPrior",
    "utilizationCurrent", "utilizationPrior", "paymentRatioCurrent",
    "paymentRatioPrior", "billTrend",
)
REASONS = {
    "age": "LIMITED_AGE_STABILITY_SIGNAL",
    "logCreditLimit": "LOW_CREDIT_CAPACITY_REFERENCE",
    "repaymentStatusCurrent": "CURRENT_PAYMENT_DELAY",
    "repaymentStatusPrior": "PRIOR_PAYMENT_DELAY",
    "utilizationCurrent": "HIGH_CURRENT_UTILIZATION",
    "utilizationPrior": "HIGH_PRIOR_UTILIZATION",
    "paymentRatioCurrent": "LOW_CURRENT_PAYMENT_RATIO",
    "paymentRatioPrior": "LOW_PRIOR_PAYMENT_RATIO",
    "billTrend": "RISING_BALANCE_TREND",
}


@dataclass(frozen=True)
class Example:
    values: tuple[float, ...]
    target: int


def sigmoid(value: float) -> float:
    if value >= 0:
        return 1.0 / (1.0 + math.exp(-value))
    exp_value = math.exp(value)
    return exp_value / (1.0 + exp_value)


def feature_engineer(row: dict[str, float]) -> tuple[float, ...]:
    limit = max(1.0, row["LIMIT_BAL"])
    bill1, bill2 = row["BILL_AMT1"], row["BILL_AMT2"]
    return (
        row["AGE"], math.log1p(limit), row["PAY_0"], row["PAY_2"],
        max(0.0, bill1) / limit, max(0.0, bill2) / limit,
        row["PAY_AMT1"] / max(1.0, abs(bill1)),
        row["PAY_AMT2"] / max(1.0, abs(bill2)),
        (bill1 - bill2) / limit,
    )


def synthetic_examples(count: int, seed: int) -> list[Example]:
    rng = random.Random(seed)
    examples: list[Example] = []
    for _ in range(count):
        limit = rng.choice((20000, 50000, 80000, 120000, 200000, 350000, 500000))
        age = min(79, max(21, round(rng.gauss(39, 11))))
        pay0 = rng.choices((-2, -1, 0, 1, 2, 3, 4), (4, 12, 59, 8, 11, 4, 2))[0]
        pay2 = rng.choices((-2, -1, 0, 1, 2, 3, 4), (5, 14, 57, 8, 10, 4, 2))[0]
        utilization = min(1.8, max(-0.05, rng.gauss(0.34 + 0.07 * max(pay0, 0), 0.27)))
        prior_utilization = min(1.8, max(-0.05, utilization + rng.gauss(-0.01, 0.12)))
        bill1 = round(limit * utilization, 2)
        bill2 = round(limit * prior_utilization, 2)
        pay_amount1 = round(max(0, bill1) * min(1.2, max(0, rng.gauss(0.16, 0.18))), 2)
        pay_amount2 = round(max(0, bill2) * min(1.2, max(0, rng.gauss(0.18, 0.20))), 2)
        raw = {
            "LIMIT_BAL": limit, "AGE": age, "PAY_0": pay0, "PAY_2": pay2,
            "BILL_AMT1": bill1, "BILL_AMT2": bill2,
            "PAY_AMT1": pay_amount1, "PAY_AMT2": pay_amount2,
        }
        logit = (
            -1.45 + 0.72 * max(pay0, 0) + 0.42 * max(pay2, 0)
            + 1.08 * max(utilization - 0.55, 0) - 0.95 * (pay_amount1 / max(1, abs(bill1)))
            - 0.17 * math.log1p(limit / 50000) + rng.gauss(0, 0.28)
        )
        examples.append(Example(feature_engineer(raw), int(rng.random() < sigmoid(logit))))
    return examples


def load_uci(path: Path) -> list[Example]:
    with path.open(newline="", encoding="utf-8-sig") as handle:
        reader = csv.DictReader(handle)
        fields = set(reader.fieldnames or ())
        missing = set(RAW_COLUMNS) - fields
        target_name = next((name for name in TARGET_ALIASES if name in fields), None)
        if missing or target_name is None:
            raise ValueError(f"Unexpected UCI schema; missing={sorted(missing)}, target={target_name}")
        examples = []
        for row in reader:
            raw = {name: float(row[name]) for name in RAW_COLUMNS}
            examples.append(Example(feature_engineer(raw), int(float(row[target_name]))))
    if len(examples) < 100:
        raise ValueError("At least 100 rows are required")
    return examples


def standardize(examples: list[Example]) -> tuple[list[Example], list[float], list[float]]:
    means = [sum(item.values[i] for item in examples) / len(examples) for i in range(len(FEATURES))]
    scales = []
    for i, mean in enumerate(means):
        variance = sum((item.values[i] - mean) ** 2 for item in examples) / len(examples)
        scales.append(max(math.sqrt(variance), 1e-8))
    transformed = [Example(tuple((v - means[i]) / scales[i] for i, v in enumerate(item.values)), item.target)
                   for item in examples]
    return transformed, means, scales


def fit_logistic(examples: list[Example], epochs: int = 900, rate: float = 0.05) -> tuple[float, list[float]]:
    intercept, weights = 0.0, [0.0] * len(FEATURES)
    for _ in range(epochs):
        intercept_grad, gradients = 0.0, [0.0] * len(weights)
        for item in examples:
            probability = sigmoid(intercept + sum(w * x for w, x in zip(weights, item.values)))
            error = probability - item.target
            intercept_grad += error
            for i, value in enumerate(item.values):
                gradients[i] += error * value
        size = len(examples)
        intercept -= rate * intercept_grad / size
        for i in range(len(weights)):
            weights[i] -= rate * (gradients[i] / size + 0.002 * weights[i])
    return intercept, weights


def metrics(examples: list[Example], intercept: float, weights: list[float]) -> dict[str, float]:
    scored = [(sigmoid(intercept + sum(w * x for w, x in zip(weights, item.values))), item.target)
              for item in examples]
    brier = sum((probability - target) ** 2 for probability, target in scored) / len(scored)
    accuracy = sum((probability >= 0.5) == bool(target) for probability, target in scored) / len(scored)
    positives = [p for p, y in scored if y == 1]
    negatives = [p for p, y in scored if y == 0]
    wins = sum((p > n) + 0.5 * (p == n) for p in positives for n in negatives)
    auc = wins / max(1, len(positives) * len(negatives))
    ece = 0.0
    for low in (i / 10 for i in range(10)):
        bucket = [(p, y) for p, y in scored if low <= p < low + 0.1 or (low == 0.9 and p == 1)]
        if bucket:
            ece += len(bucket) / len(scored) * abs(
                sum(p for p, _ in bucket) / len(bucket) - sum(y for _, y in bucket) / len(bucket))
    return {"rocAuc": auc, "brier": brier, "accuracyAt0.5": accuracy, "ece10": ece}


def export_artifact(path: Path, intercept: float, weights: list[float], means: list[float],
                    scales: list[float], source: str) -> None:
    payload = {
        "modelId": "risklens-credit-default",
        "version": "1.0.0-local",
        "trainedAt": datetime.now(timezone.utc).isoformat(),
        "trainingData": source,
        "intendedUse": "Education, model-serving demonstrations, and human-reviewed decision support only",
        "intercept": intercept,
        "calibration": {"slope": 1.0, "intercept": 0.0},
        "features": [
            {"name": name, "mean": means[i], "scale": scales[i], "coefficient": weights[i],
             "reasonCode": REASONS[name]}
            for i, name in enumerate(FEATURES)
        ],
        "referenceNormalizedMeans": {
            name: 0.0 for name in ("repaymentStatusCurrent", "utilizationCurrent", "paymentRatioCurrent", "billTrend")
        },
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    print(f"artifact={path} sha256={digest}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--uci-csv", type=Path, help="Locally obtained UCI Default of Credit Card Clients CSV")
    parser.add_argument("--output", type=Path, default=Path("build/model.json"))
    parser.add_argument("--seed", type=int, default=1729)
    parser.add_argument("--synthetic-rows", type=int, default=3000)
    args = parser.parse_args()

    examples = load_uci(args.uci_csv) if args.uci_csv else synthetic_examples(args.synthetic_rows, args.seed)
    source = "UCI Default of Credit Card Clients (user-supplied)" if args.uci_csv else "deterministic synthetic fixture"
    random.Random(args.seed).shuffle(examples)
    split = round(len(examples) * 0.8)
    train_raw, test_raw = examples[:split], examples[split:]
    train, means, scales = standardize(train_raw)
    test = [Example(tuple((v - means[i]) / scales[i] for i, v in enumerate(item.values)), item.target)
            for item in test_raw]
    intercept, weights = fit_logistic(train)
    print(json.dumps({"rows": len(examples), "test": metrics(test, intercept, weights)}, indent=2))
    export_artifact(args.output, intercept, weights, means, scales, source)


if __name__ == "__main__":
    main()
