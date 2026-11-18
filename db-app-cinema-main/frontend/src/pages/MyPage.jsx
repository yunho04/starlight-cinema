import { Link } from 'react-router-dom';
import { api } from '../api/client.js';
import { useAuth } from '../auth.jsx';
import { useAsync } from '../lib/useAsync.js';
import { formatDate } from '../lib/datetime.js';
import { ErrorBox, Loading } from '../components/Status.jsx';
import ReservationCard from './ReservationCard.jsx';

export default function MyPage() {
  const { member } = useAuth();
  const { data: list, error, loading, reload } = useAsync(() => api.getMyReservations(), []);

  return (
    <section>
      <h1 className="page-title">마이페이지</h1>
      <dl className="profile">
        <div>
          <dt>이름</dt>
          <dd>{member.name}</dd>
        </div>
        <div>
          <dt>아이디</dt>
          <dd>{member.login_id}</dd>
        </div>
        <div>
          <dt>생년월일</dt>
          <dd>{formatDate(member.birth_date)}</dd>
        </div>
      </dl>

      <h2 className="section-title">
        예매 내역 {list && <span className="count">{list.length}건</span>}
      </h2>
      {loading && <Loading />}
      {error && <ErrorBox error={error} onRetry={reload} />}
      {list && list.length === 0 && (
        <div className="empty">
          <p>예매 내역이 없습니다.</p>
          <Link to="/movies" className="btn btn-primary">
            상영작 보기
          </Link>
        </div>
      )}
      {list && list.length > 0 && (
        <ul className="ticket-list">
          {list.map((r) => (
            <li key={r.reservation_id}>
              <ReservationCard reservation={r} />
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
