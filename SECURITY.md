# Security Policy

## Reporting a Vulnerability

Please report security and privacy vulnerabilities privately using the repository's **Security** tab (**Report a vulnerability** / GitHub Private Vulnerability Reporting). If private advisories are not enabled on a fork, open an issue requesting a private contact channel without disclosing exploit or vulnerability details.

Include:
- Affected BlockReels version or commit SHA
- Android version and device model (if relevant)
- Impact and reproduction steps using synthetic or redacted data

Do not include signing keys, personal messages, or raw Accessibility tree dumps containing private user data in reports.

## Scope

Security reports cover:
- Unexpected network permissions or data leakage
- PIN lock bypasses or weak credential storage
- Privilege escalation or unsafe IPC / Intent handling

For ordinary detection regressions (such as a feed layout change after a third-party app update), open a regular bug report via [CONTRIBUTING.md](CONTRIBUTING.md).
