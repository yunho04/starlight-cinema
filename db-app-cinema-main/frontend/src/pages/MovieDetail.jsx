import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client.js';
import { useAsync } from '../lib/useAsync.js';
import { formatDate, formatRunningTime } from '../lib/datetime.js';
import { RATING_LABEL } from '../lib/age.js';
import Poster from '../components/Poster.jsx';
import RatingBadge from '../components/RatingBadge.jsx';
import { ErrorBox, Loading } from '../components/Status.jsx';

export default function MovieDetail() {
  const { movieId } = useParams();
  const { data: movie, error, loading, reload } = useAsync(() => api.getMovie(movieId), [movieId]);

  if (loading) return <Loading />;
  if (error) return <ErrorBox error={error} onRetry={reload} />;

  return (
    <article className="detail">
      <Poster movie={movie} className="detail-poster" />
      <div className="detail-body">
        <h1 className="detail-title">
          <RatingBadge rating={movie.age_rating} />
          {movie.title}
        </h1>
        <table className="info-table">
          <tbody>
            <tr>
              <th scope="row">관람등급</th>
              <td>{RATING_LABEL[movie.age_rating]}</td>
            </tr>
            <tr>
              <th scope="row">상영시간</th>
              <td>{formatRunningTime(movie.running_time)}</td>
            </tr>
            <tr>
              <th scope="row">개봉일</th>
              <td>{movie.release_date ? formatDate(movie.release_date) : '미정'}</td>
            </tr>
          </tbody>
        </table>
        <div className="detail-actions">
          <Link to={`/movies/${movie.movie_id}/schedules`} className="btn btn-primary btn-lg">
            예매하기
          </Link>
          <Link to="/movies" className="btn btn-outline btn-lg">
            목록으로
          </Link>
        </div>
        {movie.description && (
          <>
            <h2 className="sub-title">줄거리</h2>
            <p className="synopsis">{movie.description}</p>
          </>
        )}
      </div>
    </article>
  );
}
