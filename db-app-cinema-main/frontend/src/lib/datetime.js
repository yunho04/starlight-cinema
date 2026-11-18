// 백엔드 DATETIME("2026-11-12 14:30:00" 또는 "2026-11-12T14:30:00")을 로컬 시각으로 해석한다.
export function parseDateTime(value) {
  if (value instanceof Date) return value;
  return new Date(String(value).replace(' ', 'T'));
}

// DATE("2011-11-12")를 로컬 자정으로 해석한다. (new Date("2011-11-12")는 UTC로 해석되므로 쓰지 않음)
export function parseDate(value) {
  const [y, m, d] = String(value).slice(0, 10).split('-').map(Number);
  return new Date(y, m - 1, d);
}

const pad = (n) => String(n).padStart(2, '0');

export function toLocalDateTimeString(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`;
}

export function toLocalDateString(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];

export function formatDay(value) {
  const d = parseDateTime(value);
  return `${d.getMonth() + 1}월 ${d.getDate()}일 (${WEEKDAYS[d.getDay()]})`;
}

export function formatTime(value) {
  const d = parseDateTime(value);
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function formatDateTime(value) {
  return `${formatDay(value)} ${formatTime(value)}`;
}

export function formatDate(value) {
  const d = parseDate(value);
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())}`;
}

export function formatWon(n) {
  return `${Number(n).toLocaleString('ko-KR')}원`;
}

export function formatRunningTime(minutes) {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return h ? `${minutes}분 (${h}시간${m ? ` ${m}분` : ''})` : `${minutes}분`;
}
