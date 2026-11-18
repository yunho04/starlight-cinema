import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api } from '../api/client.js';
import { useAuth } from '../auth.jsx';
import { useAsync } from '../lib/useAsync.js';
import { checkAge } from '../lib/age.js';
import { formatTime, formatWon, parseDateTime } from '../lib/datetime.js';
import StepBar from '../components/StepBar.jsx';
import MovieStrip from '../components/MovieStrip.jsx';
import { ErrorBox, Loading } from '../components/Status.jsx';

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];

export default function ScheduleSelect() {
  const { movieId } = useParams();
  const { member } = useAuth();
  const navigate = useNavigate();
  const { data, error, loading, reload } = useAsync(
    () => Promise.all([api.getMovie(movieId), api.getSchedules(movieId)]),
    [movieId],
  );
  const [day, setDay] = useState(null);
  useEffect(() => setDay(null), [movieId]); // 다른 영화로 바뀌면 첫 날짜부터

  const [movie, schedules] = data ?? [null, []];

  const days = useMemo(() => {
    const map = new Map();
    for (const s of schedules) {
      const key = s.start_time.slice(0, 10);
      if (!map.has(key)) map.set(key, []);
      map.get(key).push(s);
    }
    return [...map.entries()];
  }, [schedules]);

  if (loading) return <Loading />;
  if (error) return <ErrorBox error={error} onRetry={reload} />;

  const activeDay = day ?? days[0]?.[0];
  const list = days.find(([d]) => d === activeDay)?.[1] ?? [];
  const now = new Date();
  const items = list.map((s) => {
    const started = parseDateTime(s.start_time) <= now;
    const age = checkAge(member, movie.age_rating, s.start_time);
    return { s, started, age };
  });
  const blocked = items.find((r) => !r.started && !r.age.allowed);

  // 상영관별로 묶기
  const byScreen = new Map();
  for (const it of items) {
    if (!byScreen.has(it.s.screen_name)) byScreen.set(it.s.screen_name, []);
    byScreen.get(it.s.screen_name).push(it);
  }

  return (
    <section>
      <StepBar current={0} />
      <MovieStrip movie={movie}>
        <span>{movie.running_time}분</span>
      </MovieStrip>

      {days.length === 0 ? (
        <p className="empty">예정된 상영이 없습니다.</p>
      ) : (
        <>
          <div className="date-row" role="tablist" aria-label="상영일">
            {days.map(([d]) => {
              const date = parseDateTime(`${d}T00:00:00`);
              const wd = date.getDay();
              return (
                <button
                  key={d}
                  type="button"
                  role="tab"
                  aria-selected={d === activeDay}
                  className={`date-cell ${d === activeDay ? 'active' : ''} ${wd === 0 ? 'sun' : wd === 6 ? 'sat' : ''}`}
                  onClick={() => setDay(d)}
                >
                  <span className="date-month">{date.getMonth() + 1}월</span>
                  <strong>{date.getDate()}</strong>
                  <span>{WEEKDAYS[wd]}</span>
                </button>
              );
            })}
          </div>

          {blocked && (
            <p className="notice notice-warn" role="status">
              {blocked.age.message}
            </p>
          )}

          {[...byScreen.entries()].map(([screenName, rows]) => (
            <div className="screen-group" key={screenName}>
              <h2 className="screen-name">{screenName}</h2>
              <ul className="time-grid">
                {rows.map(({ s, started, age }) => (
                  <li key={s.schedule_id}>
                    <button
                      type="button"
                      className="time-cell"
                      disabled={started || !age.allowed}
                      onClick={() => navigate(`/schedules/${s.schedule_id}/seats`)}
                    >
                      <strong>{formatTime(s.start_time)}</strong>
                      <span className="time-end">~{formatTime(s.end_time)}</span>
                      <span className="time-foot">
                        {started ? '상영 시작' : !age.allowed ? '관람 불가' : formatWon(s.price)}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </>
      )}
    </section>
  );
}
