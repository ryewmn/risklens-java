# Model card: RiskLens synthetic logistic model

## Summary

| Field | Value |
|---|---|
| Model | `risklens-credit-default` |
| Version | `1.0.0-synthetic` |
| Type | Standardized logistic regression with affine calibration |
| Artifact | `src/main/resources/models/logistic-v1.json` |
| Data | Seeded synthetic fixture shaped like selected UCI Default of Credit Card Clients columns |
| Output | Probability in `[0,1]`, risk band, up to three contribution-based reason codes |
| Intended use | Education, portfolio demonstration, model-serving tests |
| Prohibited use | Credit approval, denial, pricing, eligibility, adverse action, or automated consequential decisions |

## Inputs

The service accepts credit limit, age, two repayment-status values, two bill amounts, and two payment amounts. Derived features include utilization, payment ratios, and bill trend. UCI demographic fields for sex, education, and marital status are excluded. Age remains because it is present in the source schema; a real legal/fairness review may require excluding or specially governing it.

## Training and evaluation

The bundled coefficients are a fixed demonstration artifact. `training/train.py` provides a reproducible dependency-free pipeline that defaults to 3,000 synthetic rows, seed 1729, and an 80/20 holdout. A sample retraining run produced ROC AUC 0.6836, Brier score 0.1687, accuracy 0.7817 at threshold 0.5, and 10-bin expected calibration error 0.0556. Those results describe one synthetic run only.

## Calibration and bands

The engine applies `sigmoid(calibration.intercept + calibration.slope × rawLogit)`. The bundled artifact uses a documented demonstration affine calibration. Bands are operational labels:

| Probability | Band |
|---:|---|
| `< 0.20` | LOW |
| `0.20–<0.50` | MODERATE |
| `0.50–<0.75` | HIGH |
| `≥ 0.75` | CRITICAL |

These thresholds have no validated economic, legal, or risk-policy meaning.

## Explainability

Reason codes rank positive standardized feature contributions. They describe what increased this model's score relative to its reference mean. They are not causal findings, complete explanations, or regulatory adverse-action notices.

## Fairness and limitations

- Synthetic data cannot establish accuracy, calibration, robustness, or fairness on people.
- Removing protected attributes does not prevent proxy discrimination.
- The sample does not evaluate intersectional groups, distribution shift, reject inference, selection bias, or feedback loops.
- The model has no uncertainty interval and can be overconfident out of distribution.
- Negative bill balances and status codes have dataset-specific semantics that require data contracts in real systems.

## Production validation checklist

- Verify lawful purpose, data rights, feature definitions, retention, and consent.
- Evaluate calibration, error costs, stability, and subgroup metrics on representative governed data.
- Establish human review, appeals, reason-code validation, and adverse-action processes.
- Define out-of-distribution behavior and a safe abstention path.
- Sign the artifact, pin its digest, canary it, and monitor performance and drift.
- Revalidate after any data, code, dependency, threshold, or policy change.
