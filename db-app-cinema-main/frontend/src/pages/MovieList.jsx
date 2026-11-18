import { Link } from 'react-router-dom';
import { api } from '../api/client.js';
import { useAsync } from '../lib/useAsync.js';
import { formatDate } from '../lib/datetime.js';
import Poster from '../components/Poster.jsx';
import RatingBadge from '../components/RatingBadge.jsx';
import { ErrorBox, Loading } from '../components/Status.jsx';

export default function MovieList() {
  const { data: movies, error, loading, reload } = useAsync(() => api.getMovies(), []);

  return (
    <section>
      <h1 className="page-title">현재 상영작</h1>
      {loading && <Loading />}
      {error && <ErrorBox error={error} onRetry={reload} />}
      {movies && (
        <ul className="movie-grid">
          {movies.map((m) => (
            <li key={m.movie_id} className="movie-item">
              <Link to={`/movies/${m.movie_id}`} className="movie-poster-link" aria-label={`${m.title} 상세 정보`}>
                <Poster movie={m} />
              </Link>
              <h2 className="movie-name">
                <RatingBadge rating={m.age_rating} />
                <Link to={`/movies/${m.movie_id}`}>{m.title}</Link>
              </h2>
              <p className="movie-sub">
                <span>{m.running_time}분</span>
                {m.release_date && <span>{formatDate(m.release_date)} 개봉</span>}
              </p>
              <Link to={`/movies/${m.movie_id}/schedules`} className="btn btn-outline btn-block">
                예매
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
