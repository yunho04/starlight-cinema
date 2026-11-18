package com.cinema.cinema_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long seatId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @Column(name = "seat_row", nullable = false, length = 3)
    private String seatRow;

    @Column(name = "seat_no", nullable = false, columnDefinition = "smallint unsigned")
    private Integer seatNo;

    public Long getScreenId() {
        return screen.getScreenId();
    }
}
