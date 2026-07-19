# Keycloak Configuration

This directory contains the Keycloak realm export for the Outreach Platform.

## Files

- `outreach-realm.json` — Realm export defining the `outreach` realm with clients, roles, users, and security policies.

## Quick Start with Docker

Start Keycloak with the realm auto-imported:

```bash
docker run -d \
  --name keycloak \
  -p 8080:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=kcadmin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=kcadmin \
  -v $(pwd)/config/keycloak/outreach-realm.json:/opt/keycloak/data/import/outreach-realm.json \
  quay.io/keycloak/keycloak:26.0 \
  start-dev --import-realm
```

On Windows (PowerShell):

```powershell
docker run -d `
  --name keycloak `
  -p 8080:8080 `
  -e KC_BOOTSTRAP_ADMIN_USERNAME=kcadmin `
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=kcadmin `
  -v ${PWD}/config/keycloak/outreach-realm.json:/opt/keycloak/data/import/outreach-realm.json `
  quay.io/keycloak/keycloak:26.0 `
  start-dev --import-realm
```

Once running, access the admin console at `http://localhost:8080` with credentials `kcadmin/kcadmin`.

## Realm Details

| Setting | Value |
|---------|-------|
| Realm | `outreach` |
| SSL Required | `external` (HTTPS required for non-localhost) |
| Access Token Lifespan | 5 minutes |
| Refresh Token Lifespan | 30 minutes |
| SSO Session Idle | 30 minutes |
| SSO Session Max | 10 hours |

### Clients

| Client ID | Type | Flow | Purpose |
|-----------|------|------|---------|
| `outreach-dashboard` | Public | Authorization Code + PKCE | Angular frontend |
| `outreach-services` | Confidential | Client Credentials | Service-to-service |

### Roles

| Role | Description |
|------|-------------|
| `ROLE_ADMIN` | Full system access |
| `ROLE_PMO` | Project management office access |
| `ROLE_POC` | Point of contact — limited event access |

### Security Policies

- **Password policy:** Minimum 12 characters, 1 uppercase, 1 lowercase, 1 digit, 1 special character
- **Brute force protection:** Account locked after 5 failed attempts for 30 minutes

## Changing the Default Admin Password

The realm ships with a default user (`admin` / `Admin@12345!`) that is forced to change their password on first login. To reset it via CLI:

```bash
docker exec keycloak /opt/keycloak/bin/kcadm.sh set-password \
  --server http://localhost:8080 \
  --realm outreach \
  --user admin \
  --new-password <NEW_PASSWORD> \
  --config /tmp/kcadm.config
```

You must authenticate with the Keycloak admin (`kcadmin`) first:

```bash
docker exec keycloak /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user kcadmin \
  --password kcadmin \
  --config /tmp/kcadm.config
```

## Updating Client Secrets for Production

The `outreach-services` client uses a placeholder secret (`CHANGE_ME_IN_PRODUCTION`). For production:

1. Log into the Keycloak Admin Console
2. Navigate to **Clients → outreach-services → Credentials**
3. Click **Regenerate Secret** and copy the new value
4. Update your deployment configuration (environment variable or secrets store) with the new secret:
   ```
   OUTREACH_SERVICES_CLIENT_SECRET=<generated-secret>
   ```

Alternatively, use the Keycloak Admin CLI:

```bash
docker exec keycloak /opt/keycloak/bin/kcadm.sh get clients \
  --server http://localhost:8080 \
  --realm outreach \
  --fields id,clientId \
  --config /tmp/kcadm.config \
  -q clientId=outreach-services
```

Then regenerate the secret using the returned client UUID:

```bash
docker exec keycloak /opt/keycloak/bin/kcadm.sh create \
  clients/<CLIENT_UUID>/client-secret \
  --server http://localhost:8080 \
  --realm outreach \
  --config /tmp/kcadm.config
```

## Importing into an Existing Keycloak Instance

If Keycloak is already running, import the realm via the Admin Console:

1. Log into `http://localhost:8080` as the Keycloak admin
2. Click **Create Realm** in the top-left dropdown
3. Click **Browse** and select `outreach-realm.json`
4. Click **Create**

Or via CLI:

```bash
docker exec keycloak /opt/keycloak/bin/kcadm.sh create realms \
  --server http://localhost:8080 \
  --realm master \
  --config /tmp/kcadm.config \
  -f /opt/keycloak/data/import/outreach-realm.json
```
