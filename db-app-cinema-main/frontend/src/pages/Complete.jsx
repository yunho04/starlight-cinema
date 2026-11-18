import { Link, useLocation, useParams } from 'react-router-dom';
import { api } from '../api/client.js';
import { useAsync } from '../lib/useAsync.js';
import StepBar from '../components/StepBar.jsx';
import { ErrorBox, Loading } from '../components/Status.jsx';
import ReservationCard from './ReservationCard.jsx';

export default function Complete() {
  const { reservationId } = useParams();
  const { state } = useLocation();
  const passed = state?.reservation;

  // 새로고침으로 state가 사라졌으면 내 예매 목록에서 다시 찾는다
  const { data, error, loading, reload } = useAsync(
    () =>
      passed
        ? Promise.resolve(passed)
        : api.getMyReservations().then((list) => {
            const found = list.find((r) => String(r.reservation_id) === reservationId);
            if (!found) throw new Error('예매 내역을 찾을 수 없습니다.');
            return found;
          }),
    [reservationId],
  );

  return (
    <section className="complete">
      <StepBar current={3} />
      {loading && <Loading />}
      {error && <ErrorBox error={error} onRetry={reload} />}
      {data && (
        <>
          <h1 className="page-title">예매가 완료되었습니다</h1>
          <p className="complete-note">상영 시작 전까지 마이페이지에서 다시 확인할 수 있습니다.</p>
          <ReservationCard reservation={data} />
          <div className="actions">
            <Link to="/mypage" className="btn btn-primary">
              마이페이지에서 보기
            </Link>
            <Link to="/movies" className="btn btn-outline">
              다른 영화 보기
            </Link>
          </div>
        </>
      )}
    </section>
  );
}
