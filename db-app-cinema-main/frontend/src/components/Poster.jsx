import { useState } from 'react';

// poster_url이 없거나 이미지를 못 불러오면 제목으로 대체 포스터를 그린다.
export default function Poster({ movie, className = '' }) {
  const [broken, setBroken] = useState(false);
  if (!movie.poster_url || broken) {
    return (
      <div className={`poster poster-fallback ${className}`} role="img" aria-label={`${movie.title} 포스터`}>
        <span>{movie.title}</span>
      </div>
    );
  }
  return (
    <img
      className={`poster ${className}`}
      src={movie.poster_url}
      alt={`${movie.title} 포스터`}
      onError={() => setBroken(true)}
    />
  );
}
