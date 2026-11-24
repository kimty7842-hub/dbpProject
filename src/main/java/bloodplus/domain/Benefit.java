package bloodplus.domain;

import java.time.LocalDate;

/**
 * 혜택 BENEFIT (혜택번호, 혜택명, 필요헌혈횟수, 응모시작일, 응모종료일, 선물종류, 대상헌혈종류*, 대상지역*)
 * * 조건 확장용 컬럼. NULL 이면 제한 없음
 */
public class Benefit {
    private int no;
    private String name;
    private int requiredCount;
    private LocalDate applyStart;   // null 이면 상시
    private LocalDate applyEnd;
    private String giftType;
    private String targetType;      // null 이면 모든 헌혈 종류
    private String targetRegion;    // null 이면 전국 (회원 주소와 비교)

    public Benefit() {}
    public Benefit(int no, String name, int requiredCount, LocalDate applyStart, LocalDate applyEnd,
                   String giftType, String targetType, String targetRegion) {
        this.no = no; this.name = name; this.requiredCount = requiredCount; this.applyStart = applyStart; this.applyEnd = applyEnd;
        this.giftType = giftType; this.targetType = targetType; this.targetRegion = targetRegion;
    }

    public boolean isOpenOn(LocalDate day) {
        return applyStart == null || (!day.isBefore(applyStart) && !day.isAfter(applyEnd));
    }

    /** 상시, 예정, 모집 중, 마감 */
    public String statusOn(LocalDate day) {
        if (applyStart == null) return "상시";
        if (day.isBefore(applyStart)) return "예정";
        if (day.isAfter(applyEnd)) return "마감";
        return "모집 중";
    }

    public String getConditionText() {
        StringBuilder sb = new StringBuilder();
        if (targetRegion != null) sb.append(targetRegion).append(" 거주 회원, ");
        sb.append(targetType != null ? targetType + " 헌혈 " : "헌혈 ").append(requiredCount).append("회 이상");
        return sb.toString();
    }

    public String getPeriodText() {
        return applyStart == null ? "상시" : Fmt.date(applyStart) + " ~ " + Fmt.date(applyEnd);
    }

    public int getNo() { return no; }
    public void setNo(int no) { this.no = no; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getRequiredCount() { return requiredCount; }
    public void setRequiredCount(int requiredCount) { this.requiredCount = requiredCount; }
    public LocalDate getApplyStart() { return applyStart; }
    public void setApplyStart(LocalDate applyStart) { this.applyStart = applyStart; }
    public LocalDate getApplyEnd() { return applyEnd; }
    public void setApplyEnd(LocalDate applyEnd) { this.applyEnd = applyEnd; }
    public String getGiftType() { return giftType; }
    public void setGiftType(String giftType) { this.giftType = giftType; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetRegion() { return targetRegion; }
    public void setTargetRegion(String targetRegion) { this.targetRegion = targetRegion; }
}
