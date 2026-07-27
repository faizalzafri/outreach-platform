# React Learning Guide — Frontend Implementation

This guide documents every React, TypeScript, and frontend concept encountered during the frontend implementation. It's written for someone coming from a backend (e.g., Java/Spring) background who wants to understand modern frontend development from first principles.

---

## Task 1: Project Scaffolding and Build Configuration

### Task Summary

We created the foundational project structure for a React 19 single-page application (SPA) using Vite as the build tool and TypeScript for type safety. This is analogous to creating a new Maven/Gradle project with Spring Boot — it sets up the build system, project layout, dependency management, and development workflow before any business logic exists.

---

### React Concepts Used

#### 1. JSX (JavaScript XML)

**What it is:** JSX is a syntax extension that lets you write HTML-like markup inside JavaScript/TypeScript files. It looks like HTML but gets compiled into JavaScript function calls.

**Analogy:** Think of JSX like a Thymeleaf or JSP template, but instead of being a separate file, it lives right inside your logic code. The build tool transforms it into regular function calls.

```tsx
// This JSX:
<h1>Hello World</h1>

// Gets compiled by Vite into:
React.createElement('h1', null, 'Hello World')
```

**Why it exists:** It makes UI code readable. Instead of writing nested function calls to describe a tree of elements, you write something that visually resembles the output HTML.

#### 2. Components (Functions that Return UI)

**What it is:** A React component is just a JavaScript function that returns JSX. It's the fundamental building block — like a Java class that implements a specific interface, but simpler.

**Analogy:** Think of a component like a reusable "widget factory." You call the function with some inputs (props), and it returns a description of what should appear on screen.

```tsx
// A component is just a function:
function App() {
  return <h1>Hello</h1>
}

// You use it like an HTML tag:
<App />
```

**Why it exists:** Components let you break a complex UI into small, reusable, independently testable pieces — exactly like breaking a large service into small classes with single responsibilities.

#### 3. State (`useState` Hook)

**What it is:** State is data that lives inside a component and can change over time. When state changes, React re-renders (re-calls) the component function to produce new UI.

**Analogy:** Think of state like an instance variable in a Java object, except that when you change it, the framework automatically updates the screen. It's similar to how a `@Stateful` EJB or a Swing model/view works — change the model, the view updates.

```tsx
const [count, setCount] = useState(0)
// count = current value (like a getter)
// setCount = function to update it (like a setter that triggers a repaint)
// 0 = initial value
```

**Why it exists:** UIs are inherently stateful (forms have values, buttons can be toggled, counters increment). React needs to know WHAT changed so it can efficiently update only the parts of the DOM that are affected.

#### 4. StrictMode

**What it is:** A development-only wrapper component that enables extra checks and warnings. It renders nothing visible — it's purely a development aid.

```tsx
<StrictMode>
  <App />
</StrictMode>
```

**Analogy:** Like running your Java app with `-ea` (enable assertions) or using a lint plugin that warns about potential issues. It double-invokes certain functions to detect side effects, helping you find bugs early.

**Why it exists:** Catches common mistakes like impure rendering, missing cleanup in effects, and deprecated API usage — before they become production bugs.

#### 5. The Virtual DOM and Rendering

**What it is:** React doesn't manipulate the browser's DOM directly. Instead, your component functions return a lightweight JavaScript description of what the UI should look like (the "virtual DOM"). React compares the new description with the previous one and applies only the minimal changes to the real DOM.

**Analogy:** Think of it like a database migration tool. Instead of DROP TABLE + CREATE TABLE every time, it diffs the current schema against the desired schema and generates an ALTER TABLE with only the changes needed.

**Why it exists:** Direct DOM manipulation is slow and error-prone. By working with a virtual representation, React can batch updates, skip unnecessary work, and ensure the UI always matches the application state.

---

### Code Patterns Explained

#### Pattern 1: The Entry Point (`main.tsx`)

```tsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
```

**What's happening step by step:**
1. `document.getElementById('root')` — finds the `<div id="root">` in `index.html`
2. `createRoot(...)` — tells React "this DOM element is where you'll render everything"
3. `.render(<StrictMode><App /></StrictMode>)` — "render this component tree into that container"

**Analogy:** This is like `SpringApplication.run(MyApp.class, args)` — it bootstraps the entire application. The `<div id="root">` is like the embedded Tomcat that hosts everything.

#### Pattern 2: Single-File Components

Unlike Angular (which has separate `.ts`, `.html`, `.css` files per component), React puts logic and markup together in one file. The component function IS the template.

**Why:** Colocation of concerns. The markup is tightly coupled to the logic that drives it. Keeping them in the same file makes changes easier and eliminates synchronization bugs between files.

#### Pattern 3: Module System (ES Modules / `import` / `export`)

```tsx
import { useState } from 'react'         // Named import from a package
import App from './App.tsx'               // Default import from a local file
export default App                        // Default export
```

**Analogy:** Like Java's `import` statements, but more flexible. Named imports `{ useState }` are like importing a specific class. Default imports are like importing the "main" thing from a module.

**Why it exists:** Enables tree-shaking (the build tool removes unused code) and explicit dependency tracking. If you only import `useState`, the bundler won't include all 50+ other React functions in your final bundle.

---

### Step-by-Step Walkthrough: How a Vite + React Project Works

Here's what happens from "you type `npm run dev`" to "you see the app in your browser":

1. **`npm run dev`** executes the `"dev": "vite"` script from `package.json`

2. **Vite starts a dev server** on `http://localhost:5173` (by default). Unlike webpack, Vite doesn't bundle everything upfront — it serves files on demand using native ES modules.

3. **Browser requests `index.html`** — Vite serves it. The HTML contains:
   ```html
   <div id="root"></div>
   <script type="module" src="/src/main.tsx"></script>
   ```

4. **Browser requests `/src/main.tsx`** — Vite transforms the TypeScript/JSX on the fly (using esbuild, which is extremely fast) and serves it as plain JavaScript.

5. **`main.tsx` executes:**
   - Finds the `<div id="root">` element
   - Creates a React root
   - Renders the `<App />` component

6. **React calls the `App()` function**, which returns a virtual DOM tree (JavaScript objects describing HTML elements)

7. **React renders the virtual DOM to real DOM** — the browser paints pixels on screen

8. **HMR (Hot Module Replacement):** When you save a file, Vite tells the browser "this module changed." React swaps just that component without a full page reload — preserving state (like your counter value).

**Build mode (`npm run build`)** is different:
1. **`tsc -b`** type-checks ALL files (catches type errors)
2. **`vite build`** bundles everything into optimized static files with content hashes
3. **`vite-plugin-checker`** provides type checking overlay during dev mode
4. Output goes to `dist/` — plain HTML/CSS/JS files you can serve from any static host (Nginx, S3, etc.)

---

### Understanding the Build System (Vite)

#### What is Vite?

**Analogy:** Vite is to frontend what Maven/Gradle is to Java — it's the build tool. But it also includes a dev server (like Spring Boot's embedded Tomcat).

| Concern | Java Equivalent | Vite Equivalent |
|---------|----------------|-----------------|
| Build tool | Maven/Gradle | Vite |
| Dev server | Embedded Tomcat | Vite dev server |
| Compilation | javac | esbuild + SWC |
| Packaging | JAR/WAR | Static bundle (dist/) |
| Live reload | Spring DevTools | HMR |
| Dependency management | pom.xml | package.json |

#### `vite.config.ts` Explained

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import checker from 'vite-plugin-checker'
import path from 'path'

export default defineConfig({
  plugins: [
    react(),                    // Transforms JSX + enables React Fast Refresh (HMR)
    checker({                   // Type-checks during dev mode (overlay in browser)
      typescript: {
        tsconfigPath: './tsconfig.app.json',
      },
    }),
  ],
  resolve: {
    alias: {                    // Path aliases (like Maven resource filtering)
      '@/components': path.resolve(__dirname, 'src/components'),
      '@/lib': path.resolve(__dirname, 'src/lib'),
      // ... more aliases
    },
  },
  server: {
    proxy: {                    // Dev proxy (like Nginx reverse proxy, but for dev)
      '/api': {
        target: 'http://localhost:7093',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',            // Output directory (like target/ in Maven)
    sourcemap: false,          // No source maps in production (security)
    rollupOptions: {
      output: {
        // Content-hashed filenames for cache busting
        entryFileNames: 'assets/[name]-[hash].js',
        chunkFileNames: 'assets/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash].[ext]',
      },
    },
  },
})
```

#### Why Path Aliases?

Without aliases, deep imports look like:
```tsx
import { Button } from '../../../components/ui/Button'
```

With aliases:
```tsx
import { Button } from '@/components/ui/Button'
```

**Analogy:** Like package imports in Java. You don't write `../../src/main/java/com/company/util/Helper.java` — you write `import com.company.util.Helper`. Path aliases give you the same clean import experience.

**Important:** Aliases must be configured in TWO places:
1. `tsconfig.app.json` → for TypeScript/IDE resolution (so your editor doesn't show red squiggles)
2. `vite.config.ts` → for the build tool (so bundling actually resolves the paths)

This is a common gotcha. If you only configure one, either the IDE or the build will be confused.

---

### Understanding TypeScript Configuration

#### `tsconfig.json` — The Project References File

```json
{
  "files": [],
  "references": [
    { "path": "./tsconfig.app.json" },
    { "path": "./tsconfig.node.json" }
  ]
}
```

**What this does:** Splits the project into two TypeScript "projects":
- `tsconfig.app.json` — rules for your React app code (browser target)
- `tsconfig.node.json` — rules for build config files like `vite.config.ts` (Node.js target)

**Why:** Browser code and Node.js code have different APIs available. Browser code has `document`, `window`. Node.js code has `fs`, `path`, `process`. By splitting configs, TypeScript knows which APIs are valid where.

**Analogy:** Like having separate Maven profiles for "test" and "production" — same project, different compilation rules.

#### Key `tsconfig.app.json` Settings

| Setting | What It Does | Why It Matters |
|---------|-------------|----------------|
| `"strict": true` | Enables ALL strict type checks | Catches null pointer equivalents, implicit any types, unsound assignments at compile time |
| `"noEmit": true` | TypeScript only checks types, doesn't produce JS | Vite handles the actual compilation (much faster via esbuild) |
| `"jsx": "react-jsx"` | Enables JSX transformation (auto-imports React) | You can write JSX without `import React from 'react'` at the top |
| `"moduleResolution": "bundler"` | Resolves modules the way modern bundlers do | Matches Vite's resolution algorithm exactly |
| `"noUnusedLocals": true` | Error on unused variables | Keeps code clean, prevents dead code accumulation |
| `"noUncheckedIndexedAccess": true` | Array/object access returns `T \| undefined` | Forces null checks on array elements — prevents runtime "undefined is not a function" errors |

**`strict: true` is crucial.** It's the TypeScript equivalent of `-Werror` in C/C++ or `@NonNull` annotations everywhere in Java. Without it, TypeScript provides almost no safety guarantees.

---

### Understanding Environment Variables

```bash
# .env (base — always loaded)
VITE_API_URL=/api
VITE_KEYCLOAK_REALM=outreach
VITE_KEYCLOAK_CLIENT_ID=outreach-studio
```

**Rules:**
1. Only variables prefixed with `VITE_` are accessible in browser code via `import.meta.env.VITE_*`
2. Variables WITHOUT the prefix are NOT exposed (prevents accidentally leaking secrets to the browser)
3. `.env` → base (always), `.env.development` → dev mode, `.env.production` → build mode

**Analogy:** Like Spring Boot's `application.yml` / `application-dev.yml` / `application-prod.yml` hierarchy with profile activation.

**Security consideration:** EVERYTHING in a browser bundle is visible to users. The `VITE_` prefix is a safety rail — it makes you consciously opt-in to exposing a variable. Never put secrets (API keys, database passwords) in `VITE_` variables.

---

### Understanding the Dev Server Proxy

```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:7093',
      changeOrigin: true,
    },
  },
}
```

**What this does:** When the browser makes a request to `/api/events`, Vite's dev server intercepts it and forwards it to `http://localhost:7093/api/events`.

**Why:** The SPA runs on `localhost:5173` but the API Gateway runs on `localhost:7093`. Without a proxy, the browser would block the request due to CORS (Cross-Origin Resource Sharing) — the browser refuses to let `localhost:5173` talk to `localhost:7093` by default.

**Analogy:** Like an Nginx `proxy_pass` directive, but built into the dev server for development convenience. In production, the real Nginx config handles this.

---

### Understanding Content-Hashed Filenames

```typescript
output: {
  entryFileNames: 'assets/[name]-[hash].js',   // e.g., assets/main-a1b2c3d4.js
}
```

**What this does:** Every time you change code, the filename changes (because the hash of the content changes).

**Why:** Browser caching. If you deploy a fix, users with the old cached `main.js` would never get the new version. But `main-a1b2c3d4.js` → `main-e5f6g7h8.js` is a completely new URL — the browser fetches it fresh.

**Analogy:** Like versioned JAR filenames (`myapp-1.2.3.jar`) except it's automatic and content-based rather than manual version bumping.

---

### Understanding `vite-plugin-checker`

```typescript
checker({
  typescript: {
    tsconfigPath: './tsconfig.app.json',
  },
})
```

**What this does:** Runs the TypeScript compiler in a separate worker thread during development. If you introduce a type error, it shows an overlay in the browser (and in the terminal) with the file, line number, and error.

**Without this plugin:** Vite itself doesn't type-check — it only strips types and bundles. Type errors would slip through during development and only be caught when you run `tsc` manually or during `npm run build`.

**The dual safety net:**
- `vite-plugin-checker` → catches type errors DURING development (immediate feedback)
- `tsc -b` in the build script → catches type errors BEFORE bundling (prevents broken deployments)

**Analogy:** Like having both a linter in your IDE (immediate red squiggles) AND a CI check (prevents merge if lint fails). Belt and suspenders.

---

### Key Takeaways

- **React is a library, not a framework.** It handles UI rendering. You choose everything else (routing, state management, HTTP client). This is different from Angular which is an opinionated full framework.

- **Components are just functions.** They take inputs (props) and return UI descriptions (JSX). No class hierarchies, no lifecycle XML. Simple functions.

- **State drives the UI.** Change state → React re-renders → DOM updates. You never manually manipulate the DOM (no `document.querySelector().innerHTML = ...`).

- **Vite is the build tool AND dev server.** It gives you instant startup (no bundling in dev), lightning-fast HMR, and optimized production builds.

- **TypeScript strict mode is non-negotiable.** It's the equivalent of having null-safety in Kotlin or using `@NonNull` everywhere in Java. Without it, you lose most of TypeScript's value.

- **Path aliases** keep imports clean and refactoring easy. Configure in BOTH `tsconfig` (IDE) and `vite.config` (build).

- **Environment variables** with `VITE_` prefix are the only safe way to inject configuration into browser code. Never put secrets here — everything is visible to end users.

- **The build produces static files.** Unlike a Spring Boot JAR that runs a server, `npm run build` produces HTML/CSS/JS files that any static file server (Nginx, S3, CDN) can serve. No runtime required.

---

### Glossary

| Term | Definition |
|------|-----------|
| **SPA** | Single-Page Application — one HTML page, all navigation handled by JavaScript without full page reloads |
| **JSX** | JavaScript XML — syntax extension letting you write HTML-like code inside JavaScript |
| **Component** | A function that returns JSX, representing a reusable piece of UI |
| **State** | Data that can change over time, causing React to re-render the component |
| **Hook** | A function starting with `use` that lets you "hook into" React features (like state) from function components |
| **HMR** | Hot Module Replacement — updating code in the browser without a full page reload |
| **Vite** | A fast build tool for modern web projects (pronounced "veet", French for "fast") |
| **Bundling** | Combining many source files into fewer optimized files for production |
| **Tree-shaking** | Removing unused code from the final bundle (only possible with ES modules) |
| **TypeScript** | A typed superset of JavaScript — compiles to plain JS. Like Kotlin to Java, but for the browser |
| **ESM** | ES Modules — the native JavaScript module system using `import`/`export` |
| **Virtual DOM** | A lightweight JS representation of the real DOM that React uses for efficient updates |
| **Content Hash** | A fingerprint of file contents used in filenames for cache busting |
| **Path Alias** | A shorthand import path (like `@/components`) that maps to a real directory |
| **Proxy** | Forwarding requests from one server to another to avoid CORS issues during development |
| **tsconfig** | TypeScript configuration file controlling type checking rules and module resolution |
| **Plugin** | An extension to Vite that adds functionality (React support, type checking, etc.) |
| **npm** | Node Package Manager — the package manager for JavaScript (like Maven Central for Java) |
| **package.json** | Project metadata and dependency declarations (like `pom.xml` in Maven) |
| **node_modules** | Directory containing installed dependencies (like `.m2/repository` but per-project) |

---


## Task 4.1: Implement the Auth Module with PKCE

### Task Summary

We implemented a complete OAuth2 Authorization Code + PKCE (Proof Key for Code Exchange) authentication module that handles login, token exchange, silent refresh, and logout against a Keycloak identity provider. This is analogous to implementing a Spring Security OAuth2 client — but running entirely in the browser, where you cannot store secrets securely.

**Key files:** `src/lib/auth.ts`, `src/types/auth.ts`

---

### React Concepts Used

#### 1. Factory Function Pattern (Module Pattern)

**What it is:** Instead of a class, we use a function (`createAuthModule()`) that returns an object with methods. The function's local variables act as private state — they're inaccessible from outside.

**Analogy:** Think of it like a Java class with private fields and public methods, but expressed as a closure. The local variables (`state`, `refreshTimer`, `listeners`) are the "private fields," and the returned object's methods are the "public API."

```typescript
export function createAuthModule(): AuthModule {
  // These are "private fields" — inaccessible from outside
  let state: AuthState = { ... };
  let refreshTimer: ... = null;
  const listeners: Set<...> = new Set();

  // These are "public methods"
  function login(): void { ... }
  function logout(): Promise<void> { ... }

  return { login, logout, handleCallback, ... };
}
```

**Why this over a class?** In JavaScript/TypeScript, closures provide true privacy (no `#private` or `_convention` needed). It also avoids `this` binding issues that plague class-based code in React.


#### 2. Listener/Subscriber Pattern (Observer Pattern)

**What it is:** Components can subscribe to auth state changes via `onStateChange(callback)`. When the internal state changes, all registered listeners are notified.

**Analogy:** Exactly like Spring's `ApplicationEventPublisher` / `@EventListener` pattern, or Java's `PropertyChangeListener`. Something changes internally → all subscribers are notified.

```typescript
const listeners: Set<(state: AuthState) => void> = new Set();

function onStateChange(listener: (state: AuthState) => void): () => void {
  listeners.add(listener);
  return () => { listeners.delete(listener); }; // Returns unsubscribe function
}

function setState(partial: Partial<AuthState>): void {
  state = { ...state, ...partial };
  listeners.forEach((listener) => listener(state)); // Notify all
}
```

**Why:** React components need to re-render when auth state changes. By exposing a subscribe mechanism, the `AuthProvider` (Task 4.2) can listen for changes and update React state accordingly.

#### 3. In-Memory Token Storage

**What it is:** Access and refresh tokens are stored ONLY in JavaScript variables — never in `localStorage` or cookies.

**Analogy:** Like keeping a secret key only in JVM heap memory (a `private` field) rather than writing it to a file or database.

**Why:** `localStorage` is vulnerable to XSS attacks — any injected script can read it. In-memory storage means tokens are lost on page refresh (requiring re-authentication), but they cannot be stolen by XSS.


---

### Code Patterns Explained

#### Pattern 1: PKCE Flow (Why It Exists)

**Problem:** Browser apps (SPAs) cannot keep secrets. Unlike a Spring Boot backend that has a `client_secret` in its config, browser JavaScript is fully inspectable. Anyone can view source and extract any hardcoded secret.

**Solution — PKCE:** Instead of a static secret, the app generates a ONE-TIME cryptographic proof for each login:

1. **Generate a random `code_verifier`** (128 random characters)
2. **Hash it** with SHA-256 to create a `code_challenge`
3. **Send the challenge** to the auth server during the login redirect
4. **Send the original verifier** when exchanging the authorization code for tokens

The auth server verifies that `SHA256(verifier) == challenge`. This proves the same app that initiated login is completing it — without ever storing a long-lived secret.

```typescript
export function generateCodeVerifier(): string {
  const array = new Uint8Array(64);
  crypto.getRandomValues(array);         // Cryptographically secure random bytes
  return base64UrlEncode(array).slice(0, 128);
}

export async function generateCodeChallenge(verifier: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const digest = await crypto.subtle.digest('SHA-256', data);  // Browser's native SHA-256
  return base64UrlEncode(new Uint8Array(digest));
}
```

**Analogy:** Like a sealed envelope. You put your answer (verifier) in the envelope, show the sealed envelope (challenge) to the examiner. Later, you open the envelope to prove you had the answer all along — but nobody could peek inside before you opened it.

#### Pattern 2: State Parameter (CSRF Protection)

```typescript
export function generateState(): string {
  return crypto.randomUUID();
}
```

**Why:** Without a state parameter, an attacker could trick your browser into completing THEIR login flow (CSRF attack). The random state acts as a one-time token — when the callback arrives, we verify it matches what we stored.

**Analogy:** Like a Spring Security CSRF token in a form — it ensures the response came from a request YOU initiated.


#### Pattern 3: JWT Decoding Without Verification

```typescript
export function decodeJwtPayload(token: string): JwtClaims {
  const parts = token.split('.');
  const payload = parts[1];
  const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
  return JSON.parse(decoded) as JwtClaims;
}
```

**Why no signature verification?** The token was just received from the Keycloak server over HTTPS. The BACKEND verifies signatures (using the public key). The frontend only decodes to extract user info (name, email, roles) for display purposes.

**Analogy:** Like reading the name on a sealed letter. You trust it because you just received it from the post office (Keycloak) — the recipient (backend API) will verify the seal when you present it.

#### Pattern 4: Auto-Refresh Scheduling

```typescript
function scheduleAutoRefresh(accessToken: string): void {
  const claims = decodeJwtPayload(accessToken);
  const expiresInSeconds = claims.exp - Math.floor(Date.now() / 1000);
  const refreshInMs = Math.max((expiresInSeconds - 60) * 1000, 0);

  refreshTimer = setTimeout(() => {
    void silentRefresh();
  }, refreshInMs);
}
```

**What this does:** When we receive a token that expires in (say) 5 minutes, we schedule a refresh at the 4-minute mark (60 seconds before expiry). This ensures the user never experiences an expired token.

**Analogy:** Like a scheduled task in Spring (`@Scheduled`) that renews a certificate before it expires — proactive renewal rather than reactive failure handling.

---

### Step-by-Step Walkthrough: The Login Flow

1. User clicks "Login" → `authModule.login()` is called
2. A random `code_verifier` and `state` are generated and stored in `sessionStorage`
3. The `code_challenge` (SHA-256 hash of verifier) is computed
4. Browser redirects to Keycloak's authorization endpoint with the challenge
5. User authenticates at Keycloak (username/password, SSO, etc.)
6. Keycloak redirects back to `/callback?code=ABC&state=XYZ`
7. App calls `authModule.handleCallback(code, state)`
8. State is verified against what was stored
9. Code + verifier are sent to Keycloak's token endpoint
10. Keycloak verifies `SHA256(verifier) == challenge` and returns tokens
11. Access token is decoded to extract user profile
12. Auto-refresh timer is scheduled
13. Listeners (AuthProvider) are notified → UI updates

---

### Key Takeaways

- **PKCE replaces client secrets** for browser apps. It's mandatory for any SPA doing OAuth2.
- **In-memory storage** is the most secure option for tokens in a browser. The trade-off is losing the session on page refresh.
- **Factory functions** provide true encapsulation without class complexity.
- **The Observer pattern** bridges the gap between non-React code (auth module) and React components (AuthProvider).
- **Auto-refresh** ensures seamless UX — users don't get randomly logged out mid-session.
- **State parameter** prevents CSRF attacks on the OAuth callback.

---

### Glossary

| Term | Definition |
|------|-----------|
| **PKCE** | Proof Key for Code Exchange — a security extension for OAuth2 that replaces client secrets for public clients |
| **Code Verifier** | A random string generated by the client, used to prove identity during token exchange |
| **Code Challenge** | SHA-256 hash of the code verifier, sent during the authorization request |
| **Access Token** | A short-lived JWT that grants access to API endpoints |
| **Refresh Token** | A longer-lived token used to obtain new access tokens without re-authentication |
| **JWT** | JSON Web Token — a compact, URL-safe way to represent claims between parties |
| **CSRF** | Cross-Site Request Forgery — an attack where a malicious site tricks your browser into making authenticated requests |
| **XSS** | Cross-Site Scripting — an attack where malicious scripts are injected into trusted websites |
| **Closure** | A function that "remembers" variables from its enclosing scope even after that scope has exited |
| **Silent Refresh** | Refreshing the access token in the background without user interaction |
| **Singleton** | A single shared instance of a module — `export const authModule = createAuthModule()` |

---


## Task 4.2: Create AuthProvider React Context

### Task Summary

We created a React Context provider that wraps the auth module (Task 4.1) and makes authentication state available to any component in the tree. This is the bridge between the non-React auth logic and the React component world. It's analogous to Spring's dependency injection — instead of every component importing and calling the auth module directly, they receive auth state and actions through the component hierarchy.

**Key file:** `src/hooks/useAuth.tsx`

---

### React Concepts Used

#### 1. React Context API (`createContext` + `useContext`)

**What it is:** Context is React's built-in dependency injection system. It lets you pass data down through the component tree WITHOUT threading props through every intermediate component.

**Analogy:** Like Spring's `ApplicationContext` — you register beans (values) at the top, and any component anywhere in the tree can inject (consume) them without explicit wiring through every layer.

```typescript
// 1. Create the context (like defining a bean type)
const AuthContext = createContext<AuthContextValue | undefined>(undefined);

// 2. Provide a value at the top of the tree (like @Bean registration)
<AuthContext.Provider value={contextValue}>
  {children}
</AuthContext.Provider>

// 3. Consume it anywhere below (like @Autowired injection)
const { user, login, logout } = useContext(AuthContext);
```

**Without Context:** You'd have to pass `user`, `login`, `logout` as props through EVERY component — even ones that don't use them — just to reach the deeply nested component that does. This is called "prop drilling" and it's painful.

#### 2. The Provider Pattern

**What it is:** A component whose sole job is to supply context values to its children. It holds state and logic; its children consume the results.

```typescript
export function AuthProvider({ children }: AuthProviderProps) {
  const [authState, setAuthState] = useState<AuthState>(...);
  // ... logic ...
  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
}
```

**Analogy:** Like a Spring `@Configuration` class — it doesn't DO anything visible, but it sets up the dependencies that other components need.


#### 3. `useEffect` for Side Effects

**What it is:** `useEffect` lets you perform operations that are "side effects" — things that don't directly produce UI, like subscribing to external data sources, setting up timers, or making API calls.

**Analogy:** Like `@PostConstruct` in Spring — code that runs AFTER the component is mounted (rendered to the DOM). The return function is like `@PreDestroy` — cleanup when the component unmounts.

```typescript
useEffect(() => {
  // Subscribe to auth state changes (runs AFTER first render)
  const unsubscribe = authModule.onStateChange((newState) => {
    setAuthState(newState);
  });

  // Initialize auth module
  void authModule.initialize();

  // Cleanup: unsubscribe when component unmounts
  return unsubscribe;
}, []); // Empty array = run once on mount, cleanup on unmount
```

**The dependency array `[]`:** This tells React "only run this effect once" (on mount). If you listed `[someValue]`, it would re-run whenever `someValue` changes. If you omit the array entirely, it runs after EVERY render (usually a bug).

#### 4. `useCallback` for Memoized Functions

**What it is:** `useCallback` returns a memoized (cached) version of a function that only changes if its dependencies change.

```typescript
const login = useCallback(() => {
  authModule.login();
}, []);

const logout = useCallback(async () => {
  await authModule.logout();
}, []);
```

**Why it exists:** In React, every time a component re-renders, ALL functions inside it are re-created. If you pass these functions as props to child components, the children think they received NEW props and re-render unnecessarily.

**Analogy:** Like caching a computed value in a `Map` — if the inputs haven't changed, return the same reference. Same function logic, same object reference → children skip re-rendering.

#### 5. Loading State Pattern

```typescript
if (authState.isLoading) {
  return (
    <AuthContext.Provider value={contextValue}>
      <div role="status" aria-label="Loading authentication">
        <span>Loading...</span>
      </div>
    </AuthContext.Provider>
  );
}
```

**Why:** Authentication is asynchronous — we don't know if the user is logged in until the auth module initializes. During that time, we show a loading indicator instead of briefly flashing the login page and then switching to the app.

**Analogy:** Like a Spring Boot app's health check endpoint — the app isn't "ready" until all components have initialized. You don't route traffic to it until health returns 200.

---

### Code Patterns Explained

#### Pattern 1: Custom Hook with Guard Clause

```typescript
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
```

**Why the guard clause?** If someone uses `useAuth()` outside of an `<AuthProvider>`, the context is `undefined`. Instead of getting a cryptic "cannot read property of undefined" error somewhere else, we throw a clear message immediately.

**Analogy:** Like a Spring bean that requires an `@Autowired` dependency — if the dependency isn't available in the context, Spring throws `NoSuchBeanDefinitionException` with a clear message rather than a null pointer later.

#### Pattern 2: Bridging Non-React Code to React

The auth module (Task 4.1) is plain TypeScript — it doesn't know about React. The AuthProvider bridges the gap:

1. **Subscribe** to auth module changes via `onStateChange`
2. **Mirror** those changes into React state via `useState`
3. **React re-renders** when the state changes
4. **Children** get the updated values through context

This separation keeps the auth logic testable without React and the React layer thin.

---

### Step-by-Step Walkthrough

1. App renders `<AuthProvider>` at the top of the component tree
2. `AuthProvider` initializes with the current auth state (`isLoading: true`)
3. `useEffect` fires after first render — subscribes to auth state changes and calls `initialize()`
4. Auth module resolves (e.g., no session → `isLoading: false, isAuthenticated: false`)
5. The subscription callback fires → `setAuthState` updates React state
6. React re-renders `AuthProvider` → loading indicator disappears
7. Children render (e.g., login page or main app, depending on `isAuthenticated`)
8. Later: user logs in → auth module state changes → subscription fires → React re-renders → UI shows authenticated state

---

### Key Takeaways

- **Context is React's DI system.** Use it for cross-cutting concerns (auth, theme, locale) that many components need.
- **Providers hold logic; consumers are simple.** Keep the complex state management in the provider, expose a clean API via the hook.
- **`useEffect` with `[]` = componentDidMount.** It runs once after the first render. Return a cleanup function for subscriptions.
- **`useCallback` prevents unnecessary re-renders.** Wrap functions passed to children to maintain stable references.
- **Guard clauses in hooks** give clear error messages when components are used outside required providers.

---

### Glossary

| Term | Definition |
|------|-----------|
| **Context** | React's mechanism for passing data through the component tree without prop drilling |
| **Provider** | A component that supplies a context value to its descendants |
| **Consumer** | A component that reads a context value (via `useContext` hook) |
| **Prop Drilling** | Passing props through many intermediate components that don't use them — Context solves this |
| **useEffect** | Hook for performing side effects (subscriptions, API calls, timers) after render |
| **useCallback** | Hook that returns a memoized function reference to prevent unnecessary re-renders |
| **Memoization** | Caching a computed result so it's not recomputed unless inputs change |
| **Mount/Unmount** | When a component is added to (mount) or removed from (unmount) the DOM |
| **Side Effect** | Any operation that affects something outside the component's render output (network, DOM, subscriptions) |

---


## Task 4.3: Implement the HTTP Client with Interceptors

### Task Summary

We created an HTTP client using Axios that automatically attaches authentication tokens, adds correlation IDs for distributed tracing, normalizes all error responses into a consistent shape, and handles 401 (unauthorized) responses with automatic token refresh. This is analogous to configuring a `RestTemplate` or `WebClient` in Spring with filters/interceptors for auth, logging, and error handling.

**Key files:** `src/lib/http-client.ts`, `src/types/api.ts`

---

### React Concepts Used

#### 1. Axios Interceptor Pattern (Request/Response Chain)

**What it is:** Axios interceptors are functions that run before every request is sent or after every response is received. They form a pipeline — each interceptor can modify, augment, or reject the request/response.

**Analogy:** Exactly like Spring's `ClientHttpRequestInterceptor` for `RestTemplate`, or Servlet Filters in a filter chain. Each interceptor handles one cross-cutting concern.

```typescript
// Request interceptor: runs BEFORE every HTTP request leaves the browser
httpClient.interceptors.request.use((config) => {
  config.headers.Authorization = `Bearer ${token}`;  // Add auth
  config.headers['X-Correlation-ID'] = uuidv4();     // Add tracing
  return config;
});

// Response interceptor: runs AFTER every HTTP response arrives
httpClient.interceptors.response.use(
  (response) => response,                    // Success: pass through
  async (error) => { throw normalizeError(error); }  // Error: normalize
);
```

**Why interceptors?** They keep cross-cutting concerns (auth, tracing, error handling) out of individual API calls. Each call just does `httpClient.get('/events')` — no boilerplate.

#### 2. Error Normalization

**What it is:** All possible error shapes (network errors, timeouts, backend error bodies, non-standard responses) are transformed into one consistent `NormalizedError` interface.

```typescript
export interface NormalizedError {
  status: number;           // HTTP status (0 if no response)
  type: string;            // Error category ('TIMEOUT', 'NETWORK_ERROR', 'VALIDATION_ERROR', etc.)
  message: string;         // Human-readable message
  correlationId: string | null;  // For debugging with backend logs
  fieldErrors: Array<{ field: string; message: string }>;  // For form validation
}
```

**Analogy:** Like a Spring `@ControllerAdvice` that catches all exceptions and maps them to a standard error response body. The client-side equivalent ensures every error handler can rely on the same shape.

**Why:** Without normalization, every API call would need try/catch with branching logic to handle network errors vs. 400 vs. 500 vs. timeouts. With it, error handling is uniform everywhere.


#### 3. 401 Retry Logic with Silent Refresh

**What it is:** When the server returns 401 (token expired), the client automatically attempts to refresh the token and retry the original request ONCE. If refresh fails, it redirects to login.

```typescript
async function handleUnauthorized(error, instance): Promise<unknown> {
  const originalRequest = error.config;

  // Only attempt one retry per request
  if (originalRequest._retried) {
    authModule.login(); // Give up → redirect to login
    throw normalizeError(error);
  }

  originalRequest._retried = true;
  const newToken = await authModule.silentRefresh();

  if (!newToken) {
    authModule.login();
    throw normalizeError(error);
  }

  // Retry with fresh token
  originalRequest.headers.Authorization = `Bearer ${newToken}`;
  return instance(originalRequest);
}
```

**Why single-attempt?** Prevents infinite retry loops. If refresh fails, we know the session is truly expired — continuing to retry would just hammer the auth server uselessly.

**Analogy:** Like a Spring `RetryTemplate` with `maxAttempts(1)` — try once, fail gracefully.

#### 4. Pluggable Toast Handler (Dependency Inversion)

```typescript
let toastHandler: (toast: ToastPayload) => void = () => {};  // No-op default

export function setToastHandler(handler: (toast: ToastPayload) => void): void {
  toastHandler = handler;
}
```

**Why:** The HTTP client is a plain TypeScript module — it doesn't know about React, stores, or the toast system. But it needs to show warnings (e.g., "Rate limited, retry after 30s"). Instead of importing the toast store directly (which creates a circular dependency), we let the toast system "plug itself in" at app startup.

**Analogy:** Like Spring's `@Autowired` with a `@Lazy` or `ObjectProvider` — the HTTP client declares it needs a toast handler (interface), and the toast system provides the implementation later.

#### 5. UUID Correlation IDs for Distributed Tracing

```typescript
config.headers['X-Correlation-ID'] = uuidv4();
```

**What this does:** Every HTTP request gets a unique ID. If an error occurs, this ID appears in:
- The browser's toast notification (for error toasts)
- The backend's log files
- The API gateway's access logs

**Why:** When a user reports "I got an error," you can trace that exact request through all backend services using the correlation ID. Without it, debugging production issues requires time-range guessing.

**Analogy:** Like Spring Cloud Sleuth/Micrometer Tracing's trace IDs, but initiated by the frontend.

---

### Code Patterns Explained

#### Pattern: Keycloak Endpoint Detection

```typescript
const KEYCLOAK_PATH_SEGMENTS = ['/realms/', '/protocol/openid-connect/'];

function isKeycloakEndpoint(url: string | undefined): boolean {
  if (!url) return false;
  return KEYCLOAK_PATH_SEGMENTS.some((segment) => url.includes(segment));
}
```

**Why:** When the HTTP client makes requests to Keycloak's token endpoint (for refresh), it should NOT attach the Bearer token. Keycloak endpoints use their own authentication (the refresh token in the body). Attaching a Bearer token would confuse Keycloak.

---

### Step-by-Step Walkthrough: API Request Lifecycle

1. Component calls `httpClient.get('/events')`
2. **Request interceptor** fires:
   - Gets current access token from auth module
   - Checks if URL is a Keycloak endpoint (skip auth if so)
   - Attaches `Authorization: Bearer <token>` header
   - Generates UUID and attaches `X-Correlation-ID` header
3. Axios sends the request to the dev proxy (`/api` → `localhost:7093`)
4. **Response arrives:**
   - **200 OK:** Passes through unchanged → component receives data
   - **401 Unauthorized:** `handleUnauthorized` fires → silent refresh → retry
   - **429 Rate Limited:** Toast warning displayed → error still thrown
   - **Any error:** `normalizeError` transforms it → consistent shape thrown

---

### Key Takeaways

- **Interceptors centralize cross-cutting concerns.** Auth, tracing, and error handling are configured once, not repeated in every API call.
- **Error normalization** means UI error handling is simple — one interface to handle, regardless of what went wrong.
- **Single-attempt retry** prevents infinite loops while providing seamless UX for expired tokens.
- **Dependency inversion** (pluggable toast handler) keeps modules decoupled and prevents circular dependencies.
- **Correlation IDs** are essential for debugging in distributed systems — start them at the frontend.

---

### Glossary

| Term | Definition |
|------|-----------|
| **Interceptor** | A function that hooks into the request/response pipeline to modify or handle cross-cutting concerns |
| **Axios** | A popular HTTP client library for browsers and Node.js (like Apache HttpClient in Java) |
| **Correlation ID** | A unique identifier that follows a request through all services for distributed tracing |
| **Error Normalization** | Transforming diverse error shapes into one consistent structure |
| **Dependency Inversion** | Depending on abstractions (function interface) rather than concrete implementations |
| **Retry Logic** | Automatically retrying failed requests under specific conditions (like expired auth) |
| **Rate Limiting** | Server-side throttling that returns 429 when too many requests are sent |
| **Bearer Token** | An access token sent in the Authorization header to authenticate API requests |

---


## Task 5.1: Implement Zustand UI Store

### Task Summary

We implemented a global UI state store using Zustand — a lightweight state management library — to manage sidebar collapse state, theme preference, toast notifications, and offline status. The store persists user preferences (sidebar, theme) to localStorage while keeping ephemeral state (toasts, offline) in memory only. This is analogous to a combination of a Spring application's in-memory state with selective database persistence.

**Key file:** `src/stores/ui-store.ts`

---

### React Concepts Used

#### 1. Zustand — Lightweight State Management

**What it is:** Zustand is a minimal state management library. It creates a store (a single source of truth) that any component can subscribe to. When the store changes, only subscribed components re-render.

**Analogy:** Think of it like a shared in-memory `ConcurrentHashMap` that notifies listeners when specific keys change. Or like a simplified Redis pub/sub — components subscribe to the slices of state they care about.

**Zustand vs Redux:**
| Aspect | Redux | Zustand |
|--------|-------|---------|
| Boilerplate | Actions, reducers, action creators, dispatch | Just functions in the store |
| Bundle size | ~7KB | ~1KB |
| Setup | Provider required at app root | No provider needed |
| Learning curve | Steep (actions, reducers, middleware, selectors) | Minimal (it's just a hook) |

```typescript
import { create } from 'zustand';

export const useUIStore = create<UIState>()((set) => ({
  sidebarCollapsed: false,
  toggleSidebar: () => set((state) => ({ sidebarCollapsed: !state.sidebarCollapsed })),
  theme: 'system',
  setTheme: (theme) => set({ theme }),
}));
```

**Usage in components:**
```typescript
function Sidebar() {
  const collapsed = useUIStore((s) => s.sidebarCollapsed);  // Only re-renders when THIS field changes
  const toggle = useUIStore((s) => s.toggleSidebar);
  // ...
}
```

#### 2. Persist Middleware for localStorage

**What it is:** Zustand middleware that automatically serializes selected state to localStorage and rehydrates it on app load.

```typescript
persist(
  (set) => ({ ... }),  // Your store definition
  {
    name: 'outreach-ui-state',       // localStorage key
    storage: createJSONStorage(() => safeStorage),
    partialize: (state) => ({        // Only persist THESE fields
      sidebarCollapsed: state.sidebarCollapsed,
      theme: state.theme,
    }),
  }
)
```

**Analogy:** Like Spring Boot's `@ConfigurationProperties` with a backing file — preferences are saved and restored between sessions.

**Why `partialize`?** We don't want to persist toasts (ephemeral) or offline status (runtime-only). Only user preferences should survive a page refresh.


#### 3. Safe Storage Wrapper for Graceful Degradation

```typescript
const safeStorage: StateStorage = {
  getItem: (name: string): string | null => {
    try { return localStorage.getItem(name); }
    catch { return null; }
  },
  setItem: (name: string, value: string): void => {
    try { localStorage.setItem(name, value); }
    catch { /* Silently fail */ }
  },
  removeItem: (name: string): void => {
    try { localStorage.removeItem(name); }
    catch { /* Silently fail */ }
  },
};
```

**Why:** In some browsers (Safari private browsing, or when storage quota is exceeded), localStorage throws exceptions. Without this wrapper, the entire app would crash. With it, the app continues working — just without persistence.

**Analogy:** Like wrapping a cache (Redis) call in a try/catch — if the cache is down, the app still works, just without caching.

#### 4. Selector Pattern for Efficient Re-renders

```typescript
// ✅ Good: Component only re-renders when sidebarCollapsed changes
const collapsed = useUIStore((s) => s.sidebarCollapsed);

// ❌ Bad: Component re-renders when ANY store field changes
const store = useUIStore();
```

**Why:** Zustand uses reference equality (`===`) to determine if a component should re-render. The selector function returns just the slice of state the component needs. If that slice hasn't changed, the component skips re-rendering.

**Analogy:** Like a database query selecting specific columns (`SELECT name FROM users`) rather than `SELECT * FROM users`. You only subscribe to what you need.

#### 5. Merge Strategy for Corrupted Data

```typescript
merge: (persistedState, currentState) => {
  if (persistedState && typeof persistedState === 'object') {
    const persisted = persistedState as Partial<...>;
    return {
      ...currentState,
      sidebarCollapsed:
        typeof persisted.sidebarCollapsed === 'boolean'
          ? persisted.sidebarCollapsed
          : DEFAULT_SIDEBAR_COLLAPSED,
      theme: ['light', 'dark', 'system'].includes(persisted.theme)
        ? persisted.theme
        : DEFAULT_THEME,
    };
  }
  return currentState; // Corrupted → use defaults
}
```

**Why:** localStorage data can be corrupted (manual editing, version mismatch, browser bugs). The merge function validates every field before trusting it — falling back to safe defaults if anything is unexpected.

**Analogy:** Like defensive JSON parsing in a REST controller — validate the input before using it, even if it "should" always be correct.

---

### Code Patterns Explained

#### Pattern: Toast Queue with Max Size

```typescript
addToast: (toast) => set((state) => {
  const newToast = { ...toast, id: generateId(), timestamp: Date.now() };
  const updatedToasts = [...state.toasts, newToast];
  if (updatedToasts.length > MAX_TOASTS) {
    return { toasts: updatedToasts.slice(updatedToasts.length - MAX_TOASTS) };
  }
  return { toasts: updatedToasts };
}),
```

**Why max size?** Without a cap, a runaway error loop could produce thousands of toasts, consuming memory and crashing the browser. The cap (50) provides a safety valve.

**Analogy:** Like a bounded `BlockingQueue` in Java — oldest entries are dropped when capacity is reached.

---

### Step-by-Step Walkthrough

1. App starts → Zustand creates the store in memory with default values
2. Persist middleware reads `outreach-ui-state` from localStorage
3. `merge` function validates persisted data → restores valid preferences
4. Components call `useUIStore((s) => s.theme)` → get current theme
5. User toggles sidebar → `toggleSidebar()` updates store
6. Persist middleware serializes `{ sidebarCollapsed, theme }` to localStorage
7. All components subscribed to `sidebarCollapsed` re-render
8. Components subscribed to OTHER fields (e.g., `theme`) do NOT re-render

---

### Key Takeaways

- **Zustand is minimal by design.** No providers, no reducers, no action types. Just a store and selectors.
- **Selectors prevent wasted re-renders.** Subscribe to the smallest slice of state you need.
- **Partialize** controls what gets persisted. Keep ephemeral state out of localStorage.
- **Safe storage wrappers** prevent crashes in restricted browser environments.
- **Merge strategies** validate persisted data defensively — never trust user-controlled storage.
- **Bounded collections** prevent memory leaks from unbounded growth.

---

### Glossary

| Term | Definition |
|------|-----------|
| **Zustand** | A lightweight state management library for React (German for "state") |
| **Store** | A centralized container holding application state and update functions |
| **Selector** | A function that extracts a specific slice of state from a store |
| **Middleware** | Code that wraps store behavior to add features (persistence, logging, etc.) |
| **Persist** | Zustand middleware that saves/restores state to/from localStorage |
| **Partialize** | Selecting which parts of state to persist (excluding ephemeral data) |
| **Graceful Degradation** | Continuing to work (with reduced functionality) when a subsystem fails |
| **Rehydration** | Restoring previously persisted state when the app loads |
| **Reference Equality** | Comparing values by memory address (`===`) rather than deep comparison |

---


## Task 5.2: Implement Toast Notification System

### Task Summary

We built a toast notification system with two parts: a custom hook (`useToast`) providing a convenience API for enqueueing notifications, and a visual component (`ToastContainer` + `ToastItem`) that renders and auto-dismisses them. This is analogous to how a Spring application might use an event bus for notifications — producers emit events, consumers render them.

**Key files:** `src/hooks/useToast.ts`, `src/components/feedback/ToastContainer.tsx`

---

### React Concepts Used

#### 1. Custom Hooks Wrapping Store Actions

**What it is:** `useToast` is a custom hook that provides a cleaner API on top of the raw Zustand store actions. Instead of `useUIStore((s) => s.addToast)` everywhere, components call `toast.success("Saved!")`.

```typescript
export function useToast(): UseToastReturn {
  const addToast = useUIStore((s) => s.addToast);
  const dismissToast = useUIStore((s) => s.dismissToast);

  const success = useCallback((message: string) => {
    addToast({ severity: 'success', message });
  }, [addToast]);

  // ... error, warning, info helpers

  return { toast, success, error, warning, info, dismiss };
}
```

**Analogy:** Like a Spring Service layer that wraps Repository calls with business logic. The repository (store) is generic; the service (hook) provides domain-specific convenience methods.

**Why separate from the store?** Hooks can use React features (`useCallback`). Stores are plain TypeScript. This separation keeps each layer focused on its responsibility.

#### 2. Component Composition (ToastContainer + ToastItem)

**What it is:** Instead of one monolithic component, we split into:
- `ToastContainer` — manages the list, wires the HTTP client handler, slices visible toasts
- `ToastItem` — renders one toast, handles auto-dismiss timer, handles close button

```typescript
export function ToastContainer() {
  const visibleToasts = toasts.slice(-MAX_VISIBLE);
  return (
    <div role="region" aria-label="Notifications" aria-live="polite">
      {visibleToasts.map((toast) => (
        <ToastItem key={toast.id} toast={toast} onDismiss={dismissToast} />
      ))}
    </div>
  );
}
```

**Analogy:** Like Spring's MVC pattern — the controller (Container) manages data flow; the view (Item) handles presentation. Each has a single responsibility.

#### 3. `useEffect` for Auto-Dismiss Timers with Cleanup

```typescript
useEffect(() => {
  if (toast.severity === 'error') return;  // Errors persist until manual close

  timerRef.current = setTimeout(handleDismiss, AUTO_DISMISS_MS);

  return () => {
    if (timerRef.current) clearTimeout(timerRef.current);
  };
}, [toast.severity, handleDismiss]);
```

**What this does:**
1. When a non-error toast mounts, start a 5-second timer
2. When the timer fires, dismiss the toast
3. If the toast is dismissed manually (or component unmounts), CANCEL the timer

**Why the cleanup function?** Without it, if the user dismisses a toast quickly, the timer would fire after 5 seconds and try to dismiss an already-gone toast (or worse, dismiss a different toast that reused the ID).

**Analogy:** Like cancelling a `ScheduledFuture` in Java's `ScheduledExecutorService` — if the task is no longer needed, cancel it to prevent resource leaks.


#### 4. CSS Modules for Scoped Styling

```typescript
import styles from './ToastContainer.module.css';

// Usage:
<div className={styles.container}>
<div className={`${styles.toast} ${styles[toast.severity]}`}>
```

**What it is:** CSS Modules automatically scope class names to the component. The class `.container` in `ToastContainer.module.css` becomes something like `.ToastContainer_container_a1b2c` in the output — guaranteed unique.

**Why:** Prevents style collisions. Without scoping, a `.container` class in one component could accidentally style elements in another component. CSS Modules make styles local by default.

**Analogy:** Like Java packages — `com.company.auth.User` and `com.company.billing.User` don't collide because they're in different namespaces. CSS Modules give CSS the same namespacing.

#### 5. ARIA Accessibility Attributes

```typescript
<div role="region" aria-label="Notifications" aria-live="polite">
  <div role="alert" aria-atomic="true">
    <button aria-label="Dismiss notification">×</button>
  </div>
</div>
```

| Attribute | Purpose |
|-----------|---------|
| `role="region"` | Marks the container as a landmark for screen readers |
| `aria-label="Notifications"` | Names the region for screen reader navigation |
| `aria-live="polite"` | Screen readers announce new toasts without interrupting current reading |
| `role="alert"` | Each toast is announced as an alert |
| `aria-atomic="true"` | The entire toast is read as one unit (not individual changes) |
| `aria-label="Dismiss"` | The `×` button has a meaningful label (not just "times symbol") |

**Why:** Screen reader users can't see toast popups. ARIA attributes ensure they hear notifications and can interact with dismiss buttons.

---

### Code Patterns Explained

#### Pattern: Wiring HTTP Client Toast Handler on Mount

```typescript
useEffect(() => {
  const handler = (payload: ToastPayload) => {
    addToast({ severity: payload.severity, message: payload.message, correlationId: payload.correlationId });
  };
  setToastHandler(handler);
}, []);
```

**What this does:** On first render, the ToastContainer registers itself as the HTTP client's toast handler. Now when the HTTP client encounters a 429 (rate limit), it calls `toastHandler(...)` which flows through the Zustand store into the ToastContainer.

**Why in `useEffect`?** The connection between the HTTP client and the toast system only needs to happen once, after the component mounts. This is the dependency inversion from Task 4.3 being fulfilled.

#### Pattern: `useRef` for Timer References

```typescript
const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
```

**Why `useRef` instead of a regular variable?** Regular variables are re-created on every render. `useRef` gives you a mutable box that persists across renders without causing re-renders when changed. Perfect for storing timer IDs, DOM references, or any "instance variable."

**Analogy:** Like an instance field in a Java class — it persists for the object's lifetime. But unlike `useState`, changing it does NOT trigger a re-render.

---

### Step-by-Step Walkthrough

1. App renders `<ToastContainer />` — it mounts and wires the HTTP client handler
2. Something triggers a toast (user action, API error, rate limit)
3. `addToast` is called → Zustand store adds the toast with a unique ID and timestamp
4. `ToastContainer` re-renders (subscribed to `toasts`) → shows up to 5 visible toasts
5. `ToastItem` mounts → starts a 5-second auto-dismiss timer (unless it's an error)
6. Timer fires → `handleDismiss` removes the toast from the store
7. `ToastContainer` re-renders → toast disappears
8. Or: User clicks `×` → `handleDismiss` fires → timer cleanup runs → toast disappears

---

### Key Takeaways

- **Custom hooks** provide clean APIs on top of raw stores. They're the "Service layer" of React.
- **Component composition** (Container + Item) keeps each component focused and testable.
- **Cleanup functions in `useEffect`** are essential for timers, subscriptions, and event listeners. Always clean up what you set up.
- **CSS Modules** give you scoped styling without a runtime CSS-in-JS library.
- **ARIA attributes** are not optional — they make your app usable for the ~15% of users with disabilities.
- **`useRef`** stores mutable values that persist across renders without triggering re-renders.

---

### Glossary

| Term | Definition |
|------|-----------|
| **Toast** | A brief, non-modal notification that appears temporarily to inform the user |
| **CSS Modules** | A build-time CSS scoping technique that generates unique class names per component |
| **ARIA** | Accessible Rich Internet Applications — attributes that help assistive technologies understand your UI |
| **aria-live** | Tells screen readers to announce dynamic content changes ("polite" = after current reading, "assertive" = immediately) |
| **useRef** | Hook that stores a mutable value persisting across renders without causing re-renders |
| **Component Composition** | Building complex UIs from small, focused, reusable components |
| **Custom Hook** | A function starting with `use` that encapsulates reusable stateful logic |
| **Cleanup Function** | The function returned by useEffect, called when the component unmounts or before the effect re-runs |

---


## Task 6.1: Configure QueryClient and Query Key Factory

### Task Summary

We configured TanStack Query (React Query) as the server-state management layer and created a query key factory for consistent, hierarchical cache key management. This is analogous to configuring a caching layer (like Caffeine or Redis) in a Spring application with structured cache key patterns and time-based eviction policies.

**Key files:** `src/lib/query-client.ts`, `src/lib/query-keys.ts`

---

### React Concepts Used

#### 1. TanStack Query Client Configuration

**What it is:** TanStack Query (formerly React Query) manages "server state" — data that lives on the server and is fetched, cached, synchronized, and updated in the background. The `QueryClient` is the central configuration that defines caching behavior.

```typescript
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,           // Data is "fresh" for 30 seconds
      gcTime: 5 * 60 * 1000,      // Unused data is garbage-collected after 5 minutes
      refetchOnWindowFocus: true,  // Refetch stale data when tab regains focus
      retry: 3,                    // Retry failed requests 3 times
    },
  },
});
```

**Analogy:** Like configuring a Spring Cache with `@CacheConfig`:
- `staleTime` → like a cache TTL (time-to-live)
- `gcTime` → like a cache eviction timeout
- `retry` → like Spring Retry's `maxAttempts`
- `refetchOnWindowFocus` → like a cache invalidation trigger

**Why server state is different from UI state:**

| Aspect | UI State (Zustand) | Server State (TanStack Query) |
|--------|-------------------|-------------------------------|
| Owned by | The client | The server |
| Source of truth | In-memory store | Backend database |
| Stale? | Never (you set it, it's current) | Always potentially (someone else may have changed it) |
| Examples | Sidebar collapsed, theme | Events list, user profile, feedback data |

#### 2. Stale-While-Revalidate Caching Strategy

**What it is:** When data is "stale" (older than `staleTime`), TanStack Query immediately returns the cached version (fast!) AND fetches fresh data in the background. When the fresh data arrives, the UI updates seamlessly.

**The timeline:**
```
User navigates to /events:
  t=0s  → Query fires, no cache → loading spinner → fetch from server → display results
  t=10s → User navigates away
  t=20s → User returns to /events (within 30s staleTime):
          → Instant display from cache (no spinner!) → data still "fresh"
  t=45s → User returns to /events (past 30s staleTime):
          → Instant display from cache → ALSO refetches in background → UI updates if data changed
  t=6m  → User returns to /events (past 5m gcTime):
          → Cache was garbage-collected → loading spinner → fresh fetch
```

**Analogy:** Like a CDN with a `max-age` header. Serve stale content instantly, revalidate in the background. Users get speed AND freshness.

#### 3. Query Key Factory Pattern

```typescript
export const queryKeys = {
  events: {
    all: ['events'] as const,
    lists: () => [...queryKeys.events.all, 'list'] as const,
    list: (params: EventListParams) => [...queryKeys.events.lists(), params] as const,
    details: () => [...queryKeys.events.all, 'detail'] as const,
    detail: (id: string) => [...queryKeys.events.details(), id] as const,
  },
  // ... more domains
};
```

**What this creates (example):**
```
queryKeys.events.all          → ['events']
queryKeys.events.lists()      → ['events', 'list']
queryKeys.events.list({page:1}) → ['events', 'list', {page:1}]
queryKeys.events.details()    → ['events', 'detail']
queryKeys.events.detail('abc') → ['events', 'detail', 'abc']
```

**Why a factory?** Query keys are the "cache keys" for TanStack Query. Getting them wrong means stale data, cache misses, or invalidation failures. A factory ensures consistency — you never manually type `['events', 'list']` and risk a typo.


#### 4. Hierarchical Cache Invalidation

**The power of hierarchical keys:**

```typescript
// After creating a new event:
queryClient.invalidateQueries({ queryKey: queryKeys.events.all });
// This invalidates:
//   ['events']                     ← matches
//   ['events', 'list']             ← matches (starts with ['events'])
//   ['events', 'list', {page:1}]   ← matches
//   ['events', 'detail']           ← matches
//   ['events', 'detail', 'abc']    ← matches
// But NOT:
//   ['volunteers']                 ← doesn't match
//   ['feedback']                   ← doesn't match

// After updating a specific event:
queryClient.invalidateQueries({ queryKey: queryKeys.events.detail('abc') });
// Only invalidates: ['events', 'detail', 'abc']
// Lists are NOT refetched (they might still show outdated data though)
```

**Analogy:** Like a directory structure for cache keys:
```
events/
├── list/
│   ├── {page:1}
│   └── {page:2}
└── detail/
    ├── abc
    └── def
```

Invalidating `events/` clears the whole subtree. Invalidating `events/detail/abc` only clears that one entry.

**Why this matters:** After a mutation (create/update/delete), you need to tell the cache "this data might be stale." Hierarchical keys let you invalidate at the right granularity — not too broad (wasteful refetches) and not too narrow (stale data persists).

---

### Code Patterns Explained

#### Pattern: `as const` for Type-Safe Query Keys

```typescript
all: ['events'] as const,
lists: () => [...queryKeys.events.all, 'list'] as const,
```

**What `as const` does:** Without it, TypeScript infers `string[]` (any strings in any order). With it, TypeScript infers the exact tuple type `readonly ['events', 'list']`. This means:
- Typos are caught at compile time
- Autocomplete works
- Key hierarchies are enforced by the type system

**Analogy:** Like using an `enum` instead of `String` in Java — you get compile-time validation rather than runtime "oops, wrong key."

#### Pattern: Parameter Inclusion in Keys

```typescript
list: (params: EventListParams) => [...queryKeys.events.lists(), params] as const,
```

**Why include params in the key?** The same endpoint with different query parameters returns different data. `GET /events?page=1` and `GET /events?page=2` are different cache entries. Including params in the key ensures they're cached separately.

**Analogy:** Like a composite cache key in Spring: `@Cacheable(key = "'events:list:' + #params.hashCode()")`

---

### Step-by-Step Walkthrough

1. App wraps root in `<QueryClientProvider client={queryClient}>`
2. Component calls `useQuery({ queryKey: queryKeys.events.list({page:1}), queryFn: ... })`
3. TanStack Query checks: is `['events', 'list', {page:1}]` in cache?
4. **Cache miss:** Shows loading state → fetches data → caches result → displays
5. **Cache hit (fresh):** Returns cached data immediately → no fetch
6. **Cache hit (stale):** Returns cached data immediately → fetches in background → updates if different
7. After mutation: `queryClient.invalidateQueries({ queryKey: queryKeys.events.lists() })`
8. All list queries for events become stale → active ones refetch automatically

---

### Key Takeaways

- **TanStack Query manages server state.** Use Zustand for UI state, TanStack Query for data from the server. Don't mix them up.
- **Stale-while-revalidate** gives both speed (cached data) and freshness (background refetch). Users perceive instant page loads.
- **Query key factories** prevent key-related bugs and enable hierarchical invalidation.
- **`as const`** gives you type-safe, autocomplete-friendly keys at zero runtime cost.
- **Granular invalidation** is why key hierarchy matters — invalidate what changed, not everything.
- **`gcTime` vs `staleTime`:** Stale data is still SERVED from cache (with background refresh). GC'd data is GONE (shows loading spinner again). They serve different purposes.

---

### Glossary

| Term | Definition |
|------|-----------|
| **TanStack Query** | A library for fetching, caching, and synchronizing server state in React (formerly React Query) |
| **QueryClient** | Central configuration object controlling cache behavior, retry logic, and defaults |
| **Query Key** | An array that uniquely identifies a cached query — like a cache key in a HashMap |
| **staleTime** | Duration (ms) after which cached data is considered "stale" and eligible for background refetch |
| **gcTime** | Duration (ms) after which UNUSED cached data is garbage-collected (freed from memory) |
| **Stale-While-Revalidate** | Strategy: serve stale cached data immediately, revalidate in the background, update if different |
| **Invalidation** | Marking cached data as stale, triggering a refetch for active queries |
| **Query Key Factory** | A structured object that produces consistent, hierarchical query keys |
| **Hierarchical Keys** | Keys structured as nested arrays, enabling prefix-based invalidation |
| **as const** | TypeScript assertion that narrows a value to its literal type (enabling type-safe tuples) |
| **Server State** | Data owned by and fetched from the server — always potentially stale |
| **Background Refetch** | Fetching fresh data without showing a loading spinner (user sees stale data until fresh arrives) |

---
