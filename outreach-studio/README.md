# Outreach Studio

A modern React 19 single-page application (SPA) for the Outreach Feedback Management System. Built with TypeScript, Vite, TanStack Router, TanStack Query, and Zustand.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Available Scripts](#available-scripts)
- [Project Structure](#project-structure)
- [Environment Configuration](#environment-configuration)
- [Development Workflow](#development-workflow)
- [Testing](#testing)
- [Production Build](#production-build)
- [Docker Deployment](#docker-deployment)
- [AWS CloudFront Deployment](#aws-cloudfront-deployment)
- [Tech Stack](#tech-stack)

---

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Node.js | 20+ (LTS) | JavaScript runtime |
| npm | 10+ | Package manager (ships with Node.js) |
| Docker | 24+ | Container builds (optional, for deployment) |

---

## Quick Start

```bash
# 1. Install dependencies
npm ci

# 2. Start development server
npm run dev

# 3. Open in browser
# → http://localhost:5173
```

The dev server proxies `/api` requests to the API Gateway at `http://localhost:7093` and `/oauth2` requests to the Auth Server at `http://localhost:8090`.

---

## Available Scripts

| Command | Description |
|---------|-------------|
| `npm run dev` | Start Vite dev server with HMR (port 5173) |
| `npm run build` | Type-check → bundle → validate bundle size |
| `npm run build:no-check` | Type-check → bundle (skip size validation) |
| `npm run lint` | Run ESLint across all source files |
| `npm run test` | Run Vitest in watch mode |
| `npm run test -- --run` | Run all tests once (CI mode) |
| `npm run test:coverage` | Run tests with V8 coverage report |
| `npm run check-bundle` | Validate bundle size against thresholds |
| `npm run preview` | Serve production build locally (port 4173) |

---

## Project Structure

```
outreach-studio/
├── public/                  # Static assets (copied as-is to dist/)
│   └── health              # Health check file for container probes
├── scripts/
│   └── check-bundle-size.mjs  # Post-build bundle size validator
├── src/
│   ├── components/         # Reusable UI components
│   │   ├── data-table/     # Generic DataTable (pagination, sorting, filtering, virtualization)
│   │   ├── feedback/       # ErrorBoundary, Toasts, Connectivity banners
│   │   └── layout/         # LayoutShell, Sidebar, Header
│   ├── hooks/              # Custom React hooks
│   ├── lib/                # Non-React utilities (auth, HTTP client, query keys, offline queue)
│   ├── routes/             # File-based routes (TanStack Router)
│   │   ├── __root.tsx      # Root layout (providers, error boundary, devtools)
│   │   ├── _authenticated.tsx  # Auth guard layout
│   │   └── _authenticated/ # Protected route pages
│   ├── stores/             # Zustand global state
│   ├── styles/             # CSS custom properties (design tokens)
│   ├── test/               # Test utilities, MSW handlers, factories
│   └── types/              # TypeScript type definitions
├── Dockerfile              # Multi-stage build (Node → Nginx)
├── nginx.conf              # Production Nginx configuration
├── vite.config.ts          # Build tool configuration
├── tsconfig.app.json       # TypeScript config (browser code)
└── package.json            # Dependencies and scripts
```

---

## Environment Configuration

Environment variables are managed via `.env` files. Only variables prefixed with `VITE_` are exposed to browser code.

| Variable | Description | Default |
|----------|-------------|---------|
| `VITE_API_URL` | Base path for API calls | `/api` |
| `VITE_KEYCLOAK_URL` | OAuth2 Authorization Server URL | `http://localhost:8090` |
| `VITE_KEYCLOAK_REALM` | Keycloak realm (empty = Spring Auth Server mode) | _(empty)_ |
| `VITE_KEYCLOAK_CLIENT_ID` | OAuth2 client ID | `outreach-dashboard` |
| `VITE_AUTH_BYPASS` | Skip real auth, use mock admin user | `false` |

### File Hierarchy

| File | Loaded When |
|------|------------|
| `.env` | Always |
| `.env.development` | `npm run dev` |
| `.env.production` | `npm run build` |
| `.env.local` | Always (gitignored, for personal overrides) |

**Security:** Never put secrets in `VITE_` variables. Everything in the browser bundle is visible to end users.

---

## Development Workflow

### Running with Backend Services

1. Start all backend services via Docker Compose:
   ```bash
   cd ../outreach-platform-parent
   docker compose up -d
   ```

2. Start the frontend dev server:
   ```bash
   npm run dev
   ```

3. Access the app at `http://localhost:5173`. The Vite proxy handles API routing:
   - `/api/*` → API Gateway (port 7093)
   - `/oauth2/*` → Auth Server (port 8090)

### Auth Bypass Mode

For UI development without running the auth server:

```bash
# In .env.development (or .env.local)
VITE_AUTH_BYPASS=true
```

This injects a mock admin user with `ROLE_ADMIN` — all routes and features are accessible.

### Theme Development

The theming system uses CSS custom properties defined in `src/styles/theme.css`. To add or modify tokens:

1. Define in `:root` (light) and `[data-theme="dark"]` sections
2. Reference in component CSS modules via `var(--token-name)`
3. Never hardcode color values in component styles

---

## Testing

### Test Stack

- **Vitest** — Test runner (Vite-native, Jest-compatible API)
- **React Testing Library** — Component testing via user behavior simulation
- **MSW (Mock Service Worker)** — Network-level API mocking
- **jest-axe** — Automated accessibility testing

### Running Tests

```bash
# Watch mode (interactive, re-runs on file changes)
npm run test

# Single run (CI mode)
npm run test -- --run

# With coverage report
npm run test:coverage

# Specific file or directory
npm run test -- --run src/lib/__tests__/
```

### Coverage Thresholds

| Metric | Threshold |
|--------|-----------|
| Lines | 70% |
| Branches | 60% |

### Test File Convention

Test files live in `__tests__/` directories adjacent to their source:
```
src/components/data-table/
├── DataTable.tsx
├── DataTable.module.css
└── __tests__/
    └── DataTable.test.tsx
```

---

## Production Build

```bash
# Full production build with all validations
npm run build
```

This executes three stages:
1. **`tsc -b`** — TypeScript type checking (fails on type errors)
2. **`vite build`** — Rollup bundling with tree-shaking and code splitting
3. **`check-bundle-size.mjs`** — Validates bundle size thresholds

### Bundle Size Limits

| Metric | Limit | Current |
|--------|-------|---------|
| Total JS (gzipped) | < 1 MB | ~320 KB |
| Initial bundle (gzipped) | < 200 KB | ~128 KB |

### Output

Production files are written to `dist/` with content-hashed filenames:
```
dist/
├── index.html
├── assets/
│   ├── index-[hash].js       # Entry chunk
│   ├── vendor-react-[hash].js
│   ├── vendor-tanstack-[hash].js
│   ├── vendor-charts-[hash].js
│   └── [route]-[hash].js     # Lazy-loaded route chunks
└── health                     # Health check endpoint
```

### Vendor Splitting Strategy

| Chunk | Contains |
|-------|----------|
| `vendor-react` | React, ReactDOM |
| `vendor-tanstack` | Router, Query, Table, Form |
| `vendor-charts` | Recharts, D3 dependencies |
| Route chunks | Lazy-loaded per route (code splitting) |

---

## Docker Deployment

### Build the Image

```bash
docker build -t outreach-studio:latest .
```

### Run the Container

```bash
docker run -d \
  --name outreach-studio \
  -p 80:80 \
  outreach-studio:latest
```

### Architecture

The Dockerfile uses a multi-stage build:

| Stage | Base Image | Purpose |
|-------|-----------|---------|
| Builder | `node:20-alpine` | Install deps, run build |
| Runtime | `nginx:stable-alpine` | Serve static files (94 MB image) |

### Nginx Configuration

The included `nginx.conf` provides:
- **SPA routing** — All non-file requests serve `index.html` (client-side routing)
- **Gzip compression** — CSS, JS, JSON (min 256 bytes)
- **Cache headers** — Hashed assets: `Cache-Control: public, immutable` (1 year); `index.html`: `no-cache`
- **Health endpoint** — `GET /health` returns 200 (for container probes)
- **Non-root execution** — Nginx runs as `appuser` (security best practice)

### Health Check

```bash
curl http://localhost:80/health
# → OK
```

The HEALTHCHECK directive polls every 30s with 5s timeout and 3 retries.

---

## AWS CloudFront Deployment

This section covers deploying Outreach Studio to AWS using **S3 + CloudFront** — the recommended approach for production SPAs.

### Architecture

```
┌──────────┐     ┌──────────────┐     ┌─────────────┐
│  Browser  │────▶│  CloudFront  │────▶│  S3 Bucket  │
│           │     │  (CDN/Edge)  │     │  (Origin)   │
└──────────┘     └──────┬───────┘     └─────────────┘
                        │
                        │ /api/*
                        ▼
                 ┌──────────────┐
                 │  API Gateway  │
                 │  (ALB/Origin) │
                 └──────────────┘
```

### Prerequisites

- AWS CLI configured with appropriate IAM permissions
- An S3 bucket for hosting static files
- A CloudFront distribution
- (Optional) Route 53 hosted zone for custom domain
- (Optional) ACM certificate for HTTPS on custom domain

### Step 1: Create Production Environment File

Create `.env.production` with production values:

```bash
VITE_API_URL=/api
VITE_KEYCLOAK_URL=https://auth.yourdomain.com
VITE_KEYCLOAK_REALM=
VITE_KEYCLOAK_CLIENT_ID=outreach-dashboard
VITE_AUTH_BYPASS=false
```

> **Note:** `VITE_API_URL=/api` works because CloudFront will route `/api/*` to your backend origin.

### Step 2: Build for Production

```bash
npm run build
```

Output is in `dist/`. No code changes are needed — the same build works for Docker and CloudFront.

### Step 3: S3 Bucket Configuration

Create and configure the S3 bucket:

```bash
# Create bucket (if not exists)
aws s3 mb s3://outreach-studio-prod --region ap-south-1

# Sync build output to S3
aws s3 sync dist/ s3://outreach-studio-prod \
  --delete \
  --cache-control "public, max-age=31536000, immutable" \
  --exclude "index.html" \
  --exclude "health"

# Upload index.html with no-cache (so users always get latest version)
aws s3 cp dist/index.html s3://outreach-studio-prod/index.html \
  --cache-control "no-cache, no-store, must-revalidate"

# Upload health check file
aws s3 cp dist/health s3://outreach-studio-prod/health \
  --cache-control "no-cache" \
  --content-type "text/plain"
```

**Bucket Policy** (allow CloudFront access via OAC):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowCloudFrontOAC",
      "Effect": "Allow",
      "Principal": {
        "Service": "cloudfront.amazonaws.com"
      },
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::outreach-studio-prod/*",
      "Condition": {
        "StringEquals": {
          "AWS:SourceArn": "arn:aws:cloudfront::ACCOUNT_ID:distribution/DISTRIBUTION_ID"
        }
      }
    }
  ]
}
```

> **Important:** Do NOT enable S3 static website hosting. Use Origin Access Control (OAC) with CloudFront instead — this keeps the bucket private.

### Step 4: CloudFront Distribution Configuration

#### Origins

| Origin | Domain | Path Pattern | Purpose |
|--------|--------|-------------|---------|
| S3 (default) | `outreach-studio-prod.s3.ap-south-1.amazonaws.com` | `*` (default) | Static files |
| API Backend | `api.yourdomain.com` (ALB/API Gateway) | `/api/*` | Backend API proxy |
| Auth Server | `auth.yourdomain.com` | `/oauth2/*` | OAuth2 token endpoint |

#### Behaviors

| Path Pattern | Origin | Cache Policy | Notes |
|-------------|--------|--------------|-------|
| `/api/*` | API Backend | `CachingDisabled` | Forward all headers, cookies, query strings |
| `/oauth2/*` | Auth Server | `CachingDisabled` | Forward all headers, cookies |
| `/assets/*` | S3 | `CachingOptimized` (1 year) | Immutable hashed files |
| `Default (*)` | S3 | `CachingDisabled` | Always serve fresh `index.html` |

#### SPA Routing: Custom Error Responses

CloudFront must return `index.html` for all paths that don't match a real file (SPA client-side routing):

| HTTP Error Code | Response Page Path | Response Code | TTL |
|----------------|-------------------|---------------|-----|
| 403 (S3 returns for missing keys) | `/index.html` | 200 | 0 |
| 404 | `/index.html` | 200 | 0 |

This ensures routes like `/events/123` or `/admin` load the SPA, which then handles routing client-side.

#### Security Headers (Response Headers Policy)

Create a custom response headers policy:

```json
{
  "SecurityHeadersConfig": {
    "StrictTransportSecurity": {
      "Override": true,
      "AccessControlMaxAgeSec": 31536000,
      "IncludeSubdomains": true,
      "Preload": true
    },
    "ContentTypeOptions": {
      "Override": true
    },
    "FrameOptions": {
      "Override": true,
      "FrameOption": "DENY"
    },
    "XSSProtection": {
      "Override": true,
      "ModeBlock": true,
      "Protection": true
    },
    "ReferrerPolicy": {
      "Override": true,
      "ReferrerPolicy": "strict-origin-when-cross-origin"
    },
    "ContentSecurityPolicy": {
      "Override": true,
      "ContentSecurityPolicy": "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self' https://auth.yourdomain.com; frame-ancestors 'none';"
    }
  }
}
```

### Step 5: CloudFront Function for SPA Routing (Alternative)

If custom error responses don't meet your needs, use a CloudFront Function on the **viewer request** event:

```javascript
function handler(event) {
  var request = event.request;
  var uri = request.uri;

  // If the URI has a file extension, serve as-is
  if (uri.includes('.')) {
    return request;
  }

  // For all other paths (SPA routes), rewrite to index.html
  if (!uri.startsWith('/api/') && !uri.startsWith('/oauth2/')) {
    request.uri = '/index.html';
  }

  return request;
}
```

This is more precise than error response overrides — it handles the rewrite at the edge without hitting S3 at all.

### Step 6: Deployment Script

Create `scripts/deploy-aws.sh`:

```bash
#!/bin/bash
set -euo pipefail

BUCKET="outreach-studio-prod"
DISTRIBUTION_ID="E1234567890ABC"
REGION="ap-south-1"

echo "▸ Building production bundle..."
npm run build

echo "▸ Syncing assets to S3 (immutable cache)..."
aws s3 sync dist/assets/ s3://$BUCKET/assets/ \
  --region $REGION \
  --delete \
  --cache-control "public, max-age=31536000, immutable" \
  --content-encoding identity

echo "▸ Uploading index.html (no-cache)..."
aws s3 cp dist/index.html s3://$BUCKET/index.html \
  --region $REGION \
  --cache-control "no-cache, no-store, must-revalidate" \
  --content-type "text/html"

echo "▸ Uploading health check..."
aws s3 cp dist/health s3://$BUCKET/health \
  --region $REGION \
  --cache-control "no-cache" \
  --content-type "text/plain"

echo "▸ Invalidating CloudFront cache for index.html..."
aws cloudfront create-invalidation \
  --distribution-id $DISTRIBUTION_ID \
  --paths "/index.html" "/health"

echo "✓ Deployment complete."
```

### Step 7: Code/Build Changes for CloudFront

**No code changes are required.** The same `npm run build` output works for both Docker/Nginx and S3/CloudFront. However, consider these optimizations:

#### 1. Add `base` to Vite config (if hosting at a subpath)

If your CloudFront distribution serves the app at a subpath (e.g., `https://cdn.example.com/studio/`):

```typescript
// vite.config.ts
export default defineConfig({
  base: '/studio/',  // Add this if NOT at root
  // ... rest of config
})
```

> If serving at the root of the domain (recommended), no change is needed.

#### 2. Content-Security-Policy in meta tag (optional)

For tighter CSP without CloudFront response headers policy:

```html
<!-- index.html -->
<meta http-equiv="Content-Security-Policy" 
  content="default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; connect-src 'self' https://auth.yourdomain.com;">
```

#### 3. Service Worker for offline-first (future enhancement)

If you add a service worker (`vite-plugin-pwa`), update the S3 sync to set `no-cache` on `sw.js`:

```bash
aws s3 cp dist/sw.js s3://$BUCKET/sw.js \
  --cache-control "no-cache, no-store, must-revalidate"
```

### Cost Estimate (ap-south-1)

| Resource | Approximate Monthly Cost |
|----------|------------------------|
| S3 (< 1 GB storage, <100K requests) | ~$0.05 |
| CloudFront (100 GB transfer, 1M requests) | ~$12 |
| Route 53 hosted zone | $0.50 |
| ACM certificate | Free |
| **Total** | **~$13/month** |

### Monitoring

- **CloudFront Metrics** — Request count, error rate, cache hit ratio (CloudWatch)
- **S3 Access Logs** — Optional, for audit trail
- **CloudFront Access Logs** — Detailed request logs to S3
- **Alarms** — Set CloudWatch alarm on 5xx error rate > 1%

---

## Tech Stack

| Category | Technology | Version |
|----------|-----------|---------|
| UI Library | React | 19 |
| Language | TypeScript | 6.0 |
| Build Tool | Vite | 8.1 |
| Routing | TanStack Router | 1.170 |
| Server State | TanStack Query | 5.101 |
| Data Tables | TanStack Table | 8.21 |
| Virtualization | TanStack Virtual | 3.14 |
| Forms | TanStack Form | 1.33 |
| Client State | Zustand | 5.0 |
| HTTP Client | Axios | 1.18 |
| Charts | Recharts | 3.10 |
| Validation | Zod | 3.25 |
| Testing | Vitest + RTL + MSW | Latest |
| CSS | CSS Modules + Custom Properties | — |

---

## License

Internal project — not for public distribution.
