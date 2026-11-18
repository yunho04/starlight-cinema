import { formatDateTime, formatTime, formatWon } from '../lib/datetime.js';
import Poster from '../components/Poster.jsx';
import RatingBadge from '../components/RatingBadge.jsx';

const seatsText = (tickets) => tickets.map((t) => `${t.seat_row}${t.seat_col}`).join(', ');

// 예매 1건 (좌석이 여러 개여도 한 건으로 묶어서 표시)
export default function ReservationCard({ reservation }) {
  const { schedule, tickets } = reservation;
  const movie = { title: schedule.movie_title, poster_url: schedule.poster_url };
  return (
    <article className="resv">
      <Poster movie={movie} className="poster-thumb" />
      <div className="resv-body">
        <p className="resv-no">예매번호 {reservation.reservation_id}</p>
        <h2 className="resv-title">
          <RatingBadge rating={schedule.age_rating} />
          {schedule.movie_title}
        </h2>
        <table className="info-table">
          <tbody>
            <tr>
              <th scope="row">상영일시</th>
              <td>
                {formatDateTime(schedule.start_time)} ~ {formatTime(schedule.end_time)}
              </td>
            </tr>
            <tr>
              <th scope="row">상영관</th>
              <td>{schedule.screen_name}</td>
            </tr>
            <tr>
              <th scope="row">좌석</th>
              <td>
                {seatsText(tickets)} ({tickets.length}명)
              </td>
            </tr>
            <tr>
              <th scope="row">예매일시</th>
              <td>{formatDateTime(reservation.reserved_at)}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div className="resv-price">
        <span>결제 금액</span>
        <strong>{formatWon(reservation.total_price)}</strong>
      </div>
    </article>
  );
}
