package bloodplus.repository;

import bloodplus.domain.*;
import oracle.jdbc.OracleTypes;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.sql.DataSource;
import java.sql.Date;
import java.sql.ResultSet;
import java.util.*;

/**
 * 스프링 JdbcTemplate 구현체 (교수님 예제의 JdbcTemplateCustomerRepository 방식)
 * 연결 열고 닫기와 예외 변환을 JdbcTemplate 이 해 주므로 코드가 짧다.
 *   ③ jdbcTemplate.query / update  (내부적으로 PreparedStatement)
 *   ④ jdbcTemplate.execute("{call ...}", CallableStatementCallback) → 저장 프로시저 호출
 *   ⑤ jdbcTemplate.update 로 BENEFIT_GRANT 변경 → 트리거 자동 실행
 * SQL 문장은 Sql.java, 결과 변환은 Mappers.java 에 있다.
 */
public class JdbcTemplateBloodRepository implements BloodRepository {

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert memberInsert;

    public JdbcTemplateBloodRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        // 교수님 슬라이드처럼 회원가입은 SimpleJdbcInsert 로
        this.memberInsert = new SimpleJdbcInsert(jdbcTemplate).withTableName("MEMBER")
                .usingColumns("MEMBER_ID", "PASSWORD", "NAME", "PHONE", "ADDRESS");
    }

    // =========================================================== 회원 (③)
    @Override public Optional<Member> findMemberByIdAndPassword(String id, String password) {
        return first(query(Sql.LOGIN, Mappers::member, "로그인 중 문제가 생겼습니다.", id, password));
    }

    @Override public Optional<Member> findMemberById(String id) {
        return first(query(Sql.FIND_MEMBER, Mappers::member, "회원 정보를 불러오지 못했습니다.", id));
    }

    @Override public void saveMember(Member m) {
        SqlLog.change(Sql.INSERT_MEMBER + "   -- SimpleJdbcInsert");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("MEMBER_ID", m.getId());
        parameters.put("PASSWORD", m.getPassword());
        parameters.put("NAME", m.getName());
        parameters.put("PHONE", m.getPhone());
        parameters.put("ADDRESS", m.getAddress());
        try {
            memberInsert.execute(parameters);
            SqlLog.ok("COMMIT");
        } catch (DataAccessException e) {
            DbException d = fail(e, "가입하지 못했습니다.");
            if (DbException.isDuplicate(d)) throw new DbException("이미 사용 중인 아이디입니다.", d.getCause());
            throw d;
        }
    }

    @Override public void updateMember(Member m) {
        if (m.getPassword() != null)
            update(Sql.UPDATE_MEMBER_WITH_PASSWORD, "정보를 저장하지 못했습니다.", m.getName(), m.getPhone(), m.getAddress(), m.getPassword(), m.getId());
        else
            update(Sql.UPDATE_MEMBER, "정보를 저장하지 못했습니다.", m.getName(), m.getPhone(), m.getAddress(), m.getId());
    }

    @Override public void deleteMember(String id) {
        SqlLog.call(Sql.CALL_DELETE_MEMBER);
        try {
            jdbcTemplate.execute(Sql.CALL_DELETE_MEMBER, (CallableStatementCallback<Void>) cstmt -> {
                cstmt.setString(1, id);
                cstmt.execute();
                return null;
            });
            SqlLog.ok("COMMIT");
        } catch (DataAccessException e) { throw fail(e, "탈퇴 처리하지 못했습니다."); }
    }

    // =========================================================== 헌혈 이력 (③)
    @Override public List<Donation> findDonations(String memberId) {
        return query(Sql.FIND_DONATIONS, Mappers::donation, "헌혈 기록을 불러오지 못했습니다.", memberId);
    }

    @Override public Optional<Donation> findDonation(int no, String memberId) {
        return first(query(Sql.FIND_DONATION, Mappers::donation, "헌혈 기록을 불러오지 못했습니다.", no, memberId));
    }

    @Override public void saveDonation(Donation d) {
        update(Sql.INSERT_DONATION, "헌혈 기록을 등록하지 못했습니다.", d.getMemberId(), Date.valueOf(d.getDate()), d.getType(), d.getRegion());
    }

    @Override public void updateDonation(Donation d) {
        update(Sql.UPDATE_DONATION, "헌혈 기록을 수정하지 못했습니다.", Date.valueOf(d.getDate()), d.getType(), d.getRegion(), d.getNo(), d.getMemberId());
    }

    @Override public void deleteDonation(int no, String memberId) {
        update(Sql.DELETE_DONATION, "헌혈 기록을 삭제하지 못했습니다.", no, memberId);
    }

    @Override public Map<String, Integer> countDonationsByType(String memberId) {
        Map<String, Integer> map = new HashMap<>();
        query(Sql.COUNT_DONATIONS_BY_TYPE, rs -> map.put(rs.getString("donation_type"), rs.getInt("cnt")), "헌혈 횟수를 계산하지 못했습니다.", memberId);
        return map;
    }

    // =========================================================== 혜택 (③)
    @Override public List<Benefit> searchBenefits(BenefitQuery q) {
        Sql.Statement st = Sql.searchBenefits(q);
        return query(st.sql, Mappers::benefit, "혜택을 검색하지 못했습니다.", st.params);
    }

    @Override public Optional<Benefit> findBenefit(int benefitNo) {
        return first(query(Sql.FIND_BENEFIT, Mappers::benefit, "혜택 정보를 불러오지 못했습니다.", benefitNo));
    }

    // =========================================================== ④ CallableStatement → ① 저장 프로시저
    @Override public List<Eligible> callFindEligibleBenefits(String memberId) {
        SqlLog.call(Sql.CALL_FIND_ELIGIBLE + "   -- 1: '" + memberId + "', 2: OracleTypes.CURSOR");
        try {
            List<Eligible> list = jdbcTemplate.execute(Sql.CALL_FIND_ELIGIBLE, (CallableStatementCallback<List<Eligible>>) cstmt -> {
                cstmt.setString(1, memberId);                        // IN  : 회원 아이디
                cstmt.registerOutParameter(2, OracleTypes.CURSOR);   // OUT : 지급 대상 혜택 커서
                cstmt.executeQuery();
                List<Eligible> out = new ArrayList<>();
                try (ResultSet rs = (ResultSet) cstmt.getObject(2)) {
                    while (rs.next()) out.add(Mappers.eligible(rs));
                }
                return out;
            });
            List<Eligible> result = list == null ? List.of() : list;
            SqlLog.ok("-- 커서 반환: 지급 대상 혜택 " + result.size() + "건");
            return result;
        } catch (DataAccessException e) { throw fail(e, "받을 수 있는 혜택을 확인하지 못했습니다."); }
    }

    // =========================================================== ⑤ 변경 구문 → ② 트리거
    @Override public void insertGrant(String memberId, int benefitNo) {
        SqlLog.trigger("-- 트리거가 지급 조건을 검사");
        try {
            update(Sql.INSERT_GRANT, "응모하지 못했습니다.", memberId, benefitNo);
        } catch (DbException e) {
            if (DbException.isDuplicate(e)) throw new DbException("이미 응모했거나 받은 혜택입니다.", e.getCause());
            throw e;
        }
    }

    @Override public void useGrant(int grantNo, String memberId) {
        SqlLog.trigger("-- 트리거가 상태 변경을 검사하고 사용 이력을 기록");
        update(Sql.USE_GRANT, "사용 처리하지 못했습니다.", grantNo, memberId);
    }

    @Override public void deleteGrant(int grantNo, String memberId) {
        SqlLog.trigger("-- 트리거가 삭제 가능 여부를 검사");
        update(Sql.DELETE_GRANT, "응모를 취소하지 못했습니다.", grantNo, memberId);
    }

    // =========================================================== 지급 / 사용 이력 (③, 조인)
    @Override public List<BenefitGrant> findGrants(String memberId) {
        return query(Sql.FIND_GRANTS, Mappers::grant, "지급 이력을 불러오지 못했습니다.", memberId);
    }

    @Override public List<BenefitUsage> findUsages(String memberId) {
        return query(Sql.FIND_USAGES, Mappers::usage, "사용 이력을 불러오지 못했습니다.", memberId);
    }

    // =========================================================== 공통
    @FunctionalInterface interface RowReader<T> { T read(ResultSet rs) throws java.sql.SQLException; }

    /** jdbcTemplate.query + RowMapper. Mappers 의 메서드를 RowMapper 로 감싼다. */
    private <T> List<T> query(String sql, RowReader<T> reader, String failMessage, Object... params) {
        SqlLog.query(sql);
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> reader.read(rs), params);
        } catch (DataAccessException e) { throw fail(e, failMessage); }
    }

    private void update(String sql, String failMessage, Object... params) {
        SqlLog.change(sql);
        try {
            int n = jdbcTemplate.update(sql, params);
            SqlLog.ok("-- " + n + "행, COMMIT");
        } catch (DataAccessException e) { throw fail(e, failMessage); }
    }

    private static <T> Optional<T> first(List<T> list) { return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0)); }

    private DbException fail(DataAccessException e, String fallback) {
        DbException d = DbException.from(e, fallback);
        Throwable root = d.getCause() != null ? d.getCause() : e;
        SqlLog.error(root.getMessage() == null ? root.toString() : root.getMessage().trim());
        return d;
    }
}
