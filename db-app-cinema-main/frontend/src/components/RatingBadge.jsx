import { RATING_LABEL } from '../lib/age.js';

export default function RatingBadge({ rating, withLabel = false }) {
  const badge = (
    <span className={`rating-badge rating-${rating}`} title={RATING_LABEL[rating]} aria-label={RATING_LABEL[rating]}>
      {rating === 'ALL' ? '전체' : rating}
    </span>
  );
  if (!withLabel) return badge;
  return (
    <span className="rating">
      {badge}
      <span>{RATING_LABEL[rating]}</span>
    </span>
  );
}
