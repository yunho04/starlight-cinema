-- 별빛시네마: 선택 사항인 스키마 변경안 (아직 적용하지 않음)
-- 현재 백엔드는 이 변경 없이 동작한다. DB팀과 합의한 경우에만 실행할 것.
-- 적용 후에는 Movie 엔티티에 releaseDate, posterUrl 필드를 추가하고
-- MovieResponse.from()에서 null 대신 해당 값을 쓰도록 바꿔야 한다.
-- (ddl-auto=validate 이므로 컬럼을 추가하는 것만으로 기존 백엔드가 깨지지는 않는다.)

USE movie_booking;

-- 1) 프론트의 개봉일/포스터 표시용 컬럼 (현재: release_date=null → 화면에 '미정', poster_url은 /posters/{id}.svg로 계산)
ALTER TABLE movies
    ADD COLUMN release_date DATE NULL AFTER runtime_minutes,
    ADD COLUMN poster_url VARCHAR(500) NULL AFTER release_date;

UPDATE movies SET release_date = '2026-10-22', poster_url = '/posters/1.svg' WHERE movie_id = 1;
UPDATE movies SET release_date = '2026-11-05', poster_url = '/posters/2.svg' WHERE movie_id = 2;
UPDATE movies SET release_date = '2026-10-29', poster_url = '/posters/3.svg' WHERE movie_id = 3;

-- 2) (선택) 청소년 관람불가 표기를 '18'에서 '19'로 통일하려면
--    현재 백엔드는 DB '18' ↔ API '19'로 변환하고 있으므로 이 변경은 필요 없다.
--    적용한다면 AgeRating.RESTRICTED의 dbValue와 Movie.ageRating의 columnDefinition도 '19'로 바꿔야 한다.
-- ALTER TABLE movies MODIFY age_rating ENUM('ALL','12','15','18','19') NOT NULL;
-- UPDATE movies SET age_rating = '19' WHERE age_rating = '18';
-- ALTER TABLE movies MODIFY age_rating ENUM('ALL','12','15','19') NOT NULL;
