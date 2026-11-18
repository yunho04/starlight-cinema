export class ApiError extends Error {
  constructor(status, code, message, data) {
    super(message);
    this.status = status;
    this.code = code;
    this.data = data;
  }
}

// 백엔드와 맞출 에러 코드 (회의록 2-4 "중복 예매 실패 시 응답 형식")
export const ERROR = {
  SEAT_ALREADY_RESERVED: 'SEAT_ALREADY_RESERVED', // 409
  AGE_RESTRICTED: 'AGE_RESTRICTED', // 403
  SCHEDULE_STARTED: 'SCHEDULE_STARTED', // 400
  UNAUTHORIZED: 'UNAUTHORIZED', // 401
  NOT_FOUND: 'NOT_FOUND', // 404
};
