import axios from 'axios';
import {
  clearAuthStorage,
  getAccessToken,
  setAccessToken,
} from '../../features/auth/services/authStorage';

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

const refreshClient = axios.create({
  baseURL,
  timeout: 15000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
});

let refreshRequest = null;

axiosClient.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axiosClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const request = error.config;
    const isUnauthorized = error.response?.status === 401;
    const isAuthRequest = /\/(login|refresh-token)$/.test(request?.url || '');

    if (!isUnauthorized || request?._retry || isAuthRequest || !getAccessToken()) {
      return Promise.reject(error);
    }

    request._retry = true;
    try {
      refreshRequest ??= refreshClient
        .post('/api/users/refresh-token')
        .then(({ data }) => data.accessToken)
        .finally(() => {
          refreshRequest = null;
        });

      const accessToken = await refreshRequest;
      setAccessToken(accessToken);
      request.headers.Authorization = `Bearer ${accessToken}`;
      return axiosClient(request);
    } catch (refreshError) {
      clearAuthStorage();
      window.dispatchEvent(new Event('hotelhub:auth-expired'));
      return Promise.reject(refreshError);
    }
  },
);

export default axiosClient;
