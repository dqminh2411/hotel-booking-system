import axiosClient from '../../../shared/api/axiosClient';

export async function fetchHotelDetail(hotelId, { checkinDate, checkoutDate, guestNum, roomNum } = {}) {
  const { data } = await axiosClient.get(`/api/hotels/${hotelId}`, {
    params: { checkinDate, checkoutDate, guestNum, roomNum },
  });
  return data.data;
}

export async function fetchRoomTypesByHotelId(hotelId) {
  const { data } = await axiosClient.get(`/api/hotels/${hotelId}/room-types`);
  return data.data;
}
