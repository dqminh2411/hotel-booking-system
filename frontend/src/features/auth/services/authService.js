import axiosClient from '../../../shared/api/axiosClient';

const googleAuthPath =
  import.meta.env.VITE_GOOGLE_AUTH_PATH || '/api/users/oauth/google';

export async function registerUser({ fullName, email, phone, password }) {
  const { data } = await axiosClient.post('/api/users', {
    fullName: fullName.trim(),
    email: email.trim().toLowerCase(),
    phone: phone.trim(),
    password,
  });
  return data;
}

export async function loginUser({ email, password }) {
  const { data } = await axiosClient.post('/api/auth/login', {
    email: email.trim().toLowerCase(),
    password,
  });
  return data;
}

export async function loginWithGoogle(idToken) {
  const { data } = await axiosClient.post(googleAuthPath, { idToken });
  return data;
}

export async function logoutUser(fcmToken) {
  if (!fcmToken) return null;
  const { data } = await axiosClient.post('/api/auth/logout', { fcmToken });
  return data;
}

export async function verifyEmail(token) {
  const { data } = await axiosClient.post('/api/auth/verify-email', { token });
  return data;
}
