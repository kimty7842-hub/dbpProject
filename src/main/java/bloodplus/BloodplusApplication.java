package bloodplus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 헌혈 이력 기반 혜택 통합 관리 시스템 (DBP 12팀)
 * 실행하면 내장 톰캣이 켜지고, 브라우저에서 http://localhost:8080 으로 접속한다.
 */
@SpringBootApplication
public class BloodplusApplication {
    public static void main(String[] args) {
        SpringApplication.run(BloodplusApplication.class, args);
    }
}
