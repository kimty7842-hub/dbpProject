package bloodplus.service;

import bloodplus.domain.*;
import bloodplus.repository.BloodRepository;
import bloodplus.repository.DbException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 혜택: 검색, 상세, 지급 대상 확인(저장 프로시저), 응모, 사용, 응모 취소, 지급/사용 이력 */
@Service
public class BenefitService {

    private final BloodRepository repository;

    public BenefitService(BloodRepository repository) { this.repository = repository; }

    public LocalDate today() { return LocalDate.now(); }

    // ③ 조건 검색
    public List<Benefit> search(BenefitQuery q) { return repository.searchBenefits(q); }

    // ④ 저장 프로시저 호출
    public List<Eligible> eligible(String memberId) { return repository.callFindEligibleBenefits(memberId); }

    // ③ 혜택번호로 상세 조회 + 내 진행 상황
    public BenefitDetail detail(Member me, int benefitNo) {
        Benefit b = repository.findBenefit(benefitNo).orElseThrow(() -> new DbException("혜택 정보를 찾을 수 없습니다."));
        int have = BloodRepository.countFor(repository.countDonationsByType(me.getId()), b);
        BenefitGrant mine = repository.findGrants(me.getId()).stream().filter(g -> g.getBenefitNo() == benefitNo).findFirst().orElse(null);
        LocalDate today = today();
        boolean regionOk = b.getTargetRegion() == null || (me.getAddress() != null && me.getAddress().contains(b.getTargetRegion()));
        String hint; boolean canApply;
        if (mine != null) { hint = "이미 응모한 혜택입니다. 지금 상태는 '" + mine.getStatus() + "'입니다."; canApply = false; }
        else if (!b.isOpenOn(today)) { hint = today.isBefore(b.getApplyStart()) ? "아직 응모 기간이 아닙니다." : "응모 기간이 끝났습니다."; canApply = false; }
        else if (!regionOk) { hint = b.getTargetRegion() + " 지역 거주 회원 대상입니다. 응모하면 조건 검사에서 거절됩니다."; canApply = true; }
        else if (have < b.getRequiredCount()) { hint = (b.getRequiredCount() - have) + "회 더 헌혈하면 받을 수 있습니다. 지금 응모하면 조건 검사에서 거절됩니다."; canApply = true; }
        else { hint = "조건을 모두 채웠습니다. 지금 응모할 수 있습니다."; canApply = true; }
        return new BenefitDetail(b, b.statusOn(today), have, mine, canApply, hint);
    }

    /** 홈 화면의 '다음 혜택까지': 응모 기간 중이고 지역이 맞는데 횟수만 모자란 혜택 중 가장 가까운 것 */
    public BenefitDetail nextGoal(Member me, List<BenefitGrant> grants) {
        BenefitQuery q = new BenefitQuery();
        q.setOnDate(today());
        Map<String, Integer> counts = repository.countDonationsByType(me.getId());   // 헌혈 횟수는 한 번만 조회
        BenefitDetail best = null;
        for (Benefit b : repository.searchBenefits(q)) {
            if (grants.stream().anyMatch(g -> g.getBenefitNo() == b.getNo())) continue;
            if (b.getTargetRegion() != null && (me.getAddress() == null || !me.getAddress().contains(b.getTargetRegion()))) continue;
            int have = BloodRepository.countFor(counts, b);
            if (have >= b.getRequiredCount()) continue;
            if (best == null || b.getRequiredCount() - have < best.getBenefit().getRequiredCount() - best.getMyCount())
                best = new BenefitDetail(b, b.statusOn(today()), have, null, false, (b.getRequiredCount() - have) + "회 남음");
        }
        return best;
    }

    // ⑤ 응모 (INSERT → 트리거). 결과로 지급 상태를 돌려준다.
    public String apply(String memberId, int benefitNo) {
        repository.insertGrant(memberId, benefitNo);
        return repository.findGrants(memberId).stream().filter(g -> g.getBenefitNo() == benefitNo)
                .map(BenefitGrant::getStatus).findFirst().orElse(BenefitGrant.WAITING);
    }

    // ⑤ 사용 (UPDATE → 트리거가 사용 이력 기록)
    public void use(int grantNo, String memberId) { repository.useGrant(grantNo, memberId); }

    // ⑤ 응모 취소 (DELETE → 트리거가 삭제 가능 여부 검사)
    public void cancel(int grantNo, String memberId) { repository.deleteGrant(grantNo, memberId); }

    public List<BenefitGrant> grants(String memberId) { return repository.findGrants(memberId); }
    public List<BenefitUsage> usages(String memberId) { return repository.findUsages(memberId); }

    public String benefitName(int benefitNo) { return repository.findBenefit(benefitNo).map(Benefit::getName).orElse("혜택"); }
}
