package bloodplus.repository;

import bloodplus.domain.*;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ResultSet 한 행을 자바 객체로 바꾸는 코드 (ORM 역할).
 * JDBC 저장소는 직접 호출하고, JdbcTemplate 저장소는 RowMapper 로 넘겨서 같이 쓴다.
 */
public final class Mappers {
    private Mappers() {}

    public static Member member(ResultSet rs) throws SQLException {
        return new Member(rs.getString("member_id"), null, rs.getString("name"), rs.getString("phone"), rs.getString("address"));
    }

    public static Donation donation(ResultSet rs) throws SQLException {
        return new Donation(rs.getInt("donation_no"), rs.getString("member_id"), rs.getDate("donation_date").toLocalDate(),
                rs.getString("donation_type"), rs.getString("donation_region"));
    }

    public static Benefit benefit(ResultSet rs) throws SQLException {
        Date s = rs.getDate("apply_start"), e = rs.getDate("apply_end");
        return new Benefit(rs.getInt("benefit_no"), rs.getString("benefit_name"), rs.getInt("required_count"),
                s == null ? null : s.toLocalDate(), e == null ? null : e.toLocalDate(),
                rs.getString("gift_type"), rs.getString("target_type"), rs.getString("target_region"));
    }

    /** 저장 프로시저 커서의 한 행: 혜택 컬럼 + my_count */
    public static Eligible eligible(ResultSet rs) throws SQLException {
        return new Eligible(benefit(rs), rs.getInt("my_count"));
    }

    public static BenefitGrant grant(ResultSet rs) throws SQLException {
        BenefitGrant g = new BenefitGrant(rs.getInt("grant_no"), rs.getString("member_id"), rs.getInt("benefit_no"),
                rs.getDate("grant_date").toLocalDate(), rs.getString("grant_status"));
        g.setBenefitName(rs.getString("benefit_name"));
        g.setGiftType(rs.getString("gift_type"));
        return g;
    }

    public static BenefitUsage usage(ResultSet rs) throws SQLException {
        BenefitUsage u = new BenefitUsage(rs.getInt("usage_no"), rs.getString("member_id"), rs.getInt("benefit_no"), rs.getDate("usage_date").toLocalDate());
        u.setBenefitName(rs.getString("benefit_name"));
        u.setGiftType(rs.getString("gift_type"));
        return u;
    }
}
