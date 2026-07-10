importScripts('https://www.gstatic.com/firebasejs/11.10.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/11.10.0/firebase-messaging-compat.js');

const params = new URL(self.location.href).searchParams;
firebase.initializeApp({
  apiKey: params.get('apiKey'),
  authDomain: params.get('authDomain'),
  projectId: params.get('projectId'),
  storageBucket: params.get('storageBucket'),
  messagingSenderId: params.get('messagingSenderId'),
  appId: params.get('appId')
});

const messaging = firebase.messaging();

function normalizeBookingStatus(payload) {
  const data = payload?.data || {};
  if (data.status) return data.status.toUpperCase();

  const eventType = (data.eventType || '').toUpperCase();
  if (eventType.includes('CONFIRM')) return 'CONFIRMED';
  if (eventType.includes('CANCEL')) return 'CANCELLED';
  if (eventType.includes('FAIL')) return 'FAILED';
  return '';
}

async function notifyOpenClients(payload) {
  const windowClients = await clients.matchAll({
    type: 'window',
    includeUncontrolled: true
  });

  windowClients.forEach((client) => {
    client.postMessage({
      type: 'FCM_BOOKING_UPDATE',
      payload
    });
  });
}

messaging.onBackgroundMessage((payload) => {
  const status = normalizeBookingStatus(payload);
  const data = {
    ...(payload.data || {}),
    ...(status ? { status } : {})
  };
  const title = data.title || payload.notification?.title || 'Cập nhật đặt phòng';
  const options = {
    body: data.body || payload.notification?.body || 'Trạng thái đặt phòng vừa thay đổi.',
    data
  };

  notifyOpenClients({ ...payload, data });
  self.registration.showNotification(title, options);
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const bookingId = event.notification.data?.bookingId;
  const url = bookingId ? `/bookings/${encodeURIComponent(bookingId)}` : '/';
  event.waitUntil(clients.openWindow(url));
});
