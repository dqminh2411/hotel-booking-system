import { useCallback, useEffect, useMemo, useState } from 'react';
import { loginUser, loginWithGoogle } from '../services/authService';
import {
  clearAuthStorage,
  getAccessToken,
  getStoredUser,
  setAccessToken,
  setStoredUser,
} from '../services/authStorage';
import { AuthContext } from './authContextValue';

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => (getAccessToken() ? getStoredUser() : null));

  const applyAuthResponse = useCallback((authResponse) => {
    setAccessToken(authResponse.accessToken);
    setStoredUser(authResponse.user);
    setUser(authResponse.user);
    return authResponse.user;
  }, []);

  const login = useCallback(
    async (credentials) => applyAuthResponse(await loginUser(credentials)),
    [applyAuthResponse],
  );

  const googleLogin = useCallback(
    async (idToken) => applyAuthResponse(await loginWithGoogle(idToken)),
    [applyAuthResponse],
  );

  const logout = useCallback(() => {
    clearAuthStorage();
    setUser(null);
  }, []);

  useEffect(() => {
    window.addEventListener('hotelhub:auth-expired', logout);
    return () => window.removeEventListener('hotelhub:auth-expired', logout);
  }, [logout]);

  const value = useMemo(
    () => ({ user, isAuthenticated: Boolean(getAccessToken()), login, googleLogin, logout }),
    [googleLogin, login, logout, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
