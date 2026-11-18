// 통합 ERD(member, movie, screen, seat, schedule, reservation, ticket) 모양 그대로 만든 목업 데이터.
// 상영회차는 오늘부터 3일치를 실행 시점 기준으로 생성한다.
import { toLocalDateString, toLocalDateTimeString } from '../lib/datetime.js';

const ROW_LETTERS = 'ABCDEFGHIJKLMNOP';

export const MOVIES = [
  {
    movie_id: 1,
    title: '고래가 사는 도서관',
    running_time: 108,
    age_rating: 'ALL',
    release_date: '2026-10-22',
    poster_url: '/posters/1.svg',
    description:
      '폐관을 앞둔 바닷가 마을 도서관. 열한 살 소리는 서가 맨 아래 칸에서 물소리가 나는 책을 발견한다. 책장을 넘길 때마다 도서관은 조금씩 바다가 되고, 소리는 그 안에 사는 늙은 고래와 함께 도서관을 지킬 방법을 찾아 나선다.',
  },
  {
    movie_id: 2,
    title: '마지막 정거장',
    running_time: 124,
    age_rating: '15',
    release_date: '2026-11-05',
    poster_url: '/posters/2.svg',
    description:
      '자정을 넘긴 심야버스 472번. 종점까지 남은 정거장은 일곱 개, 승객은 여섯 명. 운전기사 도윤은 백미러 속 승객 하나가 탈 때는 없었다는 사실을 깨닫는다. 버스가 멈출 때마다 누군가 사라진다.',
  },
  {
    movie_id: 3,
    title: '붉은 장막',
    running_time: 131,
    age_rating: '19',
    release_date: '2026-10-29',
    poster_url: '/posters/3.svg',
    description:
      '1987년 부산, 문을 닫은 극장 대영관의 영사실에서 시신이 발견된다. 마지막 상영작의 필름 릴 세 개가 함께 사라졌다. 은퇴를 한 달 앞둔 형사 한석은 필름을 쫓다 극장 소유주 일가의 오래된 비밀과 마주한다.',
  },
];

export const SCREENS = [
  { screen_id: 1, name: '1관', rows: 8, cols: 12 },
  { screen_id: 2, name: '2관', rows: 7, cols: 10 },
  { screen_id: 3, name: 'IMAX관', rows: 10, cols: 16 },
];

// 영화별 상영관과 하루 상영 시작 시각
const DAILY_PLAN = [
  { movie_id: 1, screen_id: 1, times: ['09:40', '12:10', '14:40', '17:10'] },
  { movie_id: 2, screen_id: 2, times: ['10:20', '13:00', '16:30', '19:10', '22:30'] },
  { movie_id: 3, screen_id: 3, times: ['11:00', '15:20', '18:40', '21:50'] },
];

function priceFor(screenId, hour) {
  const base = screenId === 3 ? 18000 : 14000;
  return hour < 11 ? base - 4000 : base; // 조조 할인
}

function rng(seed) {
  let s = seed * 9301 + 49297;
  return () => {
    s = (s * 9301 + 49297) % 233280;
    return s / 233280;
  };
}

export function buildSeed(now = new Date()) {
  const members = [
    {
      member_id: 1,
      login_id: 'adult01',
      password: 'test1234',
      name: '김예매',
      phone: '010-1234-5678',
      birth_date: '1999-03-14',
    },
    {
      member_id: 2,
      login_id: 'teen01',
      password: 'test1234',
      name: '이청소',
      phone: '010-9876-5432',
      birth_date: '2012-05-20', // 2026년 11월 기준 만 14세 → 15·19 영화 예매 불가
    },
    { member_id: 99, login_id: 'others', password: '', name: '다른 관객', phone: '', birth_date: '1990-01-01' },
  ];

  const seats = [];
  let seatId = 1;
  for (const sc of SCREENS) {
    for (let r = 0; r < sc.rows; r++) {
      for (let c = 1; c <= sc.cols; c++) {
        seats.push({ seat_id: seatId++, screen_id: sc.screen_id, seat_row: ROW_LETTERS[r], seat_col: c });
      }
    }
  }

  const schedules = [];
  let scheduleId = 1;
  for (let day = 0; day < 3; day++) {
    for (const plan of DAILY_PLAN) {
      const movie = MOVIES.find((m) => m.movie_id === plan.movie_id);
      for (const t of plan.times) {
        const [h, m] = t.split(':').map(Number);
        const start = new Date(now.getFullYear(), now.getMonth(), now.getDate() + day, h, m);
        const end = new Date(start.getTime() + movie.running_time * 60000);
        schedules.push({
          schedule_id: scheduleId++,
          movie_id: plan.movie_id,
          screen_id: plan.screen_id,
          start_time: toLocalDateTimeString(start),
          end_time: toLocalDateTimeString(end),
          price: priceFor(plan.screen_id, h),
        });
      }
    }
  }

  // 다른 관객이 미리 예매해 둔 좌석 (회차마다 다르게, 2~3석씩 묶음)
  const reservations = [];
  const tickets = [];
  let reservationId = 1;
  let ticketId = 1;
  for (const s of schedules) {
    const rand = rng(s.schedule_id);
    const screenSeats = seats.filter((x) => x.screen_id === s.screen_id);
    const target = Math.floor(screenSeats.length * (0.12 + rand() * 0.3));
    const taken = new Set();
    while (taken.size < target) {
      const start = Math.floor(rand() * screenSeats.length);
      const size = 1 + Math.floor(rand() * 3);
      const group = screenSeats.slice(start, start + size).filter((x) => !taken.has(x.seat_id));
      if (!group.length) continue;
      const rid = reservationId++;
      reservations.push({ reservation_id: rid, member_id: 99, schedule_id: s.schedule_id, reserved_at: toLocalDateTimeString(now) });
      for (const seat of group) {
        taken.add(seat.seat_id);
        tickets.push({ ticket_id: ticketId++, reservation_id: rid, schedule_id: s.schedule_id, seat_id: seat.seat_id, price: s.price });
      }
    }
  }

  return {
    seedDate: toLocalDateString(now),
    members,
    movies: MOVIES,
    screens: SCREENS.map(({ screen_id, name }) => ({ screen_id, name })),
    seats,
    schedules,
    reservations,
    tickets,
    nextId: { reservation: reservationId, ticket: ticketId },
  };
}
