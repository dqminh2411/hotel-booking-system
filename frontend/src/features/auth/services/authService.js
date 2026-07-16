import axiosClient from '../../../shared/api/axiosClient';

// Registration, login, and Google sign-in are now handled entirely by
// Keycloak's hosted pages (see features/auth/keycloak.js). This service is
// only responsible for telling the backend to drop the device's FCM token
// as part of logout.
export async function logoutUser(fcmToken) {
  if (!fcmToken) return null;
  const { data } = await axiosClient.post('/api/auth/logout', { fcmToken });
  return data;
}
