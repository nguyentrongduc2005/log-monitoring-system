package com.vdt.log_monitoring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.clickhouse.migration.enabled=false")
class LogMonitoringApplicationTests {

	@Test
	void contextLoads() {
	}

}
