# Security policy

## Supported versions

The latest commit on the default branch is supported. This is a portfolio project and does not provide a production security SLA.

## Reporting

Do not open a public issue for a suspected vulnerability or include applicant data, credentials, tokens, model files from restricted sources, or exploit details in public discussions. Use GitHub's private vulnerability reporting feature for this repository. Include affected version, impact, minimal reproduction, and suggested mitigation when available.

## Deployment responsibility

The sample intentionally omits identity and edge controls. A real deployment must add authenticated and authorized access, request-size limits, rate limits, TLS, network isolation for actuator routes, secret management, signed artifacts, dependency and image scanning, resource limits, retention policy, and governed monitoring. See [`docs/THREAT_MODEL.md`](docs/THREAT_MODEL.md).
