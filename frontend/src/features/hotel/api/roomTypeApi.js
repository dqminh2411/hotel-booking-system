import axiosClient from '../../../shared/api/axiosClient';

export async function fetchRoomTypeDetail(roomTypeId) {
  const { data } = await axiosClient.get(`/api/room-types/${roomTypeId}`);
  return data.data;
}
