#!/usr/bin/env python3
"""Reference scorer used for Java/Python model-parity tests."""

from __future__ import annotations

import json
import math
import sys
from pathlib import Path


def score(artifact: dict, payload: dict) -> float:
    limit = payload["creditLimit"]
    bill_current, bill_prior = payload["billAmountCurrent"], payload["billAmountPrior"]
    raw = {
        "age": payload["age"],
        "logCreditLimit": math.log1p(limit),
        "repaymentStatusCurrent": payload["repaymentStatusCurrent"],
        "repaymentStatusPrior": payload["repaymentStatusPrior"],
        "utilizationCurrent": max(0, bill_current) / limit,
        "utilizationPrior": max(0, bill_prior) / limit,
        "paymentRatioCurrent": payload["paymentAmountCurrent"] / max(1, abs(bill_current)),
        "paymentRatioPrior": payload["paymentAmountPrior"] / max(1, abs(bill_prior)),
        "billTrend": (bill_current - bill_prior) / limit,
    }
    logit = artifact["intercept"]
    for feature in artifact["features"]:
        normalized = (raw[feature["name"]] - feature["mean"]) / feature["scale"]
        logit += feature["coefficient"] * normalized
    calibrated = artifact["calibration"]["intercept"] + artifact["calibration"]["slope"] * logit
    return 1 / (1 + math.exp(-calibrated))


if __name__ == "__main__":
    artifact = json.loads(Path(sys.argv[1]).read_text())
    payload = json.loads(Path(sys.argv[2]).read_text())
    print(f"{score(artifact, payload):.15f}")
