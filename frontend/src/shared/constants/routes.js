/**
 * Duong dan tap trung cho toan bo app.
 * Cac feature khac (hotel, coupon, dashboard, user, authentication...)
 * chi can bo sung them constant o day, khong sua cac route co san.
 */
export const ROUTES = {
  home: '/',
  promotions: {
    list: '/promotions',
    create: '/promotions/create',
    detail: (id) => `/promotions/${id}`,
    edit: (id) => `/promotions/${id}/edit`,
  },
  admin: {
    hotelsPending: '/admin/hotels/pending',
    hotelDetail: (hotelId) => `/admin/hotels/${hotelId}`,
    // === Bo sung moi (khong dong den cac key co san o tren) ===
    dashboard: '/admin/dashboard',
    users: '/admin/users',
    tenants: '/admin/tenants',
    tenantSubscriptions: (tenantId) => `/admin/tenants/${tenantId}/subscriptions`,
  },
  notFound: '*',
};