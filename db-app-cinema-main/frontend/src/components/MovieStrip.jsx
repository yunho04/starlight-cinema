import Poster from './Poster.jsx';
import RatingBadge from './RatingBadge.jsx';

// 예매 화면 상단: 작은 포스터 + 제목 + 부가 정보
export default function MovieStrip({ movie, children, action }) {
  return (
    <div className="movie-strip">
      <Poster movie={movie} className="poster-thumb" />
      <div>
        <h1 className="movie-strip-title">
          <RatingBadge rating={movie.age_rating} />
          {movie.title}
        </h1>
        {children && <div className="movie-strip-meta">{children}</div>}
        {action && <div className="movie-strip-action">{action}</div>}
      </div>
    </div>
  );
}
