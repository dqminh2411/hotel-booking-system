import { FCM_TOKEN_KEY } from '../../../shared/constants/storageKeys';

// Access tokens are no longer persisted here: keycloak-js keeps the token
// in memory and re-derives auth state on reload via the check-sso silent
// iframe. Only the FCM push-notification token still needs local storage.

export function getStoredFcmToken() {
  return localStorage.getItem(FCM_TOKEN_KEY);
}

export function setStoredFcmToken(fcmToken) {
  if (fcmToken) {
    localStorage.setItem(FCM_TOKEN_KEY, fcmToken);
  }
}

export function clearAuthStorage() {
  localStorage.removeItem(FCM_TOKEN_KEY);
}
