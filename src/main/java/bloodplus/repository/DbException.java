package bloodplus.repository;

import java.sql.SQLException;

/**
 * DB 오류. getMessage() 는 화면에 그대로 보여줄 쉬운 문장이다.
 * 원래 오류(ORA 코드)는 SqlLog 와 서버 로그에 남는다.
 */
public class DbException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DbException(String userMessage) { super(userMessage); }
    public DbException(String userMessage, Throwable cause) { super(userMessage, cause); }

    /** 기본키, UNIQUE 제약 위반 (ORA-00001) */
    public static boolean isDuplicate(DbException e) {
        return e.getCause() instanceof SQLException se && se.getErrorCode() == 1;
    }

    /**
     * Oracle 오류를 화면용 문장으로 바꾼다.
     * 트리거의 RAISE_APPLICATION_ERROR(-200xx, '메시지') 는 그 메시지를 그대로 쓴다.
     */
    public static DbException fromOracle(SQLException e, String fallback) {
        int code = e.getErrorCode();
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (code >= 20000 && code <= 20999) {
            String clean = msg.replaceFirst("(?s)^ORA-\\d+:\\s*", "");
            int cut = clean.indexOf("\nORA-");
            if (cut > 0) clean = clean.substring(0, cut);
            return new DbException(clean.trim(), e);
        }
        switch (code) {
            case 1:     return new DbException("이미 등록된 정보입니다.", e);                       // UNIQUE, PK 위반
            case 1400:  return new DbException("빠진 입력값이 있습니다.", e);                        // NOT NULL 위반
            case 2290:  return new DbException("입력값이 허용 범위를 벗어났습니다.", e);              // CHECK 위반
            case 2292:  return new DbException("연결된 기록이 있어 삭제할 수 없습니다.", e);          // 자식 레코드 존재
            case 12899: return new DbException("입력한 내용이 너무 깁니다.", e);                   // 컬럼 길이 초과
            case 17002: case 12505: case 12514: case 12541:
                return new DbException("지금은 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해 주시기 바랍니다.", e);
            default:    return new DbException(fallback, e);
        }
    }

    /** JdbcTemplate 은 SQLException 을 DataAccessException 으로 감싸므로 안쪽 SQLException 을 찾아 꺼낸다 */
    public static DbException from(Throwable t, String fallback) {
        for (Throwable c = t; c != null; c = c.getCause())
            if (c instanceof SQLException se) return fromOracle(se, fallback);
        return new DbException(fallback, t);
    }
}
