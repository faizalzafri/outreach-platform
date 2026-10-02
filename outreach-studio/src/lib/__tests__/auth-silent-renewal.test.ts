import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createAuthModule, SILENT_CALLBACK_MESSAGE } from '../auth';

function fakeJwt(claims: Record<string, unknown>): string {
  const encode = (value: object) =>
    btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${encode({ alg: 'none' })}.${encode(claims)}.sig`;
}

const freshToken = fakeJwt({
  sub: 'admin',
  tenant_id: '00000000-0000-0000-0000-000000000001',
  exp: Math.floor(Date.now() / 1000) + 300,
});

/** Answers the hidden iframe as the auth server + silent-callback.html would. */
function answerIframe(buildSearch: (state: string) => string): void {
  const observer = new MutationObserver(() => {
    const iframe = document.querySelector('iframe');
    if (!iframe) return;
    observer.disconnect();
    const state = new URL(iframe.src).searchParams.get('state') ?? '';
    window.dispatchEvent(new MessageEvent('message', {
      origin: window.location.origin,
      data: { type: SILENT_CALLBACK_MESSAGE, search: buildSearch(state) },
    }));
  });
  observer.observe(document.body, { childList: true });
}

describe('Auth Module - silent renewal without a refresh token', () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    document.body.innerHTML = '';
  });

  /** Logs in through the normal callback; the auth server issues no refresh token. */
  async function signedInModule() {
    sessionStorage.setItem('outreach_pkce_verifier', 'login-verifier');
    sessionStorage.setItem('outreach_pkce_state', 'login-state');
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ access_token: freshToken }), { status: 200 }));
    const auth = createAuthModule();
    await auth.handleCallback('login-code', 'login-state');
    fetchMock.mockClear();
    return auth;
  }

  it('re-authorizes in a hidden iframe and exchanges the returned code', async () => {
    const auth = await signedInModule();
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ access_token: freshToken }), { status: 200 }));
    answerIframe((state) => `?code=silent-code&state=${state}`);

    const token = await auth.silentRefresh();

    expect(token).toBe(freshToken);
    expect(auth.getState().isAuthenticated).toBe(true);
    const body = new URLSearchParams(fetchMock.mock.calls[0][1].body as string);
    expect(body.get('grant_type')).toBe('authorization_code');
    expect(body.get('code')).toBe('silent-code');
    expect(body.get('redirect_uri')).toBe(`${window.location.origin}/silent-callback.html`);
    expect(body.get('code_verifier')).toBeTruthy();
    expect(document.querySelector('iframe')).toBeNull();
  });

  it('ends the session when the iframe response does not match the request', async () => {
    const auth = await signedInModule();
    answerIframe(() => '?code=forged&state=someone-elses-state');

    const token = await auth.silentRefresh();

    expect(token).toBeNull();
    expect(auth.getState().isAuthenticated).toBe(false);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('goes straight to login when there is no session to renew', async () => {
    const auth = createAuthModule();

    expect(await auth.silentRefresh()).toBeNull();
    expect(document.querySelector('iframe')).toBeNull();
  });
});
