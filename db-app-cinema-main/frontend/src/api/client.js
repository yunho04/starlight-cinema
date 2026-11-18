import { ApiError, ERROR } from './errors.js';
import * as mock from './mock.js';

const BASE_URL = import.meta.env.VITE_API_BASE_URL;
export const isMock = !BASE_URL;

const TOKEN_KEY = 'cinema.token';
export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

async function request(method, path, body) {
  const token = tokenStore.get();
  if (isMock) return mock.handle(method, path, body, token);

  let res;
  try {
    res = await fetch(BASE_URL + path, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'NETWORK', '서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인해 주세요.');
  }
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    if (res.status === 401) tokenStore.clear();
    throw new ApiError(
      res.status,
      data?.code ?? (res.status === 401 ? ERROR.UNAUTHORIZED : undefined),
      data?.message ?? `요청을 처리하지 못했습니다. (HTTP ${res.status})`,
      data,
    );
  }
  return data;
}

export const api = {
  login: (login_id, password) => request('POST', '/auth/login', { login_id, password }),
  me: () => request('GET', '/members/me'),
  getMovies: () => request('GET', '/movies'),
  getMovie: (id) => request('GET', `/movies/${id}`),
  getSchedules: (movieId) => request('GET', `/movies/${movieId}/schedules`),
  getSeats: (scheduleId) => request('GET', `/schedules/${scheduleId}/seats`),
  createReservation: (schedule_id, seat_ids) => request('POST', '/reservations', { schedule_id, seat_ids }),
  getMyReservations: () => request('GET', '/members/me/reservations'),
};

// 목업 전용: "다른 사람이 먼저 예매한" 상황을 재현
export const simulateOtherBooking = (scheduleId, seatId) => mock.simulateOtherBooking(scheduleId, seatId);
