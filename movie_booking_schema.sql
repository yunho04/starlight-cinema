-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS movie_booking
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE movie_booking;

-- DATETIME은 프로젝트에서 한국 시간으로 가정함.
SET time_zone = '+09:00';

-- 회원
CREATE TABLE members (
    member_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id   VARCHAR(80) NOT NULL,
    name VARCHAR(80) NOT NULL,
    birth_day VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (member_id),
    UNIQUE KEY uq_members_phone_number (phone_number),
    UNIQUE KEY uq_members_id (id)
) ENGINE=InnoDB;

-- 영화
CREATE TABLE movies (
    movie_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    age_rating ENUM('ALL', '12', '15', '18') NOT NULL,
    runtime_minutes SMALLINT UNSIGNED NOT NULL,
    description TEXT NULL,

    PRIMARY KEY (movie_id),
    CONSTRAINT ck_movies_runtime CHECK (runtime_minutes > 0)
) ENGINE=InnoDB;

-- 상영관
CREATE TABLE screens (
    screen_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    screen_name VARCHAR(50) NOT NULL,

    PRIMARY KEY (screen_id),
    UNIQUE KEY uq_screens_name (screen_name)
) ENGINE=InnoDB;

-- 좌석
CREATE TABLE seats (
    seat_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    screen_id BIGINT UNSIGNED NOT NULL,
    seat_row VARCHAR(3) NOT NULL,
    seat_no SMALLINT UNSIGNED NOT NULL,

    PRIMARY KEY (seat_id),
    -- 같은 상영관에 동일한 좌석 위치가 두 개 생기지 않도록 제한
    UNIQUE KEY uq_seats_position (screen_id, seat_row, seat_no),
    -- 아래 tickets 테이블에서 상영관-좌석 일치 여부를 확인할 때 사용
    UNIQUE KEY uq_seats_id_screen (seat_id, screen_id),
    CONSTRAINT ck_seats_number CHECK (seat_no > 0),
    CONSTRAINT ck_seats_row CHECK (CHAR_LENGTH(seat_row) > 0),
    CONSTRAINT fk_seats_screen FOREIGN KEY (screen_id)
        REFERENCES screens (screen_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB;

-- 상영회차
CREATE TABLE screenings (
    screening_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    movie_id BIGINT UNSIGNED NOT NULL,
    screen_id BIGINT UNSIGNED NOT NULL,
    starts_at DATETIME NOT NULL,
    ends_at DATETIME NOT NULL,
    price INT UNSIGNED NOT NULL,

    PRIMARY KEY (screening_id),
    UNIQUE KEY uq_screenings_id_screen (screening_id, screen_id),
    KEY idx_screenings_movie_start (movie_id, starts_at),
    KEY idx_screenings_screen_start (screen_id, starts_at),
    CONSTRAINT ck_screenings_time CHECK (ends_at > starts_at),
    CONSTRAINT fk_screenings_movie FOREIGN KEY (movie_id)
        REFERENCES movies (movie_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_screenings_screen FOREIGN KEY (screen_id)
        REFERENCES screens (screen_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB;

-- 예약
CREATE TABLE reservations (
    reservation_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id BIGINT UNSIGNED NOT NULL,
    screening_id BIGINT UNSIGNED NOT NULL,
    booked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_price INT UNSIGNED NOT NULL,
    reservation_status ENUM('CONFIRMED', 'CANCELLED')
        NOT NULL DEFAULT 'CONFIRMED',

    PRIMARY KEY (reservation_id),
    UNIQUE KEY uq_reservations_id_screening (reservation_id, screening_id),
    KEY idx_reservations_member_date (member_id, booked_at),
    KEY idx_reservations_screening (screening_id),
    CONSTRAINT fk_reservations_member FOREIGN KEY (member_id)
        REFERENCES members (member_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_reservations_screening FOREIGN KEY (screening_id)
        REFERENCES screenings (screening_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB;

-- 예약좌석(티켓)
CREATE TABLE tickets (
    reservation_id BIGINT UNSIGNED NOT NULL,
    seat_id BIGINT UNSIGNED NOT NULL,
    screening_id BIGINT UNSIGNED NOT NULL,
    screen_id BIGINT UNSIGNED NOT NULL,
    paid_price INT UNSIGNED NOT NULL,
    ticket_status ENUM('ACTIVE', 'CANCELLED') NOT NULL DEFAULT 'ACTIVE',

    -- 실제로 예약된 티켓만 좌석 번호를 가짐. 취소 시 NULL로 계산됨.
    -- 자동 계산되는 열이므로 INSERT할 때 입력하지 않습니다.
    active_seat_id BIGINT UNSIGNED GENERATED ALWAYS AS (
        CASE WHEN ticket_status = 'ACTIVE' THEN seat_id ELSE NULL END
    ) STORED,

    PRIMARY KEY (reservation_id, seat_id),
    -- 같은 회차의 같은 좌석은 ACTIVE 상태로 2번 예약할 수 없음.
    -- CANCELLED 티켓은 active_seat_id가 NULL이므로 재예매 가능.
    UNIQUE KEY uq_tickets_active_screening_seat
        (screening_id, active_seat_id),

    KEY idx_tickets_reservation_screening (reservation_id, screening_id),
    KEY idx_tickets_screening_screen (screening_id, screen_id),
    KEY idx_tickets_seat_screen (seat_id, screen_id),

    -- 티켓의 회차는 예약에 적힌 회차와 같아야 함.
    CONSTRAINT fk_tickets_reservation_screening
        FOREIGN KEY (reservation_id, screening_id)
        REFERENCES reservations (reservation_id, screening_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- 티켓의 상영관은 상영회차가 열리는 상영관이어야 함.
    CONSTRAINT fk_tickets_screening_screen
        FOREIGN KEY (screening_id, screen_id)
        REFERENCES screenings (screening_id, screen_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- 티켓의 좌석은 해당 상영관 소속이어야 함.
    CONSTRAINT fk_tickets_seat_screen
        FOREIGN KEY (seat_id, screen_id)
        REFERENCES seats (seat_id, screen_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB;
