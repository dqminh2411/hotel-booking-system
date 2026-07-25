import axiosClient from '../../../shared/api/axiosClient';

export async function createBooking(payload) {
  const { data } = await axiosClient.post('/place-booking', payload);
  return data;
}

export async function fetchBookingById(bookingId) {
  const { data } = await axiosClient.get(`/bookings/${bookingId}`);
  return data;
}
