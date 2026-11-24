package bloodplus.repository;

import bloodplus.domain.BenefitQuery;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * 프로그램이 실행하는 모든 SQL 을 모아 둔 곳.
 * JdbcBloodRepository 와 JdbcTemplateBloodRepository 가 같이 쓴다.
 *
 * ★ 팀장이 만든 DB 의 테이블, 컬럼, 프로시저 이름에 맞춰 이 파일만 고치면 된다.
 *   (SELECT 결과의 컬럼 이름은 Mappers.java 가 읽으므로, 다르게 바꾸면 AS 로 별칭을 맞춘다.)
 */
public final class Sql {
    private Sql() {}

    // ------------------------------------------------------------ 회원 MEMBER
    static final String MEMBER_COLS = "member_id, name, phone, address";
    public static final String LOGIN = "SELECT " + MEMBER_COLS + " FROM MEMBER WHERE member_id = ? AND password = ?";
    public static final String FIND_MEMBER = "SELECT " + MEMBER_COLS + " FROM MEMBER WHERE member_id = ?";
    public static final String INSERT_MEMBER = "INSERT INTO MEMBER (member_id, password, name, phone, address) VALUES (?, ?, ?, ?, ?)";
    public static final String UPDATE_MEMBER = "UPDATE MEMBER SET name = ?, phone = ?, address = ? WHERE member_id = ?";
    public static final String UPDATE_MEMBER_WITH_PASSWORD = "UPDATE MEMBER SET name = ?, phone = ?, address = ?, password = ? WHERE member_id = ?";
    /** 탈퇴: 저장 프로시저 호출 */
    public static final String CALL_DELETE_MEMBER = "{call SP_DELETE_MEMBER(?)}";

    // ------------------------------------------------------------ 헌혈 이력 DONATION
    static final String DONATION_COLS = "donation_no, member_id, donation_date, donation_type, donation_region";
    public static final String FIND_DONATIONS = "SELECT " + DONATION_COLS + " FROM DONATION WHERE member_id = ? ORDER BY donation_date DESC, donation_no DESC";
    public static final String FIND_DONATION = "SELECT " + DONATION_COLS + " FROM DONATION WHERE donation_no = ? AND member_id = ?";
    public static final String INSERT_DONATION = "INSERT INTO DONATION (" + DONATION_COLS + ") VALUES (SEQ_DONATION.NEXTVAL, ?, ?, ?, ?)";
    public static final String UPDATE_DONATION = "UPDATE DONATION SET donation_date = ?, donation_type = ?, donation_region = ? WHERE donation_no = ? AND member_id = ?";
    public static final String DELETE_DONATION = "DELETE FROM DONATION WHERE donation_no = ? AND member_id = ?";
    /** 헌혈 종류별 횟수 (한 번의 쿼리로 모든 혜택 조건 계산에 사용) */
    public static final String COUNT_DONATIONS_BY_TYPE = "SELECT donation_type, COUNT(*) AS cnt FROM DONATION WHERE member_id = ? GROUP BY donation_type";

    // ------------------------------------------------------------ 혜택 BENEFIT
    static final String BENEFIT_COLS = "benefit_no, benefit_name, required_count, apply_start, apply_end, gift_type, target_type, target_region";
    public static final String FIND_BENEFIT = "SELECT " + BENEFIT_COLS + " FROM BENEFIT WHERE benefit_no = ?";

    /** 혜택 조건 검색: 입력된 조건만 WHERE 에 붙인다 */
    public static Statement searchBenefits(BenefitQuery q) {
        StringBuilder sql = new StringBuilder("SELECT " + BENEFIT_COLS + " FROM BENEFIT WHERE benefit_name LIKE ?");
        List<Object> params = new ArrayList<>();
        params.add("%" + q.getKeyword() + "%");
        if (q.getGiftType() != null) { sql.append(" AND gift_type = ?"); params.add(q.getGiftType()); }
        if (q.getDonationType() != null) { sql.append(" AND (target_type IS NULL OR target_type = ?)"); params.add(q.getDonationType()); }
        if (q.getRegion() != null) { sql.append(" AND (target_region IS NULL OR target_region = ?)"); params.add(q.getRegion()); }
        if (q.getOnDate() != null) { sql.append(" AND (apply_start IS NULL OR ? BETWEEN apply_start AND apply_end)"); params.add(Date.valueOf(q.getOnDate())); }
        sql.append(" ORDER BY benefit_no");
        return new Statement(sql.toString(), params.toArray());
    }

    // ------------------------------------------------------------ ① 저장 프로시저 (④ CallableStatement)
    /** IN: 회원 아이디, OUT: SYS_REFCURSOR (BENEFIT_COLS + my_count) */
    public static final String CALL_FIND_ELIGIBLE = "{call SP_FIND_ELIGIBLE_BENEFITS(?, ?)}";

    // ------------------------------------------------------------ 혜택 지급 BENEFIT_GRANT (⑤ → ② 트리거)
    public static final String INSERT_GRANT = "INSERT INTO BENEFIT_GRANT (grant_no, member_id, benefit_no, grant_date, grant_status) VALUES (SEQ_GRANT.NEXTVAL, ?, ?, SYSDATE, '지급 대기')";
    public static final String USE_GRANT = "UPDATE BENEFIT_GRANT SET grant_status = '사용 완료' WHERE grant_no = ? AND member_id = ?";
    public static final String DELETE_GRANT = "DELETE FROM BENEFIT_GRANT WHERE grant_no = ? AND member_id = ?";
    public static final String FIND_GRANTS =
            "SELECT g.grant_no, g.member_id, g.benefit_no, g.grant_date, g.grant_status, b.benefit_name, b.gift_type "
          + "FROM BENEFIT_GRANT g JOIN BENEFIT b ON g.benefit_no = b.benefit_no "
          + "WHERE g.member_id = ? ORDER BY g.grant_date DESC, g.grant_no DESC";

    // ------------------------------------------------------------ 혜택 사용 이력 BENEFIT_USAGE (트리거가 기록)
    public static final String FIND_USAGES =
            "SELECT u.usage_no, u.member_id, u.benefit_no, u.usage_date, b.benefit_name, b.gift_type "
          + "FROM BENEFIT_USAGE u JOIN BENEFIT b ON u.benefit_no = b.benefit_no "
          + "WHERE u.member_id = ? ORDER BY u.usage_date DESC, u.usage_no DESC";

    /** SQL 문장과 ? 에 들어갈 값 */
    public static final class Statement {
        public final String sql;
        public final Object[] params;
        Statement(String sql, Object[] params) { this.sql = sql; this.params = params; }
    }
}
