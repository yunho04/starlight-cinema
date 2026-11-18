# db-app-cinema

데이터베이스응용 팀 프로젝트: 영화 예매 서비스 "별빛시네마"

- 배포 주소: https://byeolbit-cinema.vercel.app
- 영화 선택 → 상영시간 선택 → 좌석 선택 → 결제 확인 → 마이페이지 예매 내역

## 폴더 구조

| 폴더 | 내용 |
| --- | --- |
| [`frontend/`](frontend/) | React + Vite 프론트엔드. 백엔드가 없으면 브라우저 안의 목업 데이터로 동작 |
| `backend/` | (예정) 백엔드 API 서버 |

## 데이터 모델

통합 ERD 기준 테이블 7개: `member`, `movie`, `screen`, `seat`, `schedule`, `reservation`, `ticket`

프론트엔드가 기대하는 API 요청·응답 형식과 에러 형식은 [frontend/README.md](frontend/README.md)에 정리되어 있습니다.
