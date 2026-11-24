package bloodplus.repository;

import bloodplus.domain.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 메모리 구현체 (교수님 예제의 MemoryCustomerRepository 역할)
 * DB 없이 실행된다. 저장 프로시저와 트리거의 동작을 자바 코드로 흉내 내고,
 * Oracle 이었다면 실행됐을 SQL 을 SqlLog 에 남긴다.
 */
public class MemoryBloodRepository implements BloodRepository {

    private final Map<String, Member> members = new LinkedHashMap<>();
    private final List<Donation> donations = new ArrayList<>();
    private final Map<Integer, Benefit> benefits = new LinkedHashMap<>();
    private final List<BenefitGrant> grants = new ArrayList<>();
    private final List<BenefitUsage> usages = new ArrayList<>();
    private int donationSeq = 0, grantSeq = 0, usageSeq = 0;

    /** 바로 지급 완료되는 선물 (나머지는 헌혈의집에서 받는 현물이라 '지급 대기') */
    private static final Set<String> INSTANT_GIFTS = Set.of("모바일 쿠폰", "영화관람권");

    public MemoryBloodRepository() { seed(); }

    private static LocalDate today() { return LocalDate.now(); }

    // =========================================================== 회원
    @Override public synchronized Optional<Member> findMemberByIdAndPassword(String id, String password) {
        SqlLog.query("SELECT member_id, name, phone, address FROM MEMBER WHERE member_id = '" + id + "' AND password = '****'");
        Member m = members.get(id);
        if (m == null || !m.getPassword().equals(password)) { SqlLog.info("-- 0행"); return Optional.empty(); }
        SqlLog.ok("-- 1행");
        return Optional.of(copy(m));
    }

    @Override public synchronized Optional<Member> findMemberById(String id) {
        SqlLog.query("SELECT member_id, name, phone, address FROM MEMBER WHERE member_id = '" + id + "'");
        return Optional.ofNullable(members.get(id)).map(MemoryBloodRepository::copy);
    }

    @Override public synchronized void saveMember(Member m) {
        SqlLog.change("INSERT INTO MEMBER (member_id, password, name, phone, address) VALUES ('" + m.getId() + "', '****', '" + m.getName() + "', '" + m.getPhone() + "', '" + m.getAddress() + "')");
        if (members.containsKey(m.getId())) {
            SqlLog.error("ORA-00001: 무결성 제약 조건(PK_MEMBER)에 위배됩니다");
            throw new DbException("이미 사용 중인 아이디입니다.");
        }
        members.put(m.getId(), copy(m));
        SqlLog.ok("COMMIT");
    }

    @Override public synchronized void updateMember(Member m) {
        SqlLog.change("UPDATE MEMBER SET name = '" + m.getName() + "', phone = '" + m.getPhone() + "', address = '" + m.getAddress() + "'"
                + (m.getPassword() != null ? ", password = '****'" : "") + " WHERE member_id = '" + m.getId() + "'");
        Member cur = members.get(m.getId());
        if (cur == null) throw new DbException("회원 정보를 찾을 수 없습니다.");
        cur.setName(m.getName()); cur.setPhone(m.getPhone()); cur.setAddress(m.getAddress());
        if (m.getPassword() != null) cur.setPassword(m.getPassword());
        SqlLog.ok("-- 1행 수정, COMMIT");
    }

    @Override public synchronized void deleteMember(String id) {
        SqlLog.call("{ call SP_DELETE_MEMBER('" + id + "') }");
        SqlLog.info("-- DELETE FROM MEMBER WHERE member_id = '" + id + "'  (ON DELETE CASCADE: DONATION, BENEFIT_GRANT, BENEFIT_USAGE 함께 삭제)");
        members.remove(id);
        donations.removeIf(d -> d.getMemberId().equals(id));
        grants.removeIf(g -> g.getMemberId().equals(id));
        usages.removeIf(u -> u.getMemberId().equals(id));
        SqlLog.ok("COMMIT");
    }

    // =========================================================== 헌혈 이력
    @Override public synchronized List<Donation> findDonations(String memberId) {
        SqlLog.query("SELECT donation_no, donation_date, donation_type, donation_region FROM DONATION WHERE member_id = '" + memberId + "' ORDER BY donation_date DESC, donation_no DESC");
        return donations.stream().filter(d -> d.getMemberId().equals(memberId))
                .sorted(Comparator.comparing(Donation::getDate).reversed().thenComparing(d -> -d.getNo()))
                .map(MemoryBloodRepository::copy).collect(Collectors.toList());
    }

    @Override public synchronized Optional<Donation> findDonation(int no, String memberId) {
        SqlLog.query("SELECT * FROM DONATION WHERE donation_no = " + no + " AND member_id = '" + memberId + "'");
        return donations.stream().filter(d -> d.getNo() == no && d.getMemberId().equals(memberId)).findFirst().map(MemoryBloodRepository::copy);
    }

    @Override public synchronized void saveDonation(Donation d) {
        d.setNo(++donationSeq);
        SqlLog.change("INSERT INTO DONATION (donation_no, member_id, donation_date, donation_type, donation_region) VALUES (SEQ_DONATION.NEXTVAL, '"
                + d.getMemberId() + "', DATE '" + d.getDate() + "', '" + d.getType() + "', '" + d.getRegion() + "')");
        donations.add(copy(d));
        SqlLog.ok("COMMIT");
    }

    @Override public synchronized void updateDonation(Donation d) {
        SqlLog.change("UPDATE DONATION SET donation_date = DATE '" + d.getDate() + "', donation_type = '" + d.getType() + "', donation_region = '" + d.getRegion()
                + "' WHERE donation_no = " + d.getNo() + " AND member_id = '" + d.getMemberId() + "'");
        Donation cur = donations.stream().filter(x -> x.getNo() == d.getNo() && x.getMemberId().equals(d.getMemberId())).findFirst()
                .orElseThrow(() -> new DbException("헌혈 기록을 찾을 수 없습니다."));
        cur.setDate(d.getDate()); cur.setType(d.getType()); cur.setRegion(d.getRegion());
        SqlLog.ok("-- 1행 수정, COMMIT");
    }

    @Override public synchronized void deleteDonation(int no, String memberId) {
        SqlLog.change("DELETE FROM DONATION WHERE donation_no = " + no + " AND member_id = '" + memberId + "'");
        donations.removeIf(x -> x.getNo() == no && x.getMemberId().equals(memberId));
        SqlLog.ok("-- 1행 삭제, COMMIT");
    }

    // =========================================================== 혜택
    @Override public synchronized List<Benefit> searchBenefits(BenefitQuery q) {
        StringBuilder sql = new StringBuilder("SELECT * FROM BENEFIT WHERE benefit_name LIKE '%" + q.getKeyword() + "%'");
        if (q.getGiftType() != null) sql.append(" AND gift_type = '").append(q.getGiftType()).append("'");
        if (q.getDonationType() != null) sql.append(" AND (target_type IS NULL OR target_type = '").append(q.getDonationType()).append("')");
        if (q.getRegion() != null) sql.append(" AND (target_region IS NULL OR target_region = '").append(q.getRegion()).append("')");
        if (q.getOnDate() != null) sql.append(" AND (apply_start IS NULL OR DATE '").append(q.getOnDate()).append("' BETWEEN apply_start AND apply_end)");
        SqlLog.query(sql.append(" ORDER BY benefit_no").toString());
        return benefits.values().stream()
                .filter(b -> b.getName().contains(q.getKeyword()))
                .filter(b -> q.getGiftType() == null || q.getGiftType().equals(b.getGiftType()))
                .filter(b -> q.getDonationType() == null || b.getTargetType() == null || q.getDonationType().equals(b.getTargetType()))
                .filter(b -> q.getRegion() == null || b.getTargetRegion() == null || q.getRegion().equals(b.getTargetRegion()))
                .filter(b -> q.getOnDate() == null || b.isOpenOn(q.getOnDate()))
                .collect(Collectors.toList());
    }

    @Override public synchronized Optional<Benefit> findBenefit(int benefitNo) {
        SqlLog.query("SELECT * FROM BENEFIT WHERE benefit_no = " + benefitNo);
        return Optional.ofNullable(benefits.get(benefitNo));
    }

    @Override public synchronized Map<String, Integer> countDonationsByType(String memberId) {
        SqlLog.query("SELECT donation_type, COUNT(*) AS cnt FROM DONATION WHERE member_id = '" + memberId + "' GROUP BY donation_type");
        Map<String, Integer> map = new HashMap<>();
        for (Donation d : donations) if (d.getMemberId().equals(memberId)) map.merge(d.getType(), 1, Integer::sum);
        return map;
    }

    /** 트리거, 프로시저 안에서 쓰는 횟수 계산 (로그를 남기지 않음) */
    private int countFor(String memberId, Benefit b) {
        return (int) donations.stream().filter(d -> d.getMemberId().equals(memberId))
                .filter(d -> b.getTargetType() == null || b.getTargetType().equals(d.getType())).count();
    }

    private static boolean regionOk(Member m, Benefit b) {
        return b.getTargetRegion() == null || (m.getAddress() != null && m.getAddress().contains(b.getTargetRegion()));
    }

    // =========================================================== ① 저장 프로시저 (④ CallableStatement 로 호출)
    @Override public synchronized List<Eligible> callFindEligibleBenefits(String memberId) {
        SqlLog.call("{ call SP_FIND_ELIGIBLE_BENEFITS('" + memberId + "', ?) }   -- ? = OracleTypes.CURSOR");
        Member m = members.get(memberId);
        List<Eligible> out = new ArrayList<>();
        if (m == null) return out;
        for (Benefit b : benefits.values()) {
            if (!b.isOpenOn(today())) continue;                                                        // 응모 기간
            if (!regionOk(m, b)) continue;                                                             // 주소(지역)
            if (grants.stream().anyMatch(g -> g.getMemberId().equals(memberId) && g.getBenefitNo() == b.getNo())) continue; // 이미 지급
            int cnt = countFor(memberId, b);                                                  // 헌혈 횟수 (종류 조건)
            if (cnt >= b.getRequiredCount()) out.add(new Eligible(b, cnt));
        }
        SqlLog.ok("-- 커서 반환: 지급 대상 혜택 " + out.size() + "건");
        return out;
    }

    // =========================================================== ⑤ 변경 구문 → ② 트리거
    @Override public synchronized void insertGrant(String memberId, int benefitNo) {
        SqlLog.change("INSERT INTO BENEFIT_GRANT (grant_no, member_id, benefit_no, grant_date, grant_status) VALUES (SEQ_GRANT.NEXTVAL, '"
                + memberId + "', " + benefitNo + ", SYSDATE, '" + BenefitGrant.WAITING + "')");
        Member m = members.get(memberId);
        Benefit b = benefits.get(benefitNo);
        SqlLog.trigger("트리거 동작(메모리 모드 대체): 응모 조건 검사");
        if (grants.stream().anyMatch(g -> g.getMemberId().equals(memberId) && g.getBenefitNo() == benefitNo)) {
            SqlLog.error("ORA-00001: 무결성 제약 조건(UQ_GRANT_MEMBER_BENEFIT)에 위배됩니다");
            throw new DbException("이미 응모했거나 받은 혜택입니다.");
        }
        if (!b.isOpenOn(today())) raise(20001, "응모 기간이 아닌 혜택입니다.");
        if (!regionOk(m, b)) raise(20003, b.getTargetRegion() + " 지역 거주 회원만 응모할 수 있습니다.");
        int cnt = countFor(memberId, b);
        SqlLog.trigger("  " + (b.getTargetType() != null ? b.getTargetType() + " " : "") + "헌혈 " + cnt + "회 / 필요 " + b.getRequiredCount() + "회");
        if (cnt < b.getRequiredCount())
            raise(20002, "지급 조건을 충족하지 않습니다. " + (b.getTargetType() != null ? b.getTargetType() + " 헌혈 " : "헌혈 ")
                    + b.getRequiredCount() + "회가 필요한데 지금 " + cnt + "회입니다.");
        String status = INSTANT_GIFTS.contains(b.getGiftType()) ? BenefitGrant.GRANTED : BenefitGrant.WAITING;
        SqlLog.trigger("  지급 상태 '" + status + "'(으)로 등록");
        grants.add(new BenefitGrant(++grantSeq, memberId, benefitNo, today(), status));
        SqlLog.ok("COMMIT");
    }

    @Override public synchronized void useGrant(int grantNo, String memberId) {
        SqlLog.change("UPDATE BENEFIT_GRANT SET grant_status = '" + BenefitGrant.USED + "' WHERE grant_no = " + grantNo + " AND member_id = '" + memberId + "'");
        BenefitGrant g = grant(grantNo, memberId);
        SqlLog.trigger("트리거 동작(메모리 모드 대체): 상태 변경 검사");
        if (!g.isGranted()) raise(20004, g.isUsed() ? "이미 사용한 혜택입니다." : "아직 지급되지 않은 혜택은 사용할 수 없습니다.");
        g.setStatus(BenefitGrant.USED);
        usages.add(new BenefitUsage(++usageSeq, memberId, g.getBenefitNo(), today()));
        SqlLog.trigger("트리거 동작(메모리 모드 대체): 사용 이력 1건 기록");
        SqlLog.ok("COMMIT");
    }

    @Override public synchronized void deleteGrant(int grantNo, String memberId) {
        SqlLog.change("DELETE FROM BENEFIT_GRANT WHERE grant_no = " + grantNo + " AND member_id = '" + memberId + "'");
        BenefitGrant g = grant(grantNo, memberId);
        SqlLog.trigger("트리거 동작(메모리 모드 대체): 취소 가능 여부 검사");
        if (!g.isWaiting()) raise(20005, "이미 지급된 혜택은 응모를 취소할 수 없습니다.");
        grants.remove(g);
        SqlLog.ok("-- 1행 삭제, COMMIT");
    }

    private BenefitGrant grant(int grantNo, String memberId) {
        return grants.stream().filter(x -> x.getNo() == grantNo && x.getMemberId().equals(memberId)).findFirst()
                .orElseThrow(() -> new DbException("지급 정보를 찾을 수 없습니다."));
    }

    private static void raise(int code, String msg) {
        SqlLog.error("ORA-" + code + ": " + msg);
        SqlLog.error("ROLLBACK");
        throw new DbException(msg);
    }

    // =========================================================== 지급 / 사용 이력 (조인)
    @Override public synchronized List<BenefitGrant> findGrants(String memberId) {
        SqlLog.query("SELECT g.grant_no, g.benefit_no, g.grant_date, g.grant_status, b.benefit_name, b.gift_type FROM BENEFIT_GRANT g JOIN BENEFIT b ON g.benefit_no = b.benefit_no WHERE g.member_id = '" + memberId + "' ORDER BY g.grant_date DESC, g.grant_no DESC");
        List<BenefitGrant> out = new ArrayList<>();
        for (BenefitGrant g : grants) if (g.getMemberId().equals(memberId)) {
            BenefitGrant c = new BenefitGrant(g.getNo(), g.getMemberId(), g.getBenefitNo(), g.getDate(), g.getStatus());
            Benefit b = benefits.get(g.getBenefitNo()); c.setBenefitName(b.getName()); c.setGiftType(b.getGiftType());
            out.add(c);
        }
        out.sort(Comparator.comparing(BenefitGrant::getDate).reversed().thenComparing(g -> -g.getNo()));
        return out;
    }

    @Override public synchronized List<BenefitUsage> findUsages(String memberId) {
        SqlLog.query("SELECT u.usage_no, u.benefit_no, u.usage_date, b.benefit_name, b.gift_type FROM BENEFIT_USAGE u JOIN BENEFIT b ON u.benefit_no = b.benefit_no WHERE u.member_id = '" + memberId + "' ORDER BY u.usage_date DESC, u.usage_no DESC");
        List<BenefitUsage> out = new ArrayList<>();
        for (BenefitUsage u : usages) if (u.getMemberId().equals(memberId)) {
            BenefitUsage c = new BenefitUsage(u.getNo(), u.getMemberId(), u.getBenefitNo(), u.getDate());
            Benefit b = benefits.get(u.getBenefitNo()); c.setBenefitName(b.getName()); c.setGiftType(b.getGiftType());
            out.add(c);
        }
        out.sort(Comparator.comparing(BenefitUsage::getDate).reversed().thenComparing(u -> -u.getNo()));
        return out;
    }

    // =========================================================== 초기 데이터
    private static Member copy(Member m) { return new Member(m.getId(), m.getPassword(), m.getName(), m.getPhone(), m.getAddress()); }
    private static Donation copy(Donation d) { return new Donation(d.getNo(), d.getMemberId(), d.getDate(), d.getType(), d.getRegion()); }

    private void seed() {
        SqlLog.setMuted(true);
        String[][] people = {
            {"김민수", "서울특별시 강남구 테헤란로 12"}, {"이영희", "부산광역시 부산진구 가야대로 45"}, {"박지훈", "대전광역시 유성구 대학로 99"},
            {"최수진", "인천광역시 남동구 구월로 8"}, {"정다은", "광주광역시 북구 용봉로 77"}, {"강현우", "대구광역시 수성구 달구벌대로 210"},
            {"조은비", "울산광역시 남구 삼산로 31"}, {"윤서준", "경기도 수원시 팔달구 효원로 5"}, {"장하늘", "부산광역시 해운대구 센텀로 20"},
            {"임도윤", "서울특별시 마포구 월드컵로 101"}, {"신예린", "대전광역시 서구 둔산로 15"}, {"권태양", "경기도 성남시 분당구 판교로 33"},
            {"황보라", "인천광역시 연수구 송도과학로 2"}, {"송민재", "대구광역시 중구 동성로 60"}, {"배수아", "광주광역시 서구 상무대로 18"},
            {"홍준기", "서울특별시 송파구 올림픽로 300"}, {"문서윤", "울산광역시 중구 성안로 11"}, {"양지호", "부산광역시 사하구 낙동대로 88"},
            {"노하은", "경기도 고양시 일산동구 중앙로 40"}, {"서동현", "대전광역시 중구 대종로 7"}};
        for (int i = 0; i < people.length; i++) {
            String id = String.format("user%02d", i + 1);
            members.put(id, new Member(id, "1234", people[i][0], String.format("010-%04d-%04d", 2000 + i * 137, 5000 + i * 311), people[i][1]));
        }

        addBenefit(1, "첫 헌혈 환영 선물", 1, null, null, "기념품", null, null);
        addBenefit(2, "헌혈 10회 달성 기념품", 10, null, null, "기념품", null, null);
        addBenefit(3, "헌혈유공장 은장 (30회)", 30, null, null, "유공장", null, null);
        addBenefit(4, "헌혈유공장 금장 (50회)", 50, null, null, "유공장", null, null);
        addBenefit(5, "혈장 헌혈 5회 감사 쿠폰", 5, null, null, "모바일 쿠폰", "혈장", null);
        addBenefit(6, "부산 연말 헌혈 이벤트 영화관람권", 1, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31), "영화관람권", null, "부산");
        addBenefit(7, "서울 가을 헌혈 이벤트 커피 쿠폰", 1, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31), "모바일 쿠폰", null, "서울");
        addBenefit(8, "겨울철 헌혈 캠페인 쿠폰 (3회)", 3, LocalDate.of(2026, 12, 1), LocalDate.of(2027, 2, 28), "모바일 쿠폰", null, null);

        int[] counts = {29, 9, 49, 15, 3, 31, 0, 22, 1, 55, 5, 12, 0, 28, 7, 40, 9, 2, 18, 50};
        String[] last = {"2026-10-15", "2026-09-20", "2026-10-01", "2026-08-10", "2026-07-22", "2026-10-28", "2026-01-01", "2026-09-05", "2026-11-02", "2026-10-20",
                         "2026-06-01", "2026-10-10", "2026-01-01", "2026-09-30", "2026-05-15", "2026-10-05", "2026-08-25", "2026-03-10", "2026-07-07", "2026-10-30"};
        for (int k = 0; k < counts.length; k++) {
            Member m = members.get(String.format("user%02d", k + 1));
            String region = Fmt.regionOf(m.getAddress());
            LocalDate day = LocalDate.parse(last[k]);
            List<Donation> list = new ArrayList<>();
            for (int i = 0; i < counts[k]; i++) {
                String type = i % 3 == 1 ? "혈장" : (i % 7 == 5 ? "혈소판" : "전혈");
                list.add(new Donation(0, m.getId(), day, type, i % 5 == 3 ? "경기" : region));
                day = day.minusDays(i % 3 == 1 ? 30 : 60);
            }
            Collections.reverse(list);
            for (Donation x : list) { x.setNo(++donationSeq); donations.add(x); }
        }

        // 받을 수 있는 혜택 중 일부는 이미 응모한 상태로 (하나는 남겨 두어 '응모할 수 있는 혜택'이 보이게)
        for (Member m : members.values()) {
            List<Eligible> el = callFindEligibleBenefits(m.getId());
            for (int i = 0; i < el.size(); i++) {
                if (i == el.size() - 1 && el.size() > 1) break;
                try { insertGrant(m.getId(), el.get(i).getBenefit().getNo()); } catch (DbException ignored) {}
            }
        }
        for (BenefitGrant g : grants) if (g.isWaiting() && g.getBenefitNo() <= 3) g.setStatus(BenefitGrant.GRANTED);
        for (BenefitGrant g : List.copyOf(grants)) if (g.getBenefitNo() == 1 && g.isGranted()) {
            try { useGrant(g.getNo(), g.getMemberId()); } catch (DbException ignored) {}
        }
        for (BenefitGrant g : grants) g.setDate(g.getDate().minusDays(10 + (g.getNo() * 7L) % 40));
        for (BenefitUsage u : usages) u.setDate(u.getDate().minusDays(3));
        SqlLog.setMuted(false);
    }

    private void addBenefit(int no, String name, int req, LocalDate s, LocalDate e, String gift, String type, String region) {
        benefits.put(no, new Benefit(no, name, req, s, e, gift, type, region));
    }
}
