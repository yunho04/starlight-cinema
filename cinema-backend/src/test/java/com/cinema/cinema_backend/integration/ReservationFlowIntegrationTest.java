package com.cinema.cinema_backend.integration;

import com.cinema.cinema_backend.dto.ReservationRequest;
import com.cinema.cinema_backend.exception.SeatAlreadyReservedException;
import com.cinema.cinema_backend.service.ReservationService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 MySQL에 회원·예매 데이터를 INSERT/UPDATE 하는 통합 테스트.
 * 운영/시연용 DB를 더럽히지 않도록 CINEMA_IT=true 를 명시했을 때만 실행된다.
 * schema.sql + seed.sql 을 적용한 별도 테스트 DB(DB_URL)에서 실행할 것.
 * 같은 DB에서 여러 번 실행해도 되도록 매번 새 회원을 만들고 비어 있는 좌석만 고른다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "CINEMA_IT", matches = "true")
class ReservationFlowIntegrationTest {

    private static final String PASSWORD = "password1234";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ReservationService reservationService;

    private String runId;

    @BeforeEach
    void setUp() {
        runId = Long.toString(System.currentTimeMillis() % 100_000_000L, 36) + SEQ.incrementAndGet();
    }

    // ---------------------------------------------------------------- 회원

    @Test
    void 회원가입_로그인_내정보_조회() throws Exception {
        String loginId = "it_" + runId;
        String body = signupJson(loginId, "1999-01-01", uniquePhone());

        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.login_id").value(loginId))
                .andExpect(jsonPath("$.birth_date").value("1999-01-01"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.password_hash").doesNotExist());

        // 같은 아이디 재가입 → 409
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(loginId, "1999-01-01", uniquePhone())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_LOGIN_ID"));

        String token = login(loginId, PASSWORD);

        mvc.perform(get("/api/members/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login_id").value(loginId))
                .andExpect(jsonPath("$.name").value("테스트"));
    }

    @Test
    void 잘못된_비밀번호와_없는_아이디는_401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login_id\":\"adult01\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 맞지 않습니다."));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login_id\":\"no_such_user_" + runId + "\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void seed_테스트계정으로_로그인된다() throws Exception {
        String token = login("adult01", "test1234");
        mvc.perform(get("/api/members/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login_id").value("adult01"))
                .andExpect(jsonPath("$.birth_date").value("1999-03-14"));
    }

    @Test
    void 회원가입_입력값_검증() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("x", "1999-01-01", uniquePhone())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 토큰이_없거나_잘못되면_401() throws Exception {
        mvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/members/me/reservations").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schedule_id\":1,\"seat_ids\":[1]}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- 영화 / 상영회차

    @Test
    void 영화와_상영회차_좌석_조회() throws Exception {
        mvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].movie_id").value(1))
                .andExpect(jsonPath("$[0].running_time").value(108))
                .andExpect(jsonPath("$[2].age_rating").value("19"));

        mvc.perform(get("/api/movies/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("붉은 장막"))
                .andExpect(jsonPath("$.poster_url").value("/posters/3.svg"));

        mvc.perform(get("/api/movies/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        long scheduleId = futureScheduleId(1);
        String seats = mvc.perform(get("/api/schedules/" + scheduleId + "/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedule.schedule_id").value(scheduleId))
                .andExpect(jsonPath("$.schedule.screen_name").value("1관"))
                .andExpect(jsonPath("$.seats.length()").value(96))
                .andExpect(jsonPath("$.seats[0].seat_row").value("A"))
                .andExpect(jsonPath("$.seats[0].seat_col").value(1))
                .andReturn().getResponse().getContentAsString();
        assertThat((List<Object>) JsonPath.read(seats, "$.seats[*].reserved")).isNotEmpty();

        mvc.perform(get("/api/schedules/999999/seats"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- 예매

    @Test
    void 여러좌석_예매_후_좌석이_예매됨으로_바뀌고_마이페이지에_보인다() throws Exception {
        String token = newMemberToken("1990-01-01");
        long scheduleId = futureScheduleId(2);
        List<Integer> seatIds = freeSeats(scheduleId, 3);
        int price = JsonPath.read(seatMap(scheduleId), "$.schedule.price");

        String created = mvc.perform(auth(post("/api/reservations"), token)
                        .content(reservationJson(scheduleId, seatIds)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.total_price").value(price * 3))
                .andExpect(jsonPath("$.tickets.length()").value(3))
                .andExpect(jsonPath("$.schedule.schedule_id").value(scheduleId))
                .andReturn().getResponse().getContentAsString();
        int reservationId = JsonPath.read(created, "$.reservation_id");

        List<Integer> reserved = JsonPath.read(seatMap(scheduleId), "$.seats[?(@.reserved == true)].seat_id");
        assertThat(reserved).containsAll(seatIds);

        mvc.perform(auth(get("/api/members/me/reservations"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reservation_id").value(reservationId))
                .andExpect(jsonPath("$[0].tickets.length()").value(3));

        mvc.perform(auth(get("/api/reservations/" + reservationId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_price").value(price * 3));
    }

    @Test
    void 같은_좌석_중복예매는_409와_겹친_좌석번호() throws Exception {
        String first = newMemberToken("1990-01-01");
        String second = newMemberToken("1990-01-01");
        long scheduleId = futureScheduleId(1);
        List<Integer> seats = freeSeats(scheduleId, 3);

        mvc.perform(auth(post("/api/reservations"), first).content(reservationJson(scheduleId, seats.subList(0, 2))))
                .andExpect(status().isCreated());

        // 두 번째 사람: 겹치는 1석 + 빈 1석 → 전체 실패
        mvc.perform(auth(post("/api/reservations"), second)
                        .content(reservationJson(scheduleId, List.of(seats.get(1), seats.get(2)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SEAT_ALREADY_RESERVED"))
                .andExpect(jsonPath("$.seat_ids.length()").value(1))
                .andExpect(jsonPath("$.seat_ids[0]").value(seats.get(1)));

        // 실패한 요청의 빈 좌석은 예매되지 않았어야 한다 (원자성)
        List<Integer> reserved = JsonPath.read(seatMap(scheduleId), "$.seats[?(@.reserved == true)].seat_id");
        assertThat(reserved).doesNotContain(seats.get(2));
    }

    @Test
    void 좌석_수와_좌석_소속_검증() throws Exception {
        String token = newMemberToken("1990-01-01");
        long scheduleId = futureScheduleId(1);
        List<Integer> nine = freeSeats(scheduleId, 9);

        mvc.perform(auth(post("/api/reservations"), token).content(reservationJson(scheduleId, nine)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mvc.perform(auth(post("/api/reservations"), token).content(reservationJson(scheduleId, List.of())))
                .andExpect(status().isBadRequest());

        mvc.perform(auth(post("/api/reservations"), token)
                        .content(reservationJson(scheduleId, List.of(nine.get(0), nine.get(0)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SEAT"));

        // 1관 회차에 IMAX관 좌석(326) 요청
        mvc.perform(auth(post("/api/reservations"), token).content(reservationJson(scheduleId, List.of(326))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SEAT"));

        mvc.perform(auth(post("/api/reservations"), token).content(reservationJson(999999, List.of(1))))
                .andExpect(status().isNotFound());
    }

    @Test
    void 연령제한_위반은_403() throws Exception {
        String teen = login("teen01", "test1234");
        long adultOnly = futureScheduleId(3);  // 청소년 관람불가
        long fifteen = futureScheduleId(2);    // 15세
        long all = futureScheduleId(1);        // 전체 관람가

        mvc.perform(auth(post("/api/reservations"), teen).content(reservationJson(adultOnly, freeSeats(adultOnly, 1))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AGE_RESTRICTED"));
        mvc.perform(auth(post("/api/reservations"), teen).content(reservationJson(fifteen, freeSeats(fifteen, 1))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AGE_RESTRICTED"));

        // 만 19세가 되기 하루 전인 회원도 막힌다
        LocalDateTime start = LocalDateTime.parse(JsonPath.read(seatMap(adultOnly), "$.schedule.start_time"));
        String almost19 = newMemberToken(start.toLocalDate().minusYears(19).plusDays(1).toString());
        mvc.perform(auth(post("/api/reservations"), almost19).content(reservationJson(adultOnly, freeSeats(adultOnly, 1))))
                .andExpect(status().isForbidden());

        // 전체 관람가는 예매된다
        String created = mvc.perform(auth(post("/api/reservations"), teen).content(reservationJson(all, freeSeats(all, 1))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        cancel(teen, JsonPath.read(created, "$.reservation_id"));
    }

    @Test
    void 이미_시작한_회차는_예매할_수_없다() throws Exception {
        String schedules = mvc.perform(get("/api/movies/1/schedules")).andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> list = JsonPath.read(schedules, "$");
        Integer started = list.stream()
                .filter(s -> !LocalDateTime.parse((String) s.get("start_time")).isAfter(LocalDateTime.now(KST)))
                .map(s -> (Integer) s.get("schedule_id"))
                .findFirst().orElse(null);
        assumeThat(started).as("오늘 이미 시작한 회차가 있어야 검증 가능").isNotNull();

        String token = newMemberToken("1990-01-01");
        mvc.perform(auth(post("/api/reservations"), token).content(reservationJson(started, freeSeats(started, 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SCHEDULE_STARTED"));
    }

    @Test
    void 다른_사용자의_예매는_조회_취소_불가() throws Exception {
        String owner = newMemberToken("1990-01-01");
        String other = newMemberToken("1990-01-01");
        long scheduleId = futureScheduleId(1);

        String created = mvc.perform(auth(post("/api/reservations"), owner)
                        .content(reservationJson(scheduleId, freeSeats(scheduleId, 1))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int reservationId = JsonPath.read(created, "$.reservation_id");

        mvc.perform(auth(get("/api/reservations/" + reservationId), other))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(auth(post("/api/reservations/" + reservationId + "/cancel"), other))
                .andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/members/me/reservations"), other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // 소유자의 예매는 그대로 유지
        mvc.perform(auth(get("/api/reservations/" + reservationId), owner))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void 예매취소_후_같은_좌석을_다시_예매할_수_있다() throws Exception {
        String first = newMemberToken("1990-01-01");
        String second = newMemberToken("1990-01-01");
        long scheduleId = futureScheduleId(3);
        List<Integer> seats = freeSeats(scheduleId, 2);

        String created = mvc.perform(auth(post("/api/reservations"), first).content(reservationJson(scheduleId, seats)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int reservationId = JsonPath.read(created, "$.reservation_id");

        mvc.perform(auth(post("/api/reservations/" + reservationId + "/cancel"), first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.tickets[0].status").value("CANCELLED"));

        // 두 번 취소 → 409
        mvc.perform(auth(post("/api/reservations/" + reservationId + "/cancel"), first))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_CANCELLED"));

        List<Integer> reserved = JsonPath.read(seatMap(scheduleId), "$.seats[?(@.reserved == true)].seat_id");
        assertThat(reserved).doesNotContainAnyElementsOf(seats);

        // 다른 회원이 같은 좌석 재예매
        mvc.perform(auth(post("/api/reservations"), second).content(reservationJson(scheduleId, seats)))
                .andExpect(status().isCreated());

        // 취소한 회원의 마이페이지에는 CANCELLED로 남는다
        mvc.perform(auth(get("/api/members/me/reservations"), first))
                .andExpect(jsonPath("$[0].status").value("CANCELLED"));
    }

    @Test
    void 동시에_같은_좌석을_예매하면_한_명만_성공한다() throws Exception {
        int threads = 10;
        long scheduleId = futureScheduleId(2);
        List<Integer> seats = freeSeats(scheduleId, 3);
        List<Long> memberIds = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            memberIds.add(memberIdOf(newMemberToken("1990-01-01")));
        }

        // 모든 요청이 seats[1]을 포함하도록 겹치게 구성
        List<List<Long>> requests = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            requests.add(switch (i % 3) {
                case 0 -> List.of(seats.get(0).longValue(), seats.get(1).longValue());
                case 1 -> List.of(seats.get(1).longValue());
                default -> List.of(seats.get(1).longValue(), seats.get(2).longValue());
            });
        }

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Long memberId = memberIds.get(i);
            List<Long> seatIds = requests.get(i);
            Callable<String> task = () -> {
                ready.countDown();
                go.await();
                try {
                    reservationService.reserve(memberId, new ReservationRequest(scheduleId, seatIds));
                    return "OK";
                } catch (SeatAlreadyReservedException e) {
                    return "CONFLICT";
                }
            };
            results.add(pool.submit(task));
        }
        ready.await();
        go.countDown();

        List<String> outcomes = new ArrayList<>();
        for (Future<String> f : results) {
            outcomes.add(f.get());
        }
        pool.shutdown();

        assertThat(Collections.frequency(outcomes, "OK")).isEqualTo(1);
        assertThat(Collections.frequency(outcomes, "CONFLICT")).isEqualTo(threads - 1);

        // seats[1]의 ACTIVE 티켓은 정확히 1장
        List<Integer> reserved = JsonPath.read(seatMap(scheduleId), "$.seats[?(@.reserved == true)].seat_id");
        assertThat(reserved).contains(seats.get(1));
    }

    // ---------------------------------------------------------------- helpers

    private String signupJson(String loginId, String birthDate, String phone) {
        return """
                {"login_id":"%s","password":"%s","name":"테스트","phone":"%s","birth_date":"%s"}
                """.formatted(loginId, PASSWORD, phone, birthDate);
    }

    private String uniquePhone() {
        long n = (System.nanoTime() / 1000 + SEQ.incrementAndGet()) % 100_000_000L;
        return String.format("019-%04d-%04d", n / 10_000, n % 10_000);
    }

    private String newMemberToken(String birthDate) throws Exception {
        String loginId = "it_" + runId + "_" + SEQ.incrementAndGet();
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(loginId, birthDate, uniquePhone())))
                .andExpect(status().isCreated());
        return login(loginId, PASSWORD);
    }

    private String login(String loginId, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login_id\":\"%s\",\"password\":\"%s\"}".formatted(loginId, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private long memberIdOf(String token) throws Exception {
        String me = mvc.perform(auth(get("/api/members/me"), token)).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(me, "$.member_id")).longValue();
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private String reservationJson(long scheduleId, List<Integer> seatIds) {
        return "{\"schedule_id\":%d,\"seat_ids\":%s}".formatted(scheduleId, seatIds);
    }

    private void cancel(String token, int reservationId) throws Exception {
        mvc.perform(auth(post("/api/reservations/" + reservationId + "/cancel"), token))
                .andExpect(status().isOk());
    }

    /** 1시간 이상 남은 회차 중 빈 좌석이 가장 많은 회차 */
    private long futureScheduleId(long movieId) throws Exception {
        String body = mvc.perform(get("/api/movies/" + movieId + "/schedules"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> list = JsonPath.read(body, "$");
        LocalDateTime limit = LocalDateTime.now(KST).plusHours(1);
        List<Integer> candidates = list.stream()
                .filter(s -> LocalDateTime.parse((String) s.get("start_time")).isAfter(limit))
                .map(s -> (Integer) s.get("schedule_id"))
                .toList();
        assumeThat(candidates).as("영화 %d의 미래 상영회차가 있어야 함 (seed 실행일 기준 3일치)", movieId).isNotEmpty();

        long best = -1;
        int bestFree = -1;
        for (Integer id : candidates) {
            int free = ((List<?>) JsonPath.read(seatMap(id), "$.seats[?(@.reserved == false)]")).size();
            if (free > bestFree) {
                best = id;
                bestFree = free;
            }
        }
        return best;
    }

    private String seatMap(long scheduleId) throws Exception {
        return mvc.perform(get("/api/schedules/" + scheduleId + "/seats"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private List<Integer> freeSeats(long scheduleId, int count) throws Exception {
        List<Integer> free = JsonPath.read(seatMap(scheduleId), "$.seats[?(@.reserved == false)].seat_id");
        assumeThat(free.size()).as("빈 좌석이 충분해야 함").isGreaterThanOrEqualTo(count);
        return new ArrayList<>(free.subList(0, count));
    }
}
