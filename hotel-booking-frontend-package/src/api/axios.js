import axios from 'axios';

const axiosClient = axios.create({
    baseURL: 'http://localhost:8080', // TODO: Thay đổi URL này thành URL Backend của bạn
    headers: {
        'Content-Type': 'application/json'
    }
});

// Thêm interceptors nếu cần thiết sau này
axiosClient.interceptors.response.use(
    (response) => response,
    (error) => Promise.reject(error)
);

export default axiosClient;