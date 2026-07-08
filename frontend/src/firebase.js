import { getApp, getApps, initializeApp } from 'firebase/app';
import { getMessaging, getToken, isSupported, onMessage } from 'firebase/messaging';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY ,
  authDomain:
    import.meta.env.VITE_FIREBASE_AUTH_DOMAIN ,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID ,
  storageBucket:
    import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId:
    import.meta.env.VITE_FIREBASE_APP_ID
};

async function getFirebaseMessaging() {
  if (!(await isSupported())) {
    throw new Error('Trình duyệt này không hỗ trợ Firebase Cloud Messaging.');
  }

  const app = getApps().length ? getApp() : initializeApp(firebaseConfig);
  return getMessaging(app);
}

function getServiceWorkerUrl() {
  const params = new URLSearchParams(firebaseConfig);
  return `/firebase-messaging-sw.js?${params.toString()}`;
}

export async function requestFcmToken() {
  if (!('Notification' in window) || !('serviceWorker' in navigator)) {
    throw new Error('Trình duyệt không hỗ trợ Web Push.');
  }

  const permission = await Notification.requestPermission();
  if (permission !== 'granted') {
    throw new Error('Bạn chưa cấp quyền nhận thông báo.');
  }

  const vapidKey = import.meta.env.VITE_FIREBASE_VAPID_KEY;
  if (!vapidKey) {
    throw new Error('Thiếu biến VITE_FIREBASE_VAPID_KEY.');
  }

  // register service worker and wait until it's ready
  // call to https://fcmregistrations.googleapis.com/v1/projects/hotel-booking-system-bc2eb/registrations
  const registration = await navigator.serviceWorker.register(getServiceWorkerUrl());
  await navigator.serviceWorker.ready;

  // get firebase messaging
  const messaging = await getFirebaseMessaging();

  // request fcm token with messaging and service worker registration
  const token = await getToken(messaging, {
    vapidKey,
    serviceWorkerRegistration: registration
  });

  if (!token) {
    throw new Error('Firebase không trả về FCM token.');
  }
  console.log('FCM token:', token);
  return token;
}

export async function listenForegroundMessages(callback) {
  const messaging = await getFirebaseMessaging();
  return onMessage(messaging, callback);
}
