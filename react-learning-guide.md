# React Learning Guide — Frontend Implementation

This guide is for developers who know Java/Spring but have never touched React. It covers every React, TypeScript, and frontend concept used in this project. Think of it as a translation layer between the Spring world you know and the React world you're entering.

---

## React ↔ Spring Quick Reference

| React Concept | Spring Equivalent | What It Does |
|---------------|-------------------|--------------|
| Component | `@Controller` that returns a view | A function that produces UI |
| Props | Constructor parameters / method arguments | Data passed into a component |
| `useState` | `@Stateful` session bean's instance variable | Holds data that can change and triggers UI updates |
| `useEffect` | `@PostConstruct` / `@PreDestroy` lifecycle hooks | Runs code after render (setup/cleanup) |
| React Context | `ApplicationContext` / Dependency Injection | Shares data across the component tree without passing manually |
| Custom Hook | `@Service` utility class | Reusable logic extracted into a function |
| React Router | Spring MVC `@RequestMapping` | Maps URLs to components (pages) |
| TanStack Query | Spring Cache + `RestTemplate` combined | Fetches, caches, and syncs server data |
| Zustand Store | Singleton `@Service` with `@Scope("application")` | Global shared state container |
| Error Boundary | `@ControllerAdvice` / Global exception handler | Catches errors in the component tree |
| `useMemo` | `@Cacheable` method | Caches a computed value so it isn't recalculated unnecessarily |
| `useCallback` | Cached method reference | Caches a function reference to prevent unnecessary re-renders |
| `useRef` | Instance field in a class | Stores a mutable value that persists across renders |
| JSX | Thymeleaf/JSP template embedded in code | HTML-like syntax inside JavaScript |
| Vite | Maven/Gradle + embedded Tomcat | Build tool and dev server |
| `package.json` | `pom.xml` | Declares dependencies and scripts |
| `node_modules` | `.m2/repository` (but per-project) | Installed dependency files |
| Path Aliases (`@/`) | Java package imports | Clean import paths |
| CSS Modules | Java packages (namespace isolation) | Scoped styles that don't leak |
| Axios Interceptors | Servlet Filters / `ClientHttpRequestInterceptor` | Cross-cutting logic on HTTP requests |
| ARIA attributes | Accessibility compliance annotations | Makes UI usable by screen readers |

---

## Project Scaffolding and Build Configuration

We created the foundational project structure for a React 19 single-page application (SPA) using Vite as the build tool and TypeScript for type safety. This is like creating a new Maven project with Spring Boot — you set up the build system, project layout, and dependency management before writing business logic.

---

### React Concepts Used

#### 1. JSX (JavaScript XML)

JSX lets you write HTML-like markup inside your JavaScript/TypeScript files. It looks like HTML but gets compiled into JavaScript function calls.

> **Spring Equivalent:** Like a Thymeleaf or JSP template, but living inside your logic file instead of being separate.

```tsx
// This JSX:
<h1>Hello World</h1>

// Gets compiled by Vite into:
React.createElement('h1', null, 'Hello World')
```

It exists to make UI code readable. Instead of writing nested function calls, you write something that looks like the output HTML.

#### 2. Components (Functions that Return UI)

A React component is a JavaScript function that returns JSX. It's the basic building block of any React app.

> **Spring Equivalent:** A `@Controller` method that returns a view. You give it some inputs, it produces what should appear on screen.

```tsx
// A component is just a function:
function App() {
  return <h1>Hello</h1>
}

// You use it like an HTML tag:
<App />
```

Components let you break a complex UI into small, reusable pieces — like breaking a large service into small classes with single responsibilities.

#### 3. State (`useState` Hook)

State is data that lives inside a component and can change over time. When state changes, React re-runs the component function to produce updated UI.

> **Spring Equivalent:** An instance variable in a `@Stateful` session bean. When you change it, the framework automatically updates what the user sees.

```tsx
const [count, setCount] = useState(0)
// count = current value (like a getter)
// setCount = function to update it (like a setter that triggers a screen repaint)
// 0 = initial value
```

UIs are inherently stateful — forms have values, buttons can be toggled, counters increment. React uses state to know WHAT changed so it can update only the affected parts of the page.

#### 4. StrictMode

A development-only wrapper that enables extra checks and warnings. It renders nothing visible.

```tsx
<StrictMode>
  <App />
</StrictMode>
```

> **Spring Equivalent:** Running your app with `-ea` (enable assertions) or attaching a lint plugin. It double-invokes certain functions to detect side effects early.

#### 5. The Virtual DOM and Rendering

React doesn't manipulate the browser's DOM directly. Your component returns a lightweight JavaScript description of what the UI should look like. React compares the new description with the previous one and applies only the minimal changes to the real DOM.

> **Spring Equivalent:** Like a database migration tool (Liquibase/Flyway). Instead of dropping and recreating the table every time, it diffs current vs. desired state and generates only the necessary ALTER statements.

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

Step by step:
1. `document.getElementById('root')` — finds the `<div id="root">` in `index.html`
2. `createRoot(...)` — tells React "this DOM element is where you'll render everything"
3. `.render(...)` — "render this component tree into that container"

> **Spring Equivalent:** `SpringApplication.run(MyApp.class, args)` — it bootstraps the entire application. The `<div id="root">` is like the embedded Tomcat that hosts everything.

#### Pattern 2: Single-File Components

Unlike Angular (which uses separate `.ts`, `.html`, `.css` files per component), React puts logic and markup together in one file. The component function IS the template.

Why? The markup is tightly coupled to the logic driving it. Keeping them together eliminates synchronization bugs between files.

#### Pattern 3: Module System (ES Modules)

```tsx
import { useState } from 'react'         // Named import from a package
import App from './App.tsx'               // Default import from a local file
export default App                        // Default export
```

> **Spring Equivalent:** Java `import` statements. Named imports `{ useState }` are like importing a specific class. Default imports are like importing the "main" class from a package.

This enables tree-shaking — the build tool removes unused code. If you only import `useState`, the bundler won't include all 50+ other React functions in your final output.

---

### How a Vite + React Project Works (Step by Step)

Here's what happens from `npm run dev` to seeing the app in your browser:

1. **`npm run dev`** executes the `"dev": "vite"` script from `package.json`
2. **Vite starts a dev server** on `http://localhost:5173`. Unlike older tools, Vite doesn't bundle everything upfront — it serves files on demand.
3. **Browser requests `index.html`** which contains:
   ```html
   <div id="root"></div>
   <script type="module" src="/src/main.tsx"></script>
   ```
4. **Browser requests `/src/main.tsx`** — Vite transforms TypeScript/JSX on the fly and serves plain JavaScript
5. **`main.tsx` executes** — finds the root element, creates a React root, renders `<App />`
6. **React calls the `App()` function**, which returns a virtual DOM tree
7. **React renders to real DOM** — the browser paints pixels
8. **HMR (Hot Module Replacement):** When you save a file, Vite tells the browser "this module changed." React swaps just that component without a full page reload.

**Build mode (`npm run build`)** is different:
1. `tsc -b` type-checks ALL files (catches type errors)
2. `vite build` bundles everything into optimized static files with content hashes
3. Output goes to `dist/` — plain HTML/CSS/JS files servable from any static host (Nginx, S3, CDN)

---

### Understanding the Build System (Vite)

> **Spring Equivalent:** Vite is to frontend what Maven/Gradle is to Java — the build tool. But it also includes a dev server (like Spring Boot's embedded Tomcat).

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
    react(),                    // Transforms JSX + enables Hot Module Replacement
    checker({                   // Type-checks during dev (shows errors in browser)
      typescript: {
        tsconfigPath: './tsconfig.app.json',
      },
    }),
  ],
  resolve: {
    alias: {                    // Path aliases — clean import paths
      '@/components': path.resolve(__dirname, 'src/components'),
      '@/lib': path.resolve(__dirname, 'src/lib'),
    },
  },
  server: {
    proxy: {                    // Dev proxy — forwards API calls to your backend
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
        entryFileNames: 'assets/[name]-[hash].js',
        chunkFileNames: 'assets/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash].[ext]',
      },
    },
  },
})
```

#### Why Path Aliases?

Without aliases:
```tsx
import { Button } from '../../../components/ui/Button'
```

With aliases:
```tsx
import { Button } from '@/components/ui/Button'
```

> **Spring Equivalent:** Like Java package imports. You don't write `../../src/main/java/com/company/util/Helper.java` — you write `import com.company.util.Helper`.

**Important:** Aliases must be configured in TWO places:
1. `tsconfig.app.json` → for TypeScript/IDE resolution (so your editor doesn't show errors)
2. `vite.config.ts` → for the build tool (so bundling resolves the paths)

If you only configure one, either the IDE or the build will be confused.

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

This splits the project into two TypeScript "projects":
- `tsconfig.app.json` — rules for your React app code (runs in the browser)
- `tsconfig.node.json` — rules for build config files like `vite.config.ts` (runs in Node.js)

Why? Browser code has `document` and `window`. Node.js code has `fs`, `path`, `process`. By splitting configs, TypeScript knows which APIs are valid in each context.

> **Spring Equivalent:** Like having separate Maven profiles for "test" and "production" — same project, different compilation rules.

#### Key `tsconfig.app.json` Settings

| Setting | What It Does | Why It Matters |
|---------|-------------|----------------|
| `"strict": true` | Enables ALL strict type checks | Catches null pointer equivalents at compile time |
| `"noEmit": true` | TypeScript only checks types, doesn't produce JS | Vite handles compilation (much faster) |
| `"jsx": "react-jsx"` | Enables JSX transformation | You can write JSX without manually importing React |
| `"moduleResolution": "bundler"` | Resolves modules the way modern bundlers do | Matches Vite's resolution algorithm |
| `"noUnusedLocals": true` | Error on unused variables | Keeps code clean |
| `"noUncheckedIndexedAccess": true` | Array access returns `T \| undefined` | Forces null checks — prevents "undefined is not a function" |

**`strict: true` is crucial.** It's the TypeScript equivalent of `@NonNull` annotations everywhere in Java. Without it, TypeScript provides almost no safety.

---

### Understanding Environment Variables

```bash
# .env (base — always loaded)
VITE_API_URL=/api
VITE_KEYCLOAK_REALM=outreach
VITE_KEYCLOAK_CLIENT_ID=outreach-studio
```

Rules:
1. Only variables prefixed with `VITE_` are accessible in browser code via `import.meta.env.VITE_*`
2. Variables WITHOUT the prefix are NOT exposed (prevents leaking secrets to the browser)
3. `.env` → always, `.env.development` → dev mode, `.env.production` → build mode

> **Spring Equivalent:** `application.yml` / `application-dev.yml` / `application-prod.yml` with profile activation.

**Security note:** EVERYTHING in a browser bundle is visible to users. The `VITE_` prefix makes you consciously opt-in to exposing a variable. Never put secrets (API keys, database passwords) in `VITE_` variables.

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

When the browser requests `/api/events`, Vite intercepts it and forwards it to `http://localhost:7093/api/events`.

Why? The SPA runs on `localhost:5173` but the API Gateway runs on `localhost:7093`. Without a proxy, the browser blocks the request due to CORS (Cross-Origin Resource Sharing) — browsers refuse to let one origin talk to another by default.

> **Spring Equivalent:** Like an Nginx `proxy_pass` directive, but built into the dev server for convenience. In production, real Nginx handles this.

---

### Understanding Content-Hashed Filenames

```typescript
output: {
  entryFileNames: 'assets/[name]-[hash].js',   // e.g., assets/main-a1b2c3d4.js
}
```

Every time you change code, the filename changes (because the hash of the content changes). This solves browser caching — if you deploy a fix, users with the old cached `main.js` would never get the new version. But `main-a1b2c3d4.js` → `main-e5f6g7h8.js` is a new URL, so the browser fetches it fresh.

> **Spring Equivalent:** Like versioned JAR filenames (`myapp-1.2.3.jar`) except it's automatic and content-based.

---

### Understanding `vite-plugin-checker`

```typescript
checker({
  typescript: {
    tsconfigPath: './tsconfig.app.json',
  },
})
```

This runs the TypeScript compiler in a separate thread during development. If you introduce a type error, it shows an overlay in the browser with the file, line number, and error.

Without this plugin, Vite doesn't type-check — it only strips types and bundles. Errors would slip through until you run `tsc` manually or build.

The dual safety net:
- `vite-plugin-checker` → catches type errors DURING development (immediate feedback)
- `tsc -b` in the build script → catches type errors BEFORE bundling (prevents broken deployments)

> **Spring Equivalent:** Like having a linter in your IDE (immediate red squiggles) AND a CI check (prevents merge if lint fails). Belt and suspenders.

---

### Key Takeaways

- **React is a library, not a framework.** It handles UI rendering. You choose everything else (routing, state, HTTP). Angular is opinionated; React is flexible.
- **Components are just functions.** They take inputs (props) and return UI descriptions (JSX). No class hierarchies, no lifecycle XML.
- **State drives the UI.** Change state → React re-renders → DOM updates. You never manually touch the DOM.
- **Vite is the build tool AND dev server.** Instant startup, fast HMR, optimized production builds.
- **TypeScript strict mode is non-negotiable.** Without it, you lose most of TypeScript's value.
- **Path aliases** keep imports clean. Configure in BOTH `tsconfig` (IDE) and `vite.config` (build).
- **Environment variables** with `VITE_` prefix are the only safe way to inject config into browser code. Never put secrets here.
- **The build produces static files.** Unlike a Spring Boot JAR, `npm run build` produces HTML/CSS/JS that any static host can serve. No runtime needed.

---

### Glossary

| Term | Definition |
|------|-----------|
| **SPA** | Single-Page Application — one HTML page, all navigation handled by JavaScript |
| **JSX** | JavaScript XML — HTML-like syntax inside JavaScript |
| **Component** | A function that returns JSX, representing a reusable piece of UI |
| **State** | Data that can change over time, causing React to re-render |
| **Hook** | A function starting with `use` that lets you access React features from function components |
| **HMR** | Hot Module Replacement — updating code in the browser without a full page reload |
| **Vite** | A fast build tool for modern web projects (pronounced "veet", French for "fast") |
| **Bundling** | Combining many source files into fewer optimized files for production |
| **Tree-shaking** | Removing unused code from the final bundle |
| **TypeScript** | A typed superset of JavaScript — compiles to plain JS. Like Kotlin to Java, but for the browser |
| **ESM** | ES Modules — the native JavaScript module system using `import`/`export` |
| **Virtual DOM** | A lightweight JS representation of the real DOM for efficient updates |
| **Content Hash** | A fingerprint of file contents used in filenames for cache busting |
| **Path Alias** | A shorthand import path (like `@/components`) that maps to a real directory |
| **Proxy** | Forwarding requests to avoid CORS issues during development |
| **tsconfig** | TypeScript configuration file controlling type checking and module resolution |
| **Plugin** | An extension to Vite that adds functionality (React support, type checking, etc.) |
| **npm** | Node Package Manager — like Maven Central for Java |
| **package.json** | Project metadata and dependencies (like `pom.xml`) |
| **node_modules** | Installed dependencies directory (like `.m2/repository` but per-project) |

---


## Auth Module with PKCE

We implemented a complete OAuth2 Authorization Code + PKCE (Proof Key for Code Exchange) authentication module. It handles login, token exchange, silent refresh, and logout against a Keycloak identity provider.

> **Spring Equivalent:** This is like implementing a Spring Security OAuth2 client — but running entirely in the browser, where you cannot store secrets securely.

**Key files:** `src/lib/auth.ts`, `src/types/auth.ts`

---

### React Concepts Used

#### 1. Factory Function Pattern (Module Pattern)

Instead of a class, we use a function (`createAuthModule()`) that returns an object with methods. The function's local variables act as private state — they can't be accessed from outside.

> **Spring Equivalent:** A Java class with private fields and public methods, expressed as a closure. The local variables are the "private fields," the returned methods are the "public API."

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

Why this over a class? In JavaScript, closures provide true privacy (no `#private` or `_convention` needed). It also avoids `this` binding issues that plague class-based code in React.

#### 2. Listener/Subscriber Pattern (Observer Pattern)

Components can subscribe to auth state changes via `onStateChange(callback)`. When internal state changes, all registered listeners are notified.

> **Spring Equivalent:** Exactly like `ApplicationEventPublisher` / `@EventListener`. Something changes internally → all subscribers are notified.

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

React components need to re-render when auth state changes. The subscribe mechanism lets the AuthProvider listen for changes and update React state accordingly.

#### 3. In-Memory Token Storage

Access and refresh tokens are stored ONLY in JavaScript variables — never in `localStorage` or cookies.

> **Spring Equivalent:** Like keeping a secret key only in JVM heap memory (a `private` field) rather than writing it to a file or database.

Why? `localStorage` is vulnerable to XSS attacks — any injected script can read it. In-memory storage means tokens are lost on page refresh (requiring re-authentication), but they cannot be stolen by malicious scripts.

---

### Code Patterns Explained

#### Pattern 1: PKCE Flow (Why It Exists)

**Problem:** Browser apps can't keep secrets. Unlike a Spring Boot backend with a `client_secret` in its config, browser JavaScript is fully inspectable. Anyone can view source and extract hardcoded secrets.

**Solution — PKCE:** Instead of a static secret, the app generates a ONE-TIME cryptographic proof for each login:

1. Generate a random `code_verifier` (128 random characters)
2. Hash it with SHA-256 to create a `code_challenge`
3. Send the challenge to the auth server during the login redirect
4. Send the original verifier when exchanging the authorization code for tokens

The auth server verifies that `SHA256(verifier) == challenge`. This proves the same app that started login is completing it — without ever storing a long-lived secret.

```typescript
export function generateCodeVerifier(): string {
  const array = new Uint8Array(64);
  crypto.getRandomValues(array);         // Cryptographically secure random bytes
  return base64UrlEncode(array).slice(0, 128);
}

export async function generateCodeChallenge(verifier: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return base64UrlEncode(new Uint8Array(digest));
}
```

> **Analogy:** Like a sealed envelope. You put your answer (verifier) in the envelope, show the sealed envelope (challenge) to the examiner. Later, you open the envelope to prove you had the answer all along — but nobody could peek inside before you opened it.

#### Pattern 2: State Parameter (CSRF Protection)

```typescript
export function generateState(): string {
  return crypto.randomUUID();
}
```

Without a state parameter, an attacker could trick your browser into completing THEIR login flow (CSRF attack). The random state acts as a one-time token — when the callback arrives, we verify it matches what we stored.

> **Spring Equivalent:** Like a Spring Security CSRF token in a form — it ensures the response came from a request YOU initiated.

#### Pattern 3: JWT Decoding Without Verification

```typescript
export function decodeJwtPayload(token: string): JwtClaims {
  const parts = token.split('.');
  const payload = parts[1];
  const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
  return JSON.parse(decoded) as JwtClaims;
}
```

Why no signature verification? The token was just received from Keycloak over HTTPS. The BACKEND verifies signatures (using the public key). The frontend only decodes to extract user info (name, email, roles) for display.

> **Analogy:** Like reading the name on a sealed letter. You trust it because you just received it from the post office (Keycloak) — the recipient (backend API) will verify the seal when you present it.

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

When we receive a token that expires in 5 minutes, we schedule a refresh at the 4-minute mark (60 seconds before expiry). This ensures the user never experiences an expired token.

> **Spring Equivalent:** Like a `@Scheduled` task that renews a certificate before it expires — proactive renewal rather than reactive failure.

---

### The Login Flow (Step by Step)

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
- **In-memory storage** is the most secure option for tokens in a browser. Trade-off: session is lost on page refresh.
- **Factory functions** provide true encapsulation without class complexity.
- **The Observer pattern** bridges non-React code (auth module) and React components (AuthProvider).
- **Auto-refresh** ensures seamless UX — users don't get randomly logged out mid-session.
- **State parameter** prevents CSRF attacks on the OAuth callback.

---

### Glossary

| Term | Definition |
|------|-----------|
| **PKCE** | Proof Key for Code Exchange — replaces client secrets for public clients |
| **Code Verifier** | A random string used to prove identity during token exchange |
| **Code Challenge** | SHA-256 hash of the code verifier, sent during authorization |
| **Access Token** | A short-lived JWT that grants access to API endpoints |
| **Refresh Token** | A longer-lived token used to get new access tokens without re-login |
| **JWT** | JSON Web Token — a compact way to represent claims between parties |
| **CSRF** | Cross-Site Request Forgery — an attack where a malicious site tricks your browser |
| **XSS** | Cross-Site Scripting — an attack where malicious scripts are injected into trusted websites |
| **Closure** | A function that "remembers" variables from its enclosing scope |
| **Silent Refresh** | Refreshing the access token in the background without user interaction |
| **Singleton** | A single shared instance — `export const authModule = createAuthModule()` |

---


## AuthProvider React Context

We created a React Context provider that wraps the auth module and makes authentication state available to any component in the tree. This is the bridge between the non-React auth logic and the React component world.

> **Spring Equivalent:** Spring's dependency injection. Instead of every component importing and calling the auth module directly, they receive auth state and actions through the component hierarchy — like `@Autowired` injection.

**Key file:** `src/hooks/useAuth.tsx`

---

### React Concepts Used

#### 1. React Context API (`createContext` + `useContext`)

Context is React's built-in dependency injection system. It lets you pass data down through the component tree WITHOUT threading props through every intermediate component.

> **Spring Equivalent:** `ApplicationContext`. You register beans (values) at the top, and any component anywhere in the tree can inject (consume) them without explicit wiring through every layer.

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

Without Context, you'd have to pass `user`, `login`, `logout` as props through EVERY component — even ones that don't use them — just to reach a deeply nested component. This is called "prop drilling" and it's painful.

#### 2. The Provider Pattern

A component whose sole job is to supply context values to its children. It holds state and logic; its children consume the results.

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

> **Spring Equivalent:** A `@Configuration` class — it doesn't do anything visible, but it sets up dependencies that other components need.

#### 3. `useEffect` for Side Effects

`useEffect` lets you run code that doesn't directly produce UI — like subscribing to data sources, setting up timers, or making API calls.

> **Spring Equivalent:** `@PostConstruct` — code that runs AFTER the component is mounted (rendered to the DOM). The return function is like `@PreDestroy` — cleanup when the component is removed.

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

**The dependency array `[]`:** This tells React "only run this effect once" (on mount). If you listed `[someValue]`, it would re-run whenever `someValue` changes. Omitting the array entirely means it runs after EVERY render (usually a bug).

#### 4. `useCallback` for Memoized Functions

`useCallback` returns a cached version of a function that only changes if its dependencies change.

> **Spring Equivalent:** Like `@Cacheable` on a method — if the inputs haven't changed, return the same result.

```typescript
const login = useCallback(() => {
  authModule.login();
}, []);

const logout = useCallback(async () => {
  await authModule.logout();
}, []);
```

Why? In React, every re-render creates new function objects. If you pass these as props to child components, the children think they got new data and re-render unnecessarily. `useCallback` maintains a stable reference — same function, same object → children skip re-rendering.

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

Authentication is asynchronous — we don't know if the user is logged in until the auth module initializes. During that time, we show a loading indicator instead of briefly flashing the login page and then switching to the app.

> **Spring Equivalent:** A health check endpoint. The app isn't "ready" until all components have initialized. You don't route traffic until health returns 200.

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

If someone uses `useAuth()` outside of an `<AuthProvider>`, the context is `undefined`. Instead of a cryptic error later, we throw a clear message immediately.

> **Spring Equivalent:** A bean that requires `@Autowired` — if the dependency isn't in the context, Spring throws `NoSuchBeanDefinitionException` with a clear message rather than a null pointer later.

#### Pattern 2: Bridging Non-React Code to React

The auth module is plain TypeScript — it doesn't know about React. The AuthProvider bridges the gap:

1. **Subscribe** to auth module changes via `onStateChange`
2. **Mirror** those changes into React state via `useState`
3. **React re-renders** when the state changes
4. **Children** get updated values through context

This separation keeps the auth logic testable without React, and the React layer thin.

---

### Step-by-Step Walkthrough

1. App renders `<AuthProvider>` at the top of the component tree
2. `AuthProvider` initializes with `isLoading: true`
3. `useEffect` fires after first render — subscribes to changes and calls `initialize()`
4. Auth module resolves (e.g., no session → `isLoading: false, isAuthenticated: false`)
5. Subscription callback fires → `setAuthState` updates React state
6. React re-renders → loading indicator disappears
7. Children render (login page or main app, depending on `isAuthenticated`)
8. Later: user logs in → auth state changes → subscription fires → UI shows authenticated state

---

### Key Takeaways

- **Context is React's DI system.** Use it for cross-cutting concerns (auth, theme, locale) that many components need.
- **Providers hold logic; consumers are simple.** Keep complex state in the provider, expose a clean API via the hook.
- **`useEffect` with `[]` = run once on mount.** Return a cleanup function for subscriptions.
- **`useCallback` prevents unnecessary re-renders.** Wrap functions passed to children to maintain stable references.
- **Guard clauses in hooks** give clear error messages when components are used outside required providers.

---

### Glossary

| Term | Definition |
|------|-----------|
| **Context** | React's mechanism for passing data through the tree without prop drilling |
| **Provider** | A component that supplies a context value to its descendants |
| **Consumer** | A component that reads a context value (via `useContext`) |
| **Prop Drilling** | Passing props through many intermediate components that don't use them |
| **useEffect** | Hook for side effects (subscriptions, API calls, timers) after render |
| **useCallback** | Hook that returns a memoized function reference |
| **Memoization** | Caching a computed result so it's not recomputed unless inputs change |
| **Mount/Unmount** | When a component is added to (mount) or removed from (unmount) the DOM |
| **Side Effect** | Any operation outside the component's render output (network, DOM, subscriptions) |

---


## HTTP Client with Interceptors

We created an HTTP client using Axios that automatically attaches authentication tokens, adds correlation IDs for distributed tracing, normalizes all errors into a consistent shape, and handles 401 (unauthorized) responses with automatic token refresh.

> **Spring Equivalent:** Configuring a `RestTemplate` or `WebClient` with filters/interceptors for auth, logging, and error handling.

**Key files:** `src/lib/http-client.ts`, `src/types/api.ts`

---

### React Concepts Used

#### 1. Axios Interceptor Pattern (Request/Response Chain)

Axios interceptors are functions that run before every request is sent or after every response is received. They form a pipeline — each interceptor handles one cross-cutting concern.

> **Spring Equivalent:** Exactly like `ClientHttpRequestInterceptor` for `RestTemplate`, or Servlet Filters in a filter chain.

```typescript
// Request interceptor: runs BEFORE every HTTP request
httpClient.interceptors.request.use((config) => {
  config.headers.Authorization = `Bearer ${token}`;  // Add auth
  config.headers['X-Correlation-ID'] = uuidv4();     // Add tracing
  return config;
});

// Response interceptor: runs AFTER every HTTP response
httpClient.interceptors.response.use(
  (response) => response,                    // Success: pass through
  async (error) => { throw normalizeError(error); }  // Error: normalize
);
```

Interceptors keep cross-cutting concerns out of individual API calls. Each call just does `httpClient.get('/events')` — no boilerplate.

#### 2. Error Normalization

All possible error shapes (network errors, timeouts, backend error bodies) are transformed into one consistent interface.

> **Spring Equivalent:** A `@ControllerAdvice` that catches all exceptions and maps them to a standard error response body.

```typescript
export interface NormalizedError {
  status: number;           // HTTP status (0 if no response)
  type: string;            // Error category ('TIMEOUT', 'NETWORK_ERROR', etc.)
  message: string;         // Human-readable message
  correlationId: string | null;  // For debugging with backend logs
  fieldErrors: Array<{ field: string; message: string }>;  // For form validation
}
```

Without normalization, every API call needs branching logic to handle network errors vs. 400 vs. 500 vs. timeouts. With it, error handling is uniform everywhere.

#### 3. 401 Retry Logic with Silent Refresh

When the server returns 401 (token expired), the client automatically refreshes the token and retries the original request ONCE. If refresh fails, it redirects to login.

```typescript
async function handleUnauthorized(error, instance): Promise<unknown> {
  const originalRequest = error.config;

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

  originalRequest.headers.Authorization = `Bearer ${newToken}`;
  return instance(originalRequest);
}
```

Why single-attempt? Prevents infinite retry loops. If refresh fails, the session is truly expired.

> **Spring Equivalent:** A `RetryTemplate` with `maxAttempts(1)` — try once, fail gracefully.

#### 4. Pluggable Toast Handler (Dependency Inversion)

```typescript
let toastHandler: (toast: ToastPayload) => void = () => {};  // No-op default

export function setToastHandler(handler: (toast: ToastPayload) => void): void {
  toastHandler = handler;
}
```

The HTTP client is plain TypeScript — it doesn't know about React or the toast system. But it needs to show warnings (e.g., "Rate limited, retry after 30s"). Instead of importing the toast store directly (which creates a circular dependency), we let the toast system "plug itself in" at app startup.

> **Spring Equivalent:** `@Autowired` with `@Lazy` or `ObjectProvider` — the HTTP client declares it needs a toast handler (interface), and the toast system provides the implementation later.

#### 5. UUID Correlation IDs for Distributed Tracing

```typescript
config.headers['X-Correlation-ID'] = uuidv4();
```

Every HTTP request gets a unique ID. If an error occurs, this ID appears in the browser's toast notification, the backend's log files, and the API gateway's access logs.

When a user reports "I got an error," you can trace that exact request through all backend services using the correlation ID. Without it, debugging requires time-range guessing.

> **Spring Equivalent:** Spring Cloud Sleuth/Micrometer Tracing's trace IDs, but initiated by the frontend.

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

When the HTTP client makes requests to Keycloak's token endpoint (for refresh), it should NOT attach the Bearer token. Keycloak uses its own authentication (the refresh token in the request body). Attaching a Bearer token would confuse Keycloak.

---

### API Request Lifecycle (Step by Step)

1. Component calls `httpClient.get('/events')`
2. **Request interceptor** fires:
   - Gets current access token from auth module
   - Checks if URL is a Keycloak endpoint (skip auth if so)
   - Attaches `Authorization: Bearer <token>` header
   - Generates UUID and attaches `X-Correlation-ID` header
3. Axios sends the request to the dev proxy (`/api` → `localhost:7093`)
4. **Response arrives:**
   - **200 OK:** Passes through → component receives data
   - **401 Unauthorized:** `handleUnauthorized` fires → silent refresh → retry
   - **429 Rate Limited:** Toast warning displayed → error still thrown
   - **Any error:** `normalizeError` transforms it → consistent shape thrown

---

### Key Takeaways

- **Interceptors centralize cross-cutting concerns.** Auth, tracing, and error handling are configured once.
- **Error normalization** means one interface to handle, regardless of what went wrong.
- **Single-attempt retry** prevents infinite loops while providing seamless UX for expired tokens.
- **Dependency inversion** (pluggable toast handler) keeps modules decoupled.
- **Correlation IDs** are essential for debugging distributed systems — start them at the frontend.

---

### Glossary

| Term | Definition |
|------|-----------|
| **Interceptor** | A function that hooks into the request/response pipeline |
| **Axios** | A popular HTTP client library (like Apache HttpClient in Java) |
| **Correlation ID** | A unique identifier that follows a request through all services |
| **Error Normalization** | Transforming diverse error shapes into one consistent structure |
| **Dependency Inversion** | Depending on abstractions rather than concrete implementations |
| **Retry Logic** | Automatically retrying failed requests under specific conditions |
| **Rate Limiting** | Server-side throttling that returns 429 when too many requests arrive |
| **Bearer Token** | An access token in the Authorization header to authenticate API requests |

---


## Zustand UI Store

We implemented a global UI state store using Zustand — a lightweight state management library — to manage sidebar collapse, theme preference, toast notifications, and offline status. The store persists user preferences (sidebar, theme) to localStorage while keeping temporary state (toasts, offline) in memory only.

> **Spring Equivalent:** A singleton `@Service` with `@Scope("application")` that holds shared application state. Some fields are persisted to a database; others are in-memory only.

**Key file:** `src/stores/ui-store.ts`

---

### React Concepts Used

#### 1. Zustand — Lightweight State Management

Zustand creates a store (a single source of truth) that any component can subscribe to. When the store changes, only subscribed components re-render.

> **Spring Equivalent:** A shared in-memory `ConcurrentHashMap` that notifies listeners when specific keys change. Or like a simplified event bus — components subscribe to the slices of state they care about.

**Zustand vs Redux:**
| Aspect | Redux | Zustand |
|--------|-------|---------|
| Boilerplate | Actions, reducers, action creators, dispatch | Just functions in the store |
| Bundle size | ~7KB | ~1KB |
| Setup | Provider required at app root | No provider needed |
| Learning curve | Steep | Minimal (it's just a hook) |

```typescript
import { create } from 'zustand';

export const useUIStore = create<UIState>()((set) => ({
  sidebarCollapsed: false,
  toggleSidebar: () => set((state) => ({ sidebarCollapsed: !state.sidebarCollapsed })),
  theme: 'system',
  setTheme: (theme) => set({ theme }),
}));
```

Usage in components:
```typescript
function Sidebar() {
  const collapsed = useUIStore((s) => s.sidebarCollapsed);  // Only re-renders when THIS changes
  const toggle = useUIStore((s) => s.toggleSidebar);
  // ...
}
```

#### 2. Persist Middleware for localStorage

Zustand middleware that automatically saves selected state to localStorage and restores it on app load.

> **Spring Equivalent:** `@ConfigurationProperties` with a backing file — preferences are saved and restored between sessions.

```typescript
persist(
  (set) => ({ ... }),
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

Why `partialize`? We don't want to persist toasts (they're temporary) or offline status (runtime-only). Only user preferences should survive a page refresh.

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

In some browsers (Safari private mode, or when storage is full), localStorage throws exceptions. Without this wrapper, the entire app would crash. With it, the app continues — just without persistence.

> **Spring Equivalent:** Wrapping a cache (Redis) call in a try/catch. If the cache is down, the app still works, just without caching.

#### 4. Selector Pattern for Efficient Re-renders

```typescript
// ✅ Good: Component only re-renders when sidebarCollapsed changes
const collapsed = useUIStore((s) => s.sidebarCollapsed);

// ❌ Bad: Component re-renders when ANY store field changes
const store = useUIStore();
```

Zustand uses reference equality (`===`) to determine if a component should re-render. The selector returns just the slice of state the component needs.

> **Spring Equivalent:** Like a database query with `SELECT name FROM users` instead of `SELECT * FROM users`. You only subscribe to what you need.

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

localStorage data can be corrupted (manual editing, version mismatch, browser bugs). The merge function validates every field before trusting it — falling back to safe defaults if anything is unexpected.

> **Spring Equivalent:** Defensive JSON parsing in a REST controller — validate input before using it, even if it "should" always be correct.

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

Without a cap, a runaway error loop could produce thousands of toasts, consuming memory and crashing the browser. The cap (50) provides a safety valve.

> **Spring Equivalent:** A bounded `BlockingQueue` in Java — oldest entries are dropped when capacity is reached.

---

### Step-by-Step Walkthrough

1. App starts → Zustand creates the store with default values
2. Persist middleware reads `outreach-ui-state` from localStorage
3. `merge` function validates persisted data → restores valid preferences
4. Components call `useUIStore((s) => s.theme)` → get current theme
5. User toggles sidebar → `toggleSidebar()` updates store
6. Persist middleware saves `{ sidebarCollapsed, theme }` to localStorage
7. All components subscribed to `sidebarCollapsed` re-render
8. Components subscribed to OTHER fields do NOT re-render

---

### Key Takeaways

- **Zustand is minimal by design.** No providers, no reducers, no action types. Just a store and selectors.
- **Selectors prevent wasted re-renders.** Subscribe to the smallest slice you need.
- **Partialize** controls what gets persisted. Keep temporary state out of localStorage.
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
| **Middleware** | Code that wraps store behavior to add features (persistence, logging) |
| **Persist** | Zustand middleware that saves/restores state to/from localStorage |
| **Partialize** | Selecting which parts of state to persist |
| **Graceful Degradation** | Continuing to work (with reduced features) when a subsystem fails |
| **Rehydration** | Restoring previously persisted state when the app loads |
| **Reference Equality** | Comparing values by memory address (`===`) rather than deep comparison |

---


## Toast Notification System

We built a toast notification system with two parts: a custom hook (`useToast`) providing a convenience API for showing notifications, and visual components (`ToastContainer` + `ToastItem`) that render and auto-dismiss them.

> **Spring Equivalent:** An event bus for notifications. Producers emit events, consumers render them. Like `ApplicationEventPublisher` on the UI side.

**Key files:** `src/hooks/useToast.ts`, `src/components/feedback/ToastContainer.tsx`

---

### React Concepts Used

#### 1. Custom Hooks Wrapping Store Actions

`useToast` is a custom hook that provides a cleaner API on top of the raw Zustand store. Instead of `useUIStore((s) => s.addToast)` everywhere, components call `toast.success("Saved!")`.

> **Spring Equivalent:** A `@Service` layer that wraps `@Repository` calls with business logic. The repository (store) is generic; the service (hook) provides domain-specific convenience methods.

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

Why separate from the store? Hooks can use React features (`useCallback`). Stores are plain TypeScript. Each layer stays focused.

#### 2. Component Composition (ToastContainer + ToastItem)

Instead of one monolithic component, we split into:
- `ToastContainer` — manages the list, wires the HTTP client handler, limits visible toasts
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

> **Spring Equivalent:** Like MVC — the controller (Container) manages data flow; the view (Item) handles presentation. Single responsibility.

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

What this does:
1. When a non-error toast mounts, start a 5-second timer
2. When the timer fires, dismiss the toast
3. If the toast is dismissed manually (or component unmounts), CANCEL the timer

Without the cleanup function, if the user dismisses quickly, the timer would fire after 5 seconds and try to dismiss an already-gone toast.

> **Spring Equivalent:** Cancelling a `ScheduledFuture` in Java's `ScheduledExecutorService` — if the task is no longer needed, cancel it to prevent resource leaks.

#### 4. CSS Modules for Scoped Styling

```typescript
import styles from './ToastContainer.module.css';

// Usage:
<div className={styles.container}>
<div className={`${styles.toast} ${styles[toast.severity]}`}>
```

CSS Modules automatically scope class names to the component. The class `.container` in `ToastContainer.module.css` becomes something like `.ToastContainer_container_a1b2c` in the output — guaranteed unique.

This prevents style collisions. Without scoping, a `.container` class in one component could accidentally style elements in another.

> **Spring Equivalent:** Java packages — `com.company.auth.User` and `com.company.billing.User` don't collide because they're in different namespaces. CSS Modules give CSS the same namespacing.

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
| `aria-label="Notifications"` | Names the region for navigation |
| `aria-live="polite"` | Screen readers announce new toasts without interrupting |
| `role="alert"` | Each toast is announced as an alert |
| `aria-atomic="true"` | The entire toast is read as one unit |
| `aria-label="Dismiss"` | The × button has a meaningful label |

Screen reader users can't see toast popups. ARIA attributes ensure they hear notifications and can interact with dismiss buttons.

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

On first render, the ToastContainer registers itself as the HTTP client's toast handler. Now when the HTTP client encounters a 429 (rate limit), it calls `toastHandler(...)` which flows through the Zustand store into the visible toast.

This is the dependency inversion from the HTTP client section being fulfilled.

#### Pattern: `useRef` for Timer References

```typescript
const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
```

> **Spring Equivalent:** An instance field in a Java class — it persists for the object's lifetime. Unlike `useState`, changing it does NOT trigger a re-render. Perfect for timer IDs, DOM references, or any "instance variable."

---

### Step-by-Step Walkthrough

1. App renders `<ToastContainer />` — it mounts and wires the HTTP client handler
2. Something triggers a toast (user action, API error, rate limit)
3. `addToast` is called → Zustand store adds the toast with a unique ID
4. `ToastContainer` re-renders → shows up to 5 visible toasts
5. `ToastItem` mounts → starts a 5-second auto-dismiss timer (unless it's an error)
6. Timer fires → toast removed from store → disappears
7. Or: User clicks × → toast removed → timer cleanup runs → disappears

---

### Key Takeaways

- **Custom hooks** provide clean APIs on top of raw stores. They're the "Service layer" of React.
- **Component composition** (Container + Item) keeps each component focused and testable.
- **Cleanup functions in `useEffect`** are essential for timers, subscriptions, and event listeners. Always clean up.
- **CSS Modules** give you scoped styling without runtime overhead.
- **ARIA attributes** are not optional — they make your app usable for users with disabilities.
- **`useRef`** stores mutable values that persist across renders without triggering re-renders.

---

### Glossary

| Term | Definition |
|------|-----------|
| **Toast** | A brief notification that appears temporarily |
| **CSS Modules** | Build-time CSS scoping that generates unique class names per component |
| **ARIA** | Accessible Rich Internet Applications — attributes for assistive technologies |
| **aria-live** | Tells screen readers to announce dynamic content changes |
| **useRef** | Hook storing a mutable value that persists without causing re-renders |
| **Component Composition** | Building complex UIs from small, focused components |
| **Custom Hook** | A function starting with `use` that encapsulates reusable stateful logic |
| **Cleanup Function** | Function returned by useEffect, called on unmount or before re-run |

---


## QueryClient and Query Key Factory

We configured TanStack Query (React Query) as the server-state management layer and created a query key factory for consistent, hierarchical cache key management.

> **Spring Equivalent:** Configuring a caching layer (like Caffeine or Redis) with structured cache key patterns and time-based eviction policies. TanStack Query is essentially Spring Cache + RestTemplate combined.

**Key files:** `src/lib/query-client.ts`, `src/lib/query-keys.ts`

---

### React Concepts Used

#### 1. TanStack Query Client Configuration

TanStack Query manages "server state" — data that lives on the server and is fetched, cached, synchronized, and updated in the background. The `QueryClient` is the central configuration.

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

> **Spring Equivalent:**
> - `staleTime` → cache TTL (time-to-live)
> - `gcTime` → cache eviction timeout
> - `retry` → Spring Retry's `maxAttempts`
> - `refetchOnWindowFocus` → cache invalidation trigger

**Why server state is different from UI state:**

| Aspect | UI State (Zustand) | Server State (TanStack Query) |
|--------|-------------------|-------------------------------|
| Owned by | The client | The server |
| Source of truth | In-memory store | Backend database |
| Stale? | Never (you set it, it's current) | Always potentially (someone else may have changed it) |
| Examples | Sidebar collapsed, theme | Events list, user profile, feedback data |

#### 2. Stale-While-Revalidate Caching Strategy

When data is "stale" (older than `staleTime`), TanStack Query immediately returns the cached version (fast!) AND fetches fresh data in the background. When fresh data arrives, the UI updates seamlessly.

The timeline:
```
User navigates to /events:
  t=0s  → No cache → loading spinner → fetch from server → display
  t=10s → User navigates away
  t=20s → User returns (within 30s staleTime):
          → Instant display from cache (no spinner!) → data still "fresh"
  t=45s → User returns (past 30s staleTime):
          → Instant display from cache → ALSO refetches in background → UI updates if changed
  t=6m  → User returns (past 5m gcTime):
          → Cache garbage-collected → loading spinner → fresh fetch
```

> **Spring Equivalent:** Like a CDN with `max-age` header. Serve stale content instantly, revalidate in the background. Users get speed AND freshness.

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
};
```

What this creates:
```
queryKeys.events.all            → ['events']
queryKeys.events.lists()        → ['events', 'list']
queryKeys.events.list({page:1}) → ['events', 'list', {page:1}]
queryKeys.events.details()      → ['events', 'detail']
queryKeys.events.detail('abc')  → ['events', 'detail', 'abc']
```

Query keys are the "cache keys" for TanStack Query. Getting them wrong means stale data or invalidation failures. A factory ensures consistency — you never manually type a key and risk a typo.

> **Spring Equivalent:** Like `@Cacheable(key = "'events:list:' + #params.hashCode()")` — structured, consistent cache keys.

#### 4. Hierarchical Cache Invalidation

The power of hierarchical keys:

```typescript
// After creating a new event:
queryClient.invalidateQueries({ queryKey: queryKeys.events.all });
// This invalidates ALL queries starting with ['events']:
//   ['events', 'list']             ← matches
//   ['events', 'list', {page:1}]   ← matches
//   ['events', 'detail', 'abc']    ← matches
// But NOT:
//   ['volunteers']                 ← doesn't match

// After updating a specific event:
queryClient.invalidateQueries({ queryKey: queryKeys.events.detail('abc') });
// Only invalidates: ['events', 'detail', 'abc']
```

Think of it like a directory structure:
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

After a mutation (create/update/delete), you tell the cache "this data might be stale." Hierarchical keys let you invalidate at the right level — not too broad (wasteful refetches) and not too narrow (stale data persists).

---

### Code Patterns Explained

#### Pattern: `as const` for Type-Safe Query Keys

```typescript
all: ['events'] as const,
lists: () => [...queryKeys.events.all, 'list'] as const,
```

Without `as const`, TypeScript infers `string[]` (any strings in any order). With it, TypeScript infers the exact tuple type `readonly ['events', 'list']`. This means typos are caught at compile time, autocomplete works, and key hierarchies are enforced.

> **Spring Equivalent:** Using an `enum` instead of `String` — compile-time validation rather than runtime "oops, wrong key."

#### Pattern: Parameter Inclusion in Keys

```typescript
list: (params: EventListParams) => [...queryKeys.events.lists(), params] as const,
```

The same endpoint with different parameters returns different data. `GET /events?page=1` and `GET /events?page=2` are different cache entries. Including params in the key ensures they're cached separately.

> **Spring Equivalent:** A composite cache key: `@Cacheable(key = "'events:list:' + #params.hashCode()")`

---

### Step-by-Step Walkthrough

1. App wraps root in `<QueryClientProvider client={queryClient}>`
2. Component calls `useQuery({ queryKey: queryKeys.events.list({page:1}), queryFn: ... })`
3. TanStack Query checks: is `['events', 'list', {page:1}]` in cache?
4. **Cache miss:** Shows loading state → fetches → caches → displays
5. **Cache hit (fresh):** Returns cached data immediately → no fetch
6. **Cache hit (stale):** Returns cached data → fetches in background → updates if different
7. After mutation: `queryClient.invalidateQueries({ queryKey: queryKeys.events.lists() })`
8. All list queries become stale → active ones refetch automatically

---

### Key Takeaways

- **TanStack Query manages server state.** Use Zustand for UI state, TanStack Query for server data. Don't mix them.
- **Stale-while-revalidate** gives both speed (cached data) and freshness (background refetch).
- **Query key factories** prevent key bugs and enable hierarchical invalidation.
- **`as const`** gives type-safe, autocomplete-friendly keys at zero runtime cost.
- **Granular invalidation** is why key hierarchy matters — invalidate what changed, not everything.
- **`gcTime` vs `staleTime`:** Stale data is still served from cache (with background refresh). GC'd data is gone (shows loading spinner again).

---

### Glossary

| Term | Definition |
|------|-----------|
| **TanStack Query** | A library for fetching, caching, and synchronizing server state (formerly React Query) |
| **QueryClient** | Central config object controlling cache behavior and defaults |
| **Query Key** | An array that uniquely identifies a cached query — like a cache key in a HashMap |
| **staleTime** | Duration after which cached data is eligible for background refetch |
| **gcTime** | Duration after which UNUSED cached data is freed from memory |
| **Stale-While-Revalidate** | Serve stale data instantly, refresh in background, update if different |
| **Invalidation** | Marking cached data as stale, triggering a refetch |
| **Query Key Factory** | A structured object that produces consistent, hierarchical query keys |
| **Hierarchical Keys** | Keys structured as nested arrays for prefix-based invalidation |
| **as const** | TypeScript assertion that narrows a value to its literal type |
| **Server State** | Data owned by the server — always potentially stale |
| **Background Refetch** | Fetching fresh data without showing a loading spinner |

---


## RBAC Components and Route Guards

We implemented Role-Based Access Control (RBAC) for the frontend — the mechanism that restricts what users can see and do based on their assigned roles. This includes a `usePermission` hook, a `ProtectedRoute` component, a `RequireRole` component, and sidebar navigation filtering.

> **Spring Equivalent:** Spring Security's `@PreAuthorize` and `hasRole()` annotations, but applied to UI rendering rather than HTTP endpoint access.

**Key files:** `src/hooks/usePermission.ts`, `src/components/layout/ProtectedRoute.tsx`, `src/components/layout/RequireRole.tsx`, `src/components/layout/ForbiddenPage.tsx`, `src/lib/navigation-filter.ts`

---

### What Is RBAC in a Frontend Context?

**Backend RBAC** (what you're used to in Spring Security): The server checks roles and rejects unauthorized requests with 403. This is the source of truth — even if the frontend is bypassed, the backend enforces access.

**Frontend RBAC**: The UI hides or disables elements the user isn't allowed to interact with. This is a UX optimization — you don't show a button the user can't click.

**Critical principle:** Frontend RBAC is cosmetic, NOT security. It improves UX but NEVER replaces backend authorization. An attacker can modify JavaScript, skip route guards, or call APIs directly. The backend must always be the final gatekeeper.

> **Analogy:** Like hiding the "Delete Database" button from non-admin users in a management console. Even if someone hacks the UI to show the button, the backend rejects the API call. Frontend hiding is just good UX.

---

### React Concepts Used

#### 1. Custom Hooks for Cross-Cutting Logic (`usePermission`)

A custom hook that encapsulates role-checking logic in one place. Any component can ask "does the current user have permission?" without duplicating the check.

> **Spring Equivalent:** A utility class like `SecurityUtils.hasRole("ADMIN")` that any service can call — except hooks are aware of React state and trigger re-renders when auth changes.

```typescript
export function usePermission(requiredRoles: string[]): UsePermissionResult {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return { hasPermission: false, isLoading: true };
  }

  if (requiredRoles.length === 0) {
    return { hasPermission: true, isLoading: false };
  }

  if (!user || !user.roles || user.roles.length === 0) {
    return { hasPermission: false, isLoading: false };
  }

  const hasPermission = requiredRoles.some((role) => user.roles.includes(role));
  return { hasPermission, isLoading: false };
}
```

Key behaviors:
- Empty `requiredRoles` → always granted (public route/element)
- Multi-role users → union of permissions (any matching role is sufficient)
- Loading state → returns `false` with `isLoading: true` (prevents flash of wrong content)

#### 2. Guard Components (Higher-Order Pattern)

Components that wrap other components and conditionally render them based on authorization. This is declarative — you describe WHAT access is required, not HOW to check it.

> **Spring Equivalent:** Thymeleaf's `<div sec:authorize="hasRole('ADMIN')">Admin content</div>`

In React:
```tsx
<RequireRole roles={['ROLE_ADMIN']}>
  <AdminPanel />
</RequireRole>
```

---

### Code Patterns Explained

#### Pattern 1: ProtectedRoute — Page-Level Authorization

`ProtectedRoute` guards entire routes/pages. Three possible outcomes:

| State | Rendered Output |
|-------|----------------|
| Loading (token not decoded yet) | Loading spinner |
| Authorized (user has required role) | Children (the page content) |
| Unauthorized (user lacks required role) | 403 Forbidden page |

```tsx
export function ProtectedRoute({ requiredRoles, children }: ProtectedRouteProps) {
  const { hasPermission, isLoading } = usePermission(requiredRoles);

  if (isLoading) {
    return <div role="status" aria-label="Verifying permissions">Verifying access...</div>;
  }

  if (!hasPermission) {
    return <ForbiddenPage />;
  }

  return <>{children}</>;
}
```

Usage:
```tsx
<ProtectedRoute requiredRoles={['ROLE_ADMIN']}>
  <AdminDashboard />
</ProtectedRoute>
```

Why show a 403 page instead of redirecting to login? The user IS authenticated. They just don't have the right role. Redirecting to login would be confusing — they'd log in again and still be denied.

#### Pattern 2: RequireRole — Element-Level Authorization

`RequireRole` is lighter — it renders children or nothing. No error page. Use it for conditionally showing buttons, menu items, or sections within a page.

```tsx
export function RequireRole({ roles, children }: RequireRoleProps): ReactNode {
  const { hasPermission, isLoading } = usePermission(roles);

  if (isLoading || !hasPermission) {
    return null;
  }

  return <>{children}</>;
}
```

Usage:
```tsx
<RequireRole roles={['ROLE_ADMIN']}>
  <button onClick={deleteUser}>Delete User</button>
</RequireRole>
```

Why render null during loading? To prevent a "flash of content" — briefly showing the Delete button, then hiding it once roles resolve.

#### Pattern 3: How ProtectedRoute and RequireRole Differ

| Concern | ProtectedRoute | RequireRole |
|---------|---------------|-------------|
| **Scope** | Entire page/route | Individual UI elements |
| **On unauthorized** | Shows 403 page with message | Renders nothing |
| **On loading** | Shows "Verifying access..." | Renders nothing |
| **Typical usage** | Wrapping route components | Wrapping buttons, menu items |
| **User sees** | A clear error message | The element simply doesn't exist |

When to use which:
- User would be confused by a blank page → use `ProtectedRoute` (shows explanation)
- Element should just disappear for unauthorized users → use `RequireRole` (silent hide)

#### Pattern 4: Sidebar Navigation Filtering

Rather than checking roles on every sidebar link in JSX, we filter the navigation data BEFORE rendering. Transform the data, then render it.

```typescript
export function filterNavigationByRoles(
  groups: NavigationGroup[],
  userRoles: string[]
): NavigationGroup[] {
  return groups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => {
        if (item.requiredRoles.length === 0) return true;
        return item.requiredRoles.some((role) => userRoles.includes(role));
      }),
    }))
    .filter((group) => group.items.length > 0);
}
```

How roles map to navigation visibility:

| Role | Sees |
|------|------|
| ROLE_ADMIN | All navigation items |
| ROLE_PMO | Dashboard, Events, Volunteers, Feedback, Reports |
| ROLE_POC | Dashboard, Events, Volunteers, Feedback |

Items with `requiredRoles: []` (empty array) are visible to ALL authenticated users. Multi-role users get the union — they see everything either role grants.

The filtering uses `useMemo` — it only recomputes when `userRoles` changes (rare, typically only on login):

```tsx
const filteredNavigationGroups = useMemo(
  () => filterNavigationByRoles(navigationGroups, userRoles),
  [userRoles]
);
```

> **Spring Equivalent:** `useMemo` is like `@Cacheable` — the value is only recomputed when inputs change.

#### Pattern 5: Loading State While Roles Resolve

When the page first loads, the JWT hasn't been decoded yet. During this brief window, we don't know the user's roles. The layout shows a full-screen loading indicator:

```tsx
if (isLoading) {
  return (
    <div role="status" aria-label="Loading navigation">
      <span>Loading...</span>
    </div>
  );
}
```

Without this, you'd see a flash of either "all nav items" (security issue) or "no nav items" (confusing) before the correct filtered set appears.

> **Spring Equivalent:** A Security filter chain that hasn't resolved the `Authentication` object yet. You don't serve ANY page until you know who the user is.

---

### The RBAC Architecture at a Glance

```
┌─────────────────────────────────────────────────────────────────┐
│                    LayoutShell                                     │
│   ┌──────────────────────────────────────────────────────────┐  │
│   │  isLoading?  →  Full-screen loading indicator             │  │
│   └──────────────────────────────────────────────────────────┘  │
│                          │                                        │
│                          ▼                                        │
│   ┌───────────────┐  ┌──────────────────────────────────────┐  │
│   │   Sidebar      │  │  Main Content Area                    │  │
│   │                │  │                                        │  │
│   │  Filtered by   │  │  ProtectedRoute (page-level guard)    │  │
│   │  user roles    │  │    ├─ Loading → spinner               │  │
│   │  (navigation-  │  │    ├─ Unauthorized → ForbiddenPage    │  │
│   │   filter.ts)   │  │    └─ Authorized → children           │  │
│   │                │  │                                        │  │
│   │  Empty roles   │  │  RequireRole (element-level guard)    │  │
│   │  = show item   │  │    ├─ Loading → null                  │  │
│   │                │  │    ├─ Unauthorized → null              │  │
│   │  Multi-role    │  │    └─ Authorized → children           │  │
│   │  = union       │  │                                        │  │
│   └───────────────┘  └──────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
              ┌─────────────────────┐
              │    usePermission     │
              │  (single source of   │
              │   role-check logic)  │
              │                     │
              │  Uses useAuth() to   │
              │  get user.roles     │
              └─────────────────────┘
```

---

### Key Takeaways

- **Frontend RBAC is for UX, not security.** Always enforce authorization on the backend.
- **`usePermission` centralizes all role-checking logic.** One hook, used by both guard components.
- **`ProtectedRoute` is for pages** (shows 403). **`RequireRole` is for elements** (renders nothing).
- **Navigation filtering is data-driven** — transform the data before rendering, rather than conditionally rendering in JSX.
- **Loading states prevent security flashes** — never show authorized-only content before you know the user's roles.
- **Multi-role is additive** (union, not intersection) — more roles expands access.
- **`useMemo` for derived data** — recompute filtered navigation only when roles change.

---

### Glossary

| Term | Definition |
|------|-----------|
| **RBAC** | Role-Based Access Control — restricting access based on user roles |
| **Route Guard** | A component that prevents navigation to a route unless conditions are met |
| **403 Forbidden** | HTTP status: authenticated but not authorized |
| **Union of Permissions** | Multi-role users get all permissions from ALL their roles combined |
| **Conditional Rendering** | Rendering different output based on conditions |
| **useMemo** | React hook that caches a computed value, recomputing only when dependencies change |
| **Flash of Content** | Brief unwanted appearance of content before correct state is determined |
| **Guard Component** | A wrapper that conditionally renders children based on access rules |
| **Navigation Filter** | Logic that removes menu items the user isn't authorized to see |
