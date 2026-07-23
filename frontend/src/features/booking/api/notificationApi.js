import axiosClient from '../../../shared/api/axiosClient';

export async function upsertDeviceToken(fcmTokenOrUserId, fcmToken) {
  const token = typeof fcmTokenOrUserId === 'string' && fcmToken ? fcmToken : fcmTokenOrUserId;
  const { data } = await axiosClient.post('/api/notifications/device-token', {
    fcmToken: token,
    platform: 'WEB',
  });
  return data;
}

export async function subscribeTopic(fcmTokenOrUserId, topicOrFcmToken, topicName) {
  const fcmToken = topicName ? topicOrFcmToken : fcmTokenOrUserId;
  const topic = topicName || topicOrFcmToken;
  const { data } = await axiosClient.post('/api/notifications/topics/subscribe', {
    fcmToken,
    topic,
  });
  return data;
}

export async function unsubscribeTopic(fcmTokenOrUserId, topicOrFcmToken, topicName) {
  const fcmToken = topicName ? topicOrFcmToken : fcmTokenOrUserId;
  const topic = topicName || topicOrFcmToken;
  const { data } = await axiosClient.post('/api/notifications/topics/unsubscribe', {
    fcmToken,
    topic,
  });
  return data;
}
