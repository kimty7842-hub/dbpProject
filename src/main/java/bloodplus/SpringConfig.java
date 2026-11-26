package bloodplus;

import bloodplus.repository.BloodRepository;
import bloodplus.repository.JdbcBloodRepository;
import bloodplus.repository.JdbcTemplateBloodRepository;
import bloodplus.repository.MemoryBloodRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 저장소(Repository) 구현체를 고르는 곳. (교수님 예제의 SpringConfig 와 같은 역할, 다형성 활용)
 * application.properties 의 app.repository 값만 바꾸면 화면, 서비스 코드는 그대로 두고 저장소가 바뀐다.
 */
@Configuration
public class SpringConfig {

    private final DataSource dataSource;

    @Value("${app.repository:memory}")
    private String repositoryType;

    public SpringConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    public BloodRepository bloodRepository() {
//        switch (repositoryType.trim().toLowerCase()) {
//            case "jdbc":
//                return new JdbcBloodRepository(dataSource);
//            case "jdbctemplate":
//                return new JdbcTemplateBloodRepository(dataSource);
//            default:
//                return new MemoryBloodRepository();
//        }
        // 교수님 예제 방식으로 쓰려면 위 switch 대신 아래 중 하나만 남겨도 된다.
        // return new MemoryBloodRepository();
        // return new JdbcBloodRepository(dataSource);
         return new JdbcTemplateBloodRepository(dataSource);
    }
}
