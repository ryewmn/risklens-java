# Contributing

Thanks for improving RiskLens. Open an issue before a large change so its model, API, privacy, and compatibility impact can be discussed.

## Local checks

```bash
./mvnw test
python3 -m unittest training/test_parity.py
python3 training/train.py --synthetic-rows 3000 --seed 1729 --output build/model.json
```

Keep changes small and include tests. API changes must update `docs/openapi.yaml`. Model changes must update parity cases, evaluation evidence, `docs/MODEL_CARD.md`, version, and artifact digest expectations. Never commit customer, applicant, production, licensed, or downloaded UCI data.

Use conventional commit subjects when practical, such as `feat: add signed artifact check` or `fix: reject non-finite values`. Pull requests should explain the problem, design, validation, security/privacy impact, and rollback plan.

By contributing, you agree that your contribution is licensed under MIT.
