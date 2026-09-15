# Security Policy

## Reporting a Vulnerability

**Please do not open a public GitHub issue for security vulnerabilities.**

Instead, report it privately using [GitHub Security Advisories](https://github.com/faizalzafri/outreach-platform/security/advisories/new) for this repository. This lets maintainers assess and fix the issue before it's publicly disclosed.

Include as much of the following as you can:

- A description of the vulnerability and its potential impact
- Steps to reproduce (which service, request/response, relevant config)
- Any proof-of-concept code or logs (with secrets/PII redacted)

## What to expect

- We'll acknowledge your report as soon as we're able to.
- We'll work with you to understand and validate the issue.
- Once a fix is ready, we'll coordinate on disclosure timing before making the
  advisory public.

## Scope

This is a multi-tenant SaaS platform (`outreach-platform-parent/` backend services,
`outreach-studio/` frontend). Reports most relevant here concern:

- Cross-tenant data isolation failures (see `CLAUDE.md`'s multi-tenancy section for
  the intended isolation model — a bypass of it is a high-priority report)
- Authentication/authorization bypass
- Injection (SQL, command, etc.) or other OWASP Top 10 classes
- Exposure of `@PiiField`-annotated data or other sensitive information

## Supported versions

This project does not yet maintain multiple release branches — security fixes are
applied to the latest code on `master`.
