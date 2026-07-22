import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import keycloak from '../keycloak';
import { logoutUser } from '../services/authService';
import { clearAuthStorage, getStoredFcmToken } from '../services/authStorage';
import { AuthContext } from './authContextValue';

function mapKeycloakUser(tokenParsed) {
  if (!tokenParsed) return null;

  return {
    id: tokenParsed.sub,
    // "fullName" and "phone" are custom User Profile attributes exposed via
    // dedicated protocol mappers (see Keycloak setup guide). Fall back to
    // the standard OIDC claims in case those mappers aren't configured yet
    // (e.g. accounts created through Google, which populate name/given_name).
    fullName:
      tokenParsed.fullName ||
      tokenParsed.name ||
      [tokenParsed.given_name, tokenParsed.family_name].filter(Boolean).join(' '),
    email: tokenParsed.email,
    emailVerified: Boolean(tokenParsed.email_verified),
    phone: tokenParsed.phone || tokenParsed.phone_number || '',
    roles: tokenParsed.realm_access?.roles || [],
  };
}

export function AuthProvider({ children }) {
  const [initialized, setInitialized] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [user, setUser] = useState(null);
  const initStarted = useRef(false);

  useEffect(() => {
    if (initStarted.current) return;
    initStarted.current = true;

    keycloak.onAuthSuccess = () => {
      setAuthenticated(true);
      setUser(mapKeycloakUser(keycloak.tokenParsed));
    };

    keycloak.onAuthRefreshSuccess = () => {
      setUser(mapKeycloakUser(keycloak.tokenParsed));
    };

    keycloak.onAuthRefreshError = () => {
      setAuthenticated(false);
      setUser(null);
    };

    keycloak.onAuthLogout = () => {
      setAuthenticated(false);
      setUser(null);
    };

    keycloak.onTokenExpired = () => {
      keycloak.updateToken(30).catch(() => {
        setAuthenticated(false);
        setUser(null);
      });
    };

    keycloak
      .init({
        onLoad: 'check-sso',
        pkceMethod: 'S256',
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
        checkLoginIframe: false,
      })
      .then((isAuthenticated) => {
        setAuthenticated(isAuthenticated);
        setUser(isAuthenticated ? mapKeycloakUser(keycloak.tokenParsed) : null);
      })
      .catch(() => {
        setAuthenticated(false);
        setUser(null);
      })
      .finally(() => setInitialized(true));
  }, []);

  // Redirects to Keycloak's hosted login page.
  const login = useCallback((options) => keycloak.login(options), []);

  // Redirects to Keycloak's hosted registration page.
  const register = useCallback((options) => keycloak.register(options), []);

  // Skips straight to the Google identity provider (still via Keycloak).
  const googleLogin = useCallback((options) => keycloak.login({ idpHint: 'google', ...options }), []);

  const logout = useCallback(async () => {
    const fcmToken = getStoredFcmToken();
    try {
      await logoutUser(fcmToken);
    } catch {
      // Best-effort: still proceed to clear local state and end the Keycloak session.
    } finally {
      clearAuthStorage();
      await keycloak.logout({ redirectUri: window.location.origin });
    }
  }, []);

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: authenticated,
      initialized,
      keycloak,
      login,
      register,
      googleLogin,
      logout,
    }),
    [authenticated, googleLogin, initialized, login, logout, register, user],
  );

  if (!initialized) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50">
        <span className="h-8 w-8 animate-spin rounded-full border-2 border-slate-200 border-t-blue-700" />
      </div>
    );
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
