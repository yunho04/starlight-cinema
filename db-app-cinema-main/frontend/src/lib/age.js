import { parseDate, parseDateTime } from './datetime.js';

// 관람등급(age_rating: ALL / 12 / 15 / 19) → 관람 가능 최소 만 나이
export function minAgeFor(rating) {
  return rating === 'ALL' ? 0 : Number(rating);
}

export const RATING_LABEL = {
  ALL: '전체 관람가',
  12: '12세 이상 관람가',
  15: '15세 이상 관람가',
  19: '청소년 관람불가',
};

// 상영일 기준 만 나이
export function ageOn(birthDate, onDate) {
  const b = parseDate(birthDate);
  const d = parseDateTime(onDate);
  let age = d.getFullYear() - b.getFullYear();
  const beforeBirthday =
    d.getMonth() < b.getMonth() || (d.getMonth() === b.getMonth() && d.getDate() < b.getDate());
  if (beforeBirthday) age -= 1;
  return age;
}

// 회원이 해당 회차를 볼 수 있는지 판단. 막힌 경우 안내 문구를 함께 돌려준다.
export function checkAge(member, rating, startTime) {
  const need = minAgeFor(rating);
  if (!member || need === 0) return { allowed: true };
  const age = ageOn(member.birth_date, startTime);
  if (age >= need) return { allowed: true, age };
  return {
    allowed: false,
    age,
    message: `만 ${need}세 이상 관람할 수 있는 영화입니다. 상영일 기준 회원님은 만 ${age}세라 예매할 수 없습니다.`,
  };
}
