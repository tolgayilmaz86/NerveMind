# Security Policy

## Reporting a Vulnerability

We take the security of NerveMind seriously. If you have discovered a security vulnerability in NerveMind, please do not disclose it publicly until we have had a chance to fix it.

### How to Report

Please report security vulnerabilities by emailing **security@nervemind.io**.

We will try to review your report within 48 hours and will prioritize a fix for any confirmed vulnerabilities.

### Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.x.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Credentials and Secrets

NerveMind is a local-first application. 
- **API Keys are stored locally**: All API keys (OpenAI, etc.) are stored in your local H2 database, encrypted using AES-256.
- **No Cloud Sync**: Your credentials are never sent to our servers.
- **Git Safety**: Never commit your `.env` file or `credentials.json` export to a public repository. The `.gitignore` is configured to help prevent this, but please be vigilant.

## Local-First Security Defaults

NerveMind is designed primarily as a local desktop application.

- **H2 console access**: The `/h2-console/**` endpoint is intentionally configured as `permitAll()` in the default security configuration so local developers can inspect and troubleshoot data.
- **Intended environment**: This default is acceptable for local-only usage (loopback/private machine access).

## Production Hardening Guidance

If you deploy NerveMind in a shared or network-accessible environment, harden the security configuration before exposure:

- Remove `permitAll()` access for `/h2-console/**`, or disable H2 console entirely.
- Require authentication for all non-static routes.
- Keep CSRF enabled for application endpoints.
- Restrict network exposure (firewall/VPN/reverse proxy) and use TLS.

The current default favors local-first developer experience over production profile separation. If production deployment is needed, create a dedicated security profile (for example, a `production` Spring profile) with stricter rules.
