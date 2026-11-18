package com.cinema.cinema_backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

// 실제 DB 접속 정보(DB_USERNAME 등)가 있을 때만 실행. 조회와 스키마 검증(validate)만 하므로 데이터는 바뀌지 않는다.
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
class CinemaBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
