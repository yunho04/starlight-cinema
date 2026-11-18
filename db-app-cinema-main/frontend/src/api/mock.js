// 백엔드가 준비되기 전까지 회의록 2-4의 API를 브라우저 안에서 흉내 내는 목업 서버.
// 데이터는 localStorage에 저장되며, 날짜가 바뀌면 새로 생성된다.
import { ApiError, ERROR } from './errors.js';
import { buildSeed } from './mockData.js';
import { checkAge } from '../lib/age.js';
import { parseDateTime, toLocalDateString, toLocalDateTimeString } from '../lib/datetime.js';

const STORE_KEY = 'cinema.mock.v2';
const LATENCY = 250;

let db = load();

function load() {
  try {
    const saved = JSON.parse(localStorage.getItem(STORE_KEY));
    if (saved && saved.seedDate === toLocalDateString(new Date())) return saved;
  } catch {
    /* 저장본이 깨졌으면 새로 만든다 */
  }
  const seed = buildSeed();
  persist(seed);
  return seed;
}

function persist(data = db) {
  try {
    localStorage.setItem(STORE_KEY, JSON.stringify(data));
  } catch {
    /* 저장 실패해도 메모리 데이터로 계속 동작 */
  }
}

const delay = () => new Promise((r) => setTimeout(r, LATENCY));
const fail = (status, code, message, extra) => {
  throw new ApiError(status, code, message, { code, message, ...extra });
};

function publicMember(m) {
  const { password, ...rest } = m;
  return rest;
}

function currentMember(token) {
  const id = Number(String(token ?? '').replace('mock-token-', ''));
  const m = db.members.find((x) => x.member_id === id);
  if (!m) fail(401, ERROR.UNAUTHORIZED, '로그인이 필요합니다.');
  return m;
}

function scheduleView(s) {
  const screen = db.screens.find((x) => x.screen_id === s.screen_id);
  const movie = db.movies.find((x) => x.movie_id === s.movie_id);
  return {
    ...s,
    screen_name: screen.name,
    movie_title: movie.title,
    age_rating: movie.age_rating,
    poster_url: movie.poster_url,
  };
}

function reservationView(r) {
  const schedule = scheduleView(db.schedules.find((s) => s.schedule_id === r.schedule_id));
  const tickets = db.tickets
    .filter((t) => t.reservation_id === r.reservation_id)
    .map((t) => {
      const seat = db.seats.find((x) => x.seat_id === t.seat_id);
      return { ticket_id: t.ticket_id, seat_id: t.seat_id, seat_row: seat.seat_row, seat_col: seat.seat_col, price: t.price };
    })
    .sort((a, b) => a.seat_row.localeCompare(b.seat_row) || a.seat_col - b.seat_col);
  return {
    reservation_id: r.reservation_id,
    reserved_at: r.reserved_at,
    total_price: tickets.reduce((sum, t) => sum + t.price, 0),
    schedule,
    tickets,
  };
}

const routes = [
  ['POST', /^\/auth\/login$/, (_, body) => {
    const m = db.members.find((x) => x.login_id === body?.login_id && x.password && x.password === body?.password);
    if (!m) fail(401, ERROR.UNAUTHORIZED, '아이디 또는 비밀번호가 맞지 않습니다.');
    return { token: `mock-token-${m.member_id}`, member: publicMember(m) };
  }],
  ['GET', /^\/members\/me$/, (_, __, token) => publicMember(currentMember(token))],
  ['GET', /^\/movies$/, () => db.movies],
  ['GET', /^\/movies\/(\d+)$/, ([id]) => {
    const m = db.movies.find((x) => x.movie_id === Number(id));
    if (!m) fail(404, ERROR.NOT_FOUND, '영화를 찾을 수 없습니다.');
    return m;
  }],
  ['GET', /^\/movies\/(\d+)\/schedules$/, ([id]) =>
    db.schedules
      .filter((s) => s.movie_id === Number(id))
      .map(scheduleView)
      .sort((a, b) => a.start_time.localeCompare(b.start_time)),
  ],
  ['GET', /^\/schedules\/(\d+)\/seats$/, ([id]) => {
    const s = db.schedules.find((x) => x.schedule_id === Number(id));
    if (!s) fail(404, ERROR.NOT_FOUND, '상영회차를 찾을 수 없습니다.');
    const reserved = new Set(db.tickets.filter((t) => t.schedule_id === s.schedule_id).map((t) => t.seat_id));
    return {
      schedule: scheduleView(s),
      seats: db.seats
        .filter((x) => x.screen_id === s.screen_id)
        .map((x) => ({ seat_id: x.seat_id, seat_row: x.seat_row, seat_col: x.seat_col, reserved: reserved.has(x.seat_id) })),
    };
  }],
  ['POST', /^\/reservations$/, (_, body, token) => {
    const member = currentMember(token);
    const s = db.schedules.find((x) => x.schedule_id === Number(body?.schedule_id));
    if (!s) fail(404, ERROR.NOT_FOUND, '상영회차를 찾을 수 없습니다.');
    const seatIds = [...new Set((body?.seat_ids ?? []).map(Number))];
    if (!seatIds.length) fail(400, 'BAD_REQUEST', '좌석을 1개 이상 선택해 주세요.');
    if (parseDateTime(s.start_time) <= new Date()) fail(400, ERROR.SCHEDULE_STARTED, '이미 상영이 시작된 회차입니다.');
    const movie = db.movies.find((x) => x.movie_id === s.movie_id);
    const age = checkAge(member, movie.age_rating, s.start_time);
    if (!age.allowed) fail(403, ERROR.AGE_RESTRICTED, age.message);
    if (seatIds.some((id) => !db.seats.find((x) => x.seat_id === id && x.screen_id === s.screen_id))) {
      fail(400, 'BAD_REQUEST', '이 상영관에 없는 좌석입니다.');
    }
    // (schedule_id, seat_id) UNIQUE 제약 흉내: 하나라도 겹치면 전체 실패
    const conflicts = db.tickets.filter((t) => t.schedule_id === s.schedule_id && seatIds.includes(t.seat_id));
    if (conflicts.length) {
      fail(409, ERROR.SEAT_ALREADY_RESERVED, '이미 예매된 좌석이 포함되어 있습니다.', {
        seat_ids: conflicts.map((t) => t.seat_id),
      });
    }
    const r = {
      reservation_id: db.nextId.reservation++,
      member_id: member.member_id,
      schedule_id: s.schedule_id,
      reserved_at: toLocalDateTimeString(new Date()),
    };
    db.reservations.push(r);
    for (const seatId of seatIds) {
      db.tickets.push({ ticket_id: db.nextId.ticket++, reservation_id: r.reservation_id, schedule_id: s.schedule_id, seat_id: seatId, price: s.price });
    }
    persist();
    return reservationView(r);
  }],
  ['GET', /^\/members\/me\/reservations$/, (_, __, token) => {
    const member = currentMember(token);
    return db.reservations
      .filter((r) => r.member_id === member.member_id)
      .sort((a, b) => b.reserved_at.localeCompare(a.reserved_at) || b.reservation_id - a.reservation_id)
      .map(reservationView);
  }],
];

export async function handle(method, path, body, token) {
  await delay();
  for (const [m, re, fn] of routes) {
    const match = method === m && path.match(re);
    if (match) return structuredClone(fn(match.slice(1), body, token));
  }
  fail(404, ERROR.NOT_FOUND, `목업에 없는 요청입니다: ${method} ${path}`);
}

export function simulateOtherBooking(scheduleId, seatId) {
  if (db.tickets.some((t) => t.schedule_id === scheduleId && t.seat_id === seatId)) return;
  const s = db.schedules.find((x) => x.schedule_id === scheduleId);
  const rid = db.nextId.reservation++;
  db.reservations.push({ reservation_id: rid, member_id: 99, schedule_id: scheduleId, reserved_at: toLocalDateTimeString(new Date()) });
  db.tickets.push({ ticket_id: db.nextId.ticket++, reservation_id: rid, schedule_id: scheduleId, seat_id: seatId, price: s.price });
  persist();
}
