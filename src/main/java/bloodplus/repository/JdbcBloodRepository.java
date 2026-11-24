package bloodplus.repository;

import bloodplus.domain.*;
import oracle.jdbc.OracleTypes;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 순수 JDBC 구현체 (교수님 예제의 JdbcCustomerRepository 방식)
 *   ③ PreparedStatement : query() / update() 도우미 안에서 사용
 *   ④ CallableStatement : callFindEligibleBenefits() 에서 저장 프로시저 호출, OracleTypes.CURSOR 로 결과 받기
 *   ⑤ 변경 구문         : BENEFIT_GRANT INSERT / UPDATE / DELETE → 트리거 자동 실행
 * SQL 문장은 Sql.java, 결과 변환은 Mappers.java 에 있다.
 */
public class JdbcBloodRepository implements BloodRepository {

    private final DataSource dataSource;

    public JdbcBloodRepository(DataSource dataSource) { this.dataSource = dataSource; }

    // =========================================================== 회원 (③)
    @Override public Optional<Member> findMemberByIdAndPassword(String id, String password) {
        return first(query(Sql.LOGIN, Mappers::member, "로그인 중 문제가 생겼습니다.", id, password));
    }

    @Override public Optional<Member> findMemberById(String id) {
        return first(query(Sql.FIND_MEMBER, Mappers::member, "회원 정보를 불러오지 못했습니다.", id));
    }

    @Override public void saveMember(Member m) {
        try {
            update(Sql.INSERT_MEMBER, "가입하지 못했습니다.", m.getId(), m.getPassword(), m.getName(), m.getPhone(), m.getAddress());
        } catch (DbException e) {
            if (DbException.isDuplicate(e)) throw new DbException("이미 사용 중인 아이디입니다.", e.getCause());
            throw e;
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
        Connection conn = null; CallableStatement cstmt = null;
        try {
            conn = getConnection();
            cstmt = conn.prepareCall(Sql.CALL_DELETE_MEMBER);
            cstmt.setString(1, id);
            cstmt.execute();
            SqlLog.ok("COMMIT");
        } catch (SQLException e) {
            throw fail(e, "탈퇴 처리하지 못했습니다.");
        } finally {
            close(conn, cstmt, null);
        }
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
        Connection conn = null; CallableStatement cstmt = null; ResultSet rs = null;
        try {
            conn = getConnection();
            cstmt = conn.prepareCall(Sql.CALL_FIND_ELIGIBLE);
            cstmt.setString(1, memberId);                          // IN  : 회원 아이디
            cstmt.registerOutParameter(2, OracleTypes.CURSOR);     // OUT : 지급 대상 혜택 커서
            cstmt.executeQuery();
            rs = (ResultSet) cstmt.getObject(2);
            List<Eligible> list = new ArrayList<>();
            while (rs.next()) list.add(Mappers.eligible(rs));
            SqlLog.ok("-- 커서 반환: 지급 대상 혜택 " + list.size() + "건");
            return list;
        } catch (SQLException e) {
            throw fail(e, "받을 수 있는 혜택을 확인하지 못했습니다.");
        } finally {
            close(conn, cstmt, rs);
        }
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

    // =========================================================== 공통 도우미 (PreparedStatement)
    @FunctionalInterface interface RowReader<T> { T read(ResultSet rs) throws SQLException; }

    /** SELECT: PreparedStatement 에 값을 넣고 executeQuery, 한 행씩 reader 로 변환 */
    private <T> List<T> query(String sql, RowReader<T> reader, String failMessage, Object... params) {
        SqlLog.query(sql);
        Connection conn = null; PreparedStatement pstmt = null; ResultSet rs = null;
        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) pstmt.setObject(i + 1, params[i]);
            rs = pstmt.executeQuery();
            List<T> list = new ArrayList<>();
            while (rs.next()) list.add(reader.read(rs));
            return list;
        } catch (SQLException e) {
            throw fail(e, failMessage);
        } finally {
            close(conn, pstmt, rs);
        }
    }

    /** INSERT, UPDATE, DELETE: PreparedStatement 에 값을 넣고 executeUpdate */
    private void update(String sql, String failMessage, Object... params) {
        SqlLog.change(sql);
        Connection conn = null; PreparedStatement pstmt = null;
        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) pstmt.setObject(i + 1, params[i]);
            int n = pstmt.executeUpdate();
            SqlLog.ok("-- " + n + "행, COMMIT");
        } catch (SQLException e) {
            throw fail(e, failMessage);
        } finally {
            close(conn, pstmt, null);
        }
    }

    private static <T> Optional<T> first(List<T> list) { return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0)); }

    private DbException fail(SQLException e, String fallback) {
        SqlLog.error(e.getMessage() == null ? e.toString() : e.getMessage().trim());
        return DbException.fromOracle(e, fallback);
    }

    private Connection getConnection() { return DataSourceUtils.getConnection(dataSource); }

    private void close(Connection conn, Statement stmt, ResultSet rs) {
        try { if (rs != null) rs.close(); } catch (SQLException ignored) { }
        try { if (stmt != null) stmt.close(); } catch (SQLException ignored) { }
        if (conn != null) DataSourceUtils.releaseConnection(conn, dataSource);
    }
}
