package bloodplus.repository;

import bloodplus.domain.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 데이터 저장소 인터페이스 (교수님 예제의 CustomerRepository 와 같은 역할)
 * 구현체: MemoryBloodRepository, JdbcBloodRepository, JdbcTemplateBloodRepository
 * 어떤 구현체를 쓸지는 SpringConfig 에서 정한다. (다형성)
 *
 * 주석의 번호는 계획서 2.4 프로그램 기능 번호
 */
public interface BloodRepository {

    // ---------- 회원 (③)
    Optional<Member> findMemberByIdAndPassword(String id, String password);
    Optional<Member> findMemberById(String id);
    void saveMember(Member m);                       // 회원가입
    void updateMember(Member m);                     // 정보 수정 (password 가 null 이면 비밀번호는 그대로)
    void deleteMember(String id);                    // 회원 탈퇴

    // ---------- 헌혈 이력 (③)
    List<Donation> findDonations(String memberId);   // 최근 순
    Optional<Donation> findDonation(int no, String memberId);
    void saveDonation(Donation d);
    void updateDonation(Donation d);
    void deleteDonation(int no, String memberId);
    /** 헌혈 종류별 횟수 (예: 전혈=6, 혈장=3). 혜택 조건 계산에 쓴다. */
    Map<String, Integer> countDonationsByType(String memberId);

    // ---------- 혜택 (③)
    List<Benefit> searchBenefits(BenefitQuery q);
    Optional<Benefit> findBenefit(int benefitNo);

    // ---------- ④ CallableStatement 로 ① 저장 프로시저 호출
    List<Eligible> callFindEligibleBenefits(String memberId);

    // ---------- ⑤ 변경 구문 → ② 트리거 자동 실행
    void insertGrant(String memberId, int benefitNo);      // 응모: 트리거가 조건 검사
    void useGrant(int grantNo, String memberId);           // 사용: 트리거가 상태 검사, 사용 이력 기록
    void deleteGrant(int grantNo, String memberId);        // 응모 취소: 트리거가 삭제 가능 여부 검사

    // ---------- 지급 / 사용 이력 (③, BENEFIT 과 조인)
    List<BenefitGrant> findGrants(String memberId);
    List<BenefitUsage> findUsages(String memberId);

    /** 혜택 조건(헌혈 종류)에 맞는 횟수 */
    static int countFor(Map<String, Integer> byType, Benefit b) {
        if (b.getTargetType() != null) return byType.getOrDefault(b.getTargetType(), 0);
        return byType.values().stream().mapToInt(Integer::intValue).sum();
    }
}
