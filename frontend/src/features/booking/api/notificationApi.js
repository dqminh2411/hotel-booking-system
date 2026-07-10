import axiosClient from '../../../shared/api/axiosClient';

export async function upsertDeviceToken(userId, fcmToken) {
  const { data } = await axiosClient.post(
    '/api/notifications/device-token',
    { fcmToken, platform: 'WEB' },
    { headers: { 'X-User-Id': userId } },
  );
  return data;
}
