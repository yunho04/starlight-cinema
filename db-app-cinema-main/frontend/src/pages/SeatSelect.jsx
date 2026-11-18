import { Fragment, useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api, isMock, simulateOtherBooking } from '../api/client.js';
import { ERROR } from '../api/errors.js';
import { useAuth } from '../auth.jsx';
import { checkAge } from '../lib/age.js';
import { formatDateTime, formatTime, formatWon } from '../lib/datetime.js';
import StepBar from '../components/StepBar.jsx';
import MovieStrip from '../components/MovieStrip.jsx';
import Modal from '../components/Modal.jsx';
import { ErrorBox, Loading } from '../components/Status.jsx';

const MAX_SEATS = 8;
const seatLabel = (s) => `${s.seat_row}${s.seat_col}`;
const bySeat = (a, b) => a.seat_row.localeCompare(b.seat_row) || a.seat_col - b.seat_col;

export default function SeatSelect() {
  const { scheduleId } = useParams();
  const { member } = useAuth();
  const navigate = useNavigate();

  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [selected, setSelected] = useState([]); // seat_id[]
  const [notice, setNotice] = useState(null);
  const [paying, setPaying] = useState(false);

  const load = useCallback(async () => {
    setError(null);
    try {
      const res = await api.getSeats(scheduleId);
      setData(res);
      // 새로 불러온 현황에서 이미 예매된 좌석은 선택에서 뺀다
      const taken = new Set(res.seats.filter((s) => s.reserved).map((s) => s.seat_id));
      setSelected((prev) => prev.filter((id) => !taken.has(id)));
      return res;
    } catch (e) {
      setError(e);
      return null;
    }
  }, [scheduleId]);

  useEffect(() => {
    load();
  }, [load]);

  const layout = useMemo(() => {
    if (!data) return null;
    const rows = [...new Set(data.seats.map((s) => s.seat_row))].sort();
    const maxCol = Math.max(...data.seats.map((s) => s.seat_col));
    const map = new Map(data.seats.map((s) => [`${s.seat_row}-${s.seat_col}`, s]));
    const edge = maxCol >= 12 ? Math.floor(maxCol / 4) : maxCol >= 8 ? 2 : 0;
    const aisles = edge ? new Set([edge, maxCol - edge]) : new Set();
    return { rows, maxCol, map, aisles };
  }, [data]);

  if (error && !data) return <ErrorBox error={error} onRetry={load} />;
  if (!data) return <Loading />;

  const { schedule } = data;
  const age = checkAge(member, schedule.age_rating, schedule.start_time);
  const selectedSeats = data.seats.filter((s) => selected.includes(s.seat_id)).sort(bySeat);
  const total = schedule.price * selectedSeats.length;

  function toggle(seat) {
    setNotice(null);
    if (selected.includes(seat.seat_id)) {
      setSelected(selected.filter((id) => id !== seat.seat_id));
    } else if (selected.length >= MAX_SEATS) {
      setNotice({ type: 'warn', text: `한 번에 최대 ${MAX_SEATS}석까지 예매할 수 있습니다.` });
    } else {
      setSelected([...selected, seat.seat_id]);
    }
  }

  async function onReservationConflict(err) {
    setPaying(false);
    const fresh = await load();
    const conflictIds = err.data?.seat_ids ?? [];
    const names = (fresh?.seats ?? data.seats)
      .filter((s) => conflictIds.includes(s.seat_id))
      .sort(bySeat)
      .map(seatLabel);
    setNotice({
      type: 'error',
      text: `${names.length ? `${names.join(', ')} 좌석은` : '선택한 좌석 중 일부가'} 방금 다른 분이 먼저 예매했습니다. 좌석 현황을 새로 불러왔으니 다른 좌석을 선택해 주세요.`,
    });
  }

  const movie = { title: schedule.movie_title, age_rating: schedule.age_rating, poster_url: schedule.poster_url };

  if (!age.allowed) {
    return (
      <section>
        <StepBar current={1} />
        <p className="notice notice-warn">{age.message}</p>
        <Link to={`/movies/${schedule.movie_id}/schedules`} className="btn btn-outline">
          다른 상영시간 보기
        </Link>
      </section>
    );
  }

  return (
    <section className="seat-page">
      <StepBar current={paying ? 2 : 1} />
      <MovieStrip
        movie={movie}
        action={<Link to={`/movies/${schedule.movie_id}/schedules`}>상영시간 변경</Link>}
      >
        <span>{formatDateTime(schedule.start_time)} ~ {formatTime(schedule.end_time)}</span>
        <span>{schedule.screen_name}</span>
      </MovieStrip>

      {notice && (
        <p className={`notice notice-${notice.type}`} role="alert">
          {notice.text}
        </p>
      )}
      {error && <ErrorBox error={error} onRetry={load} />}

      <div className="seat-stage">
        <div className="screen-bar" aria-hidden="true">
          스크린
        </div>
        <div className="seat-map-scroll">
          <div className="seat-map" role="group" aria-label={`${schedule.screen_name} 좌석 배치도`}>
            {layout.rows.map((row) => (
              <div className="seat-row" key={row}>
                <span className="row-label">{row}</span>
                {Array.from({ length: layout.maxCol }, (_, i) => i + 1).map((col) => {
                  const seat = layout.map.get(`${row}-${col}`);
                  const isAisle = layout.aisles.has(col);
                  let cell;
                  if (!seat) {
                    cell = <span className="seat seat-none" />;
                  } else {
                    const isSel = selected.includes(seat.seat_id);
                    cell = (
                      <button
                        type="button"
                        className={`seat ${seat.reserved ? 'seat-taken' : ''} ${isSel ? 'seat-selected' : ''}`}
                        disabled={seat.reserved}
                        aria-pressed={isSel}
                        aria-label={`${seatLabel(seat)}${seat.reserved ? ' 예매됨' : ''}`}
                        onClick={() => toggle(seat)}
                      >
                        {col}
                      </button>
                    );
                  }
                  return (
                    <Fragment key={col}>
                      {cell}
                      {isAisle && <span className="aisle" />}
                    </Fragment>
                  );
                })}
              </div>
            ))}
          </div>
        </div>
        <ul className="legend">
          <li>
            <span className="seat seat-demo" /> 선택 가능
          </li>
          <li>
            <span className="seat seat-demo seat-selected" /> 선택
          </li>
          <li>
            <span className="seat seat-demo seat-taken" /> 예매 완료
          </li>
        </ul>
      </div>

      <div className="pay-bar">
        <div className="pay-bar-inner">
          <div className="pay-seats">
            <span className="pay-label">선택 좌석</span>
            {selectedSeats.length === 0 ? (
              <span className="pay-empty">좌석을 선택하세요. 최대 {MAX_SEATS}석</span>
            ) : (
              <span className="pay-seat-list">{selectedSeats.map(seatLabel).join(', ')}</span>
            )}
          </div>
          <div className="pay-total">
            <span className="pay-label">
              {formatWon(schedule.price)} × {selectedSeats.length}
            </span>
            <strong>{formatWon(total)}</strong>
          </div>
          <button
            type="button"
            className="btn btn-primary btn-lg pay-button"
            disabled={!selectedSeats.length}
            onClick={() => {
              setNotice(null);
              setPaying(true);
            }}
          >
            결제하기
          </button>
        </div>
      </div>

      {paying && (
        <PaymentModal
          schedule={schedule}
          seats={selectedSeats}
          total={total}
          onClose={() => setPaying(false)}
          onSuccess={(reservation) =>
            navigate(`/reservations/${reservation.reservation_id}/complete`, { state: { reservation } })
          }
          onConflict={onReservationConflict}
        />
      )}
    </section>
  );
}

function PaymentModal({ schedule, seats, total, onClose, onSuccess, onConflict }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [simulate, setSimulate] = useState(false);

  async function confirm() {
    setBusy(true);
    setError('');
    try {
      if (isMock && simulate) simulateOtherBooking(schedule.schedule_id, seats[0].seat_id);
      const reservation = await api.createReservation(
        schedule.schedule_id,
        seats.map((s) => s.seat_id),
      );
      onSuccess(reservation);
    } catch (err) {
      if (err.code === ERROR.SEAT_ALREADY_RESERVED || err.status === 409) {
        onConflict(err);
        return;
      }
      setError(err.message);
      setBusy(false);
    }
  }

  return (
    <Modal title="결제 확인" onClose={busy ? undefined : onClose}>
      <dl className="receipt">
        <div>
          <dt>영화</dt>
          <dd>{schedule.movie_title}</dd>
        </div>
        <div>
          <dt>상영관</dt>
          <dd>{schedule.screen_name}</dd>
        </div>
        <div>
          <dt>시작 시간</dt>
          <dd>{formatDateTime(schedule.start_time)}</dd>
        </div>
        <div>
          <dt>좌석</dt>
          <dd>{seats.map(seatLabel).join(', ')}</dd>
        </div>
        <div className="receipt-total">
          <dt>총액</dt>
          <dd>{formatWon(total)}</dd>
        </div>
      </dl>
      <p className="muted small">실제 결제는 이루어지지 않습니다. 확인을 누르면 예매가 완료됩니다.</p>
      {isMock && (
        <label className="dev-toggle">
          <input type="checkbox" checked={simulate} onChange={(e) => setSimulate(e.target.checked)} />
          테스트: 다른 사람이 {seatLabel(seats[0])} 좌석을 먼저 예매한 상황 만들기
        </label>
      )}
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
      <div className="modal-actions">
        <button type="button" className="btn btn-outline" onClick={onClose} disabled={busy}>
          취소
        </button>
        <button type="button" className="btn btn-primary" onClick={confirm} disabled={busy}>
          {busy ? '예매하는 중…' : '확인'}
        </button>
      </div>
    </Modal>
  );
}
