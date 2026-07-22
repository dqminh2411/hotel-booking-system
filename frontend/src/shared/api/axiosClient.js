import axios from 'axios';
import keycloak from '../../features/auth/keycloak';

const baseURL = (
  import.meta.env.VITE_API_BASE_URL ||
  import.meta.env.VITE_API_URL ||
  'http://localhost:8080'
).replace(/\/$/, '');

const axiosClient = axios.create({
  baseURL,
  timeout: 15000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
});

// Attach a fresh access token to every request. keycloak.updateToken()
// resolves immediately if the current token still has >30s of validity,
// and otherwise transparently refreshes it first.
axiosClient.interceptors.request.use(async (config) => {
  if (keycloak.authenticated) {
    try {
      await keycloak.updateToken(30);
    } catch {
      // Refresh failed (e.g. refresh token expired) — let the request go
      // out as-is; the 401 handler below will send the user to re-login.
    }
    if (keycloak.token) {
      config.headers.Authorization = `Bearer ${keycloak.token}`;
    }
  }
  return config;
});

axiosClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const request = error.config;
    const isUnauthorized = error.response?.status === 401;

    if (!isUnauthorized || request?._retry || !keycloak.authenticated) {
      return Promise.reject(error);
    }

    request._retry = true;
    try {
      await keycloak.updateToken(-1); // force a refresh regardless of current expiry
      request.headers.Authorization = `Bearer ${keycloak.token}`;
      return axiosClient(request);
    } catch (refreshError) {
      keycloak.login();
      return Promise.reject(refreshError);
    }
  },
);

export default axiosClient;
