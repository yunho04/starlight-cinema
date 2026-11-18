# 별빛시네마 백엔드 API

- Base URL: `http://localhost:8080/api`
- 요청/응답: JSON (`Content-Type: application/json`), 필드명은 snake_case
- 일시: `"2026-11-18T17:10:00"` (한국 시간, 타임존 표기 없음) / 날짜: `"1999-03-14"`
- 인증: 로그인 응답의 `token`을 `Authorization: Bearer <token>` 헤더로 전송 (기본 유효시간 120분)

## 인증이 필요한 API

| 경로 | 인증 |
| --- | --- |
| `/api/auth/**`, `/api/movies/**`, `/api/schedules/**`, `/api/health` | 불필요 |
| `/api/members/**`, `/api/reservations/**` | 필요 (없거나 만료 시 401) |

## 에러 형식

```json
{ "code": "SEAT_ALREADY_RESERVED", "message": "이미 예매된 좌석이 포함되어 있습니다.", "seat_ids": [17] }
```

`seat_ids`는 409 `SEAT_ALREADY_RESERVED`에만 포함된다.

| 상태 | code | 상황 |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED` | 필수값 누락, 형식 오류, 좌석 0개 또는 9개 이상 |
| 400 | `BAD_REQUEST` | JSON 파싱 실패, 경로 변수 타입 오류 |
| 400 | `INVALID_SEAT` | 같은 좌석 중복 선택, 해당 상영관에 없는 좌석 |
| 400 | `SCHEDULE_STARTED` | 이미 시작한 회차를 예매하거나 취소함 |
| 401 | `UNAUTHORIZED` | 토큰 없음, 위조, 만료 |
| 401 | `INVALID_CREDENTIALS` | 아이디 또는 비밀번호 불일치 |
| 403 | `AGE_RESTRICTED` | 관람 연령 미달 (상영일 기준 만 나이) |
| 403 | `FORBIDDEN` | 다른 회원의 예매를 조회하거나 취소함 |
| 404 | `NOT_FOUND` | 영화, 회차, 예매, 경로 없음 |
| 409 | `DUPLICATE_LOGIN_ID` / `DUPLICATE_PHONE` | 회원가입 중복 |
| 409 | `SEAT_ALREADY_RESERVED` | 이미 예매된 좌석 포함 (전체 실패, 일부만 예매되지 않음) |
| 409 | `ALREADY_CANCELLED` | 이미 취소된 예매 |
| 500 | `INTERNAL_ERROR` | 서버 오류 |

---

## 회원

### `POST /api/auth/signup` → 201

```json
// 요청
{ "login_id": "newuser1", "password": "password1234", "name": "홍길동", "phone": "010-1111-2222", "birth_date": "2000-01-01" }
// 응답
{ "member_id": 3, "login_id": "newuser1", "name": "홍길동", "phone": "010-1111-2222", "birth_date": "2000-01-01" }
```

- `login_id`: 영문, 숫자, `_` 4~20자 / `password`: 8~64자 / `phone`: `01012345678` 또는 `010-1234-5678` (`010-1234-5678` 형식으로 저장)
- 비밀번호는 BCrypt 해시로 `members.password_hash`에 저장

### `POST /api/auth/login` → 200

```json
// 요청
{ "login_id": "adult01", "password": "test1234" }
// 응답
{ "token": "eyJhbGciOiJIUzI1NiJ9...", "member": { "member_id": 1, "login_id": "adult01", "name": "김예매", "phone": "010-1234-5678", "birth_date": "1999-03-14" } }
```

### `GET /api/members/me` 🔒 → 200

member 객체 (위와 같음)

### `GET /api/members/me/reservations` 🔒 → 200

내 예매 목록 (최신순). 취소된 예매도 `status: "CANCELLED"`로 포함된다. 항목 형식은 아래 예매 응답과 같다.

## 영화 / 상영회차

### `GET /api/movies` → 200, `GET /api/movies/{id}` → 200

```json
{ "movie_id": 3, "title": "붉은 장막", "running_time": 131, "age_rating": "19", "release_date": null, "poster_url": "/posters/3.svg", "description": "..." }
```

- `age_rating`: `ALL` / `12` / `15` / `19` (DB의 `18`은 `19`로 변환해서 응답)
- `release_date`: DB에 컬럼이 없어 항상 `null`
- `poster_url`: DB에 컬럼이 없어 `/posters/{movie_id}.svg`로 만든 값 (프론트 `public/posters`)

### `GET /api/movies/{id}/schedules` → 200

오늘(한국 시간 0시) 이후 회차를 시작 시각 순으로 반환한다. 오늘 이미 시작한 회차도 포함한다.

```json
[{ "schedule_id": 30, "movie_id": 1, "movie_title": "고래가 사는 도서관", "age_rating": "ALL", "poster_url": "/posters/1.svg",
   "screen_id": 1, "screen_name": "1관", "start_time": "2026-11-18T17:10:00", "end_time": "2026-11-18T18:58:00", "price": 14000 }]
```

`schedule_id`는 DB의 `screenings.screening_id`다.

### `GET /api/schedules/{id}/seats` → 200

```json
{
  "schedule": { "schedule_id": 39, "movie_title": "붉은 장막", "age_rating": "19", "screen_name": "IMAX관", "price": 18000, "...": "..." },
  "seats": [{ "seat_id": 167, "seat_row": "A", "seat_col": 1, "reserved": false }]
}
```

- `seat_col`은 DB의 `seats.seat_no`다.
- `reserved`는 해당 회차에 ACTIVE 티켓이 있는지 여부다. 취소된 티켓은 `false`.

## 예매

### `POST /api/reservations` 🔒 → 201 (모의 결제 포함)

```json
// 요청
{ "schedule_id": 39, "seat_ids": [300, 301] }
// 응답
{
  "reservation_id": 8, "status": "CONFIRMED", "reserved_at": "2026-11-16T20:22:32", "total_price": 36000,
  "schedule": { "schedule_id": 39, "movie_id": 3, "movie_title": "붉은 장막", "age_rating": "19", "poster_url": "/posters/3.svg",
                "screen_id": 3, "screen_name": "IMAX관", "start_time": "2026-11-18T21:50:00", "end_time": "2026-11-19T00:01:00", "price": 18000 },
  "tickets": [
    { "seat_id": 300, "seat_row": "I", "seat_col": 6, "price": 18000, "status": "ACTIVE" },
    { "seat_id": 301, "seat_row": "I", "seat_col": 7, "price": 18000, "status": "ACTIVE" }
  ]
}
```

- 좌석은 1~8석이다. `total_price`는 회차 가격 × 좌석 수로 계산해서 `reservations.total_price`에 저장한다.
- 검사 순서: 회차 존재 → 상영 시작 여부 → 관람 연령 → 좌석 소속 → 중복 좌석
- 좌석이 하나라도 겹치면 전체가 실패하고 아무것도 저장되지 않는다.
- 티켓에는 별도 `ticket_id`가 없다. DB 기본키가 `(reservation_id, seat_id)`이기 때문이다.

### `GET /api/reservations/{id}` 🔒 → 200

본인 예매만 조회할 수 있다. 다른 회원의 예매는 403.

### `POST /api/reservations/{id}/cancel` 🔒 → 200

본인 예매만, 상영 시작 전까지 취소할 수 있다. 예약과 티켓이 `CANCELLED`가 되고, 해당 좌석은 즉시 다시 예매할 수 있다. 응답은 예매 객체이며 `status`는 `"CANCELLED"`다.

## 기타

- `GET /api/health`: `"Cinema Backend OK"`
- `GET /api/db-check`: DB 연결 확인용 임시 API (배포 전 삭제 권장)
