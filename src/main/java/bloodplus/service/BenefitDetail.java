package bloodplus.service;

import bloodplus.domain.Benefit;
import bloodplus.domain.BenefitGrant;

/** 혜택 상세 화면에 필요한 값 묶음 (혜택 + 내 진행 상황 + 응모 가능 여부) */
public class BenefitDetail {
    private final Benefit benefit;
    private final String status;        // 상시, 모집 중, 예정, 마감
    private final int myCount;
    private final BenefitGrant myGrant; // 이미 응모했으면 그 지급 정보
    private final boolean canApply;
    private final String hint;

    public BenefitDetail(Benefit benefit, String status, int myCount, BenefitGrant myGrant, boolean canApply, String hint) {
        this.benefit = benefit; this.status = status; this.myCount = myCount; this.myGrant = myGrant; this.canApply = canApply; this.hint = hint;
    }

    public Benefit getBenefit() { return benefit; }
    public String getStatus() { return status; }
    public int getMyCount() { return myCount; }
    public BenefitGrant getMyGrant() { return myGrant; }
    public boolean isCanApply() { return canApply; }
    public String getHint() { return hint; }
    public int getPercent() { return (int) Math.min(100, Math.round(myCount * 100.0 / Math.max(1, benefit.getRequiredCount()))); }
    public int getShownCount() { return Math.min(myCount, benefit.getRequiredCount()); }
}
