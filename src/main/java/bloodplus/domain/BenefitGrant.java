package bloodplus.domain;

import java.time.LocalDate;

/** 혜택 지급 이력 BENEFIT_GRANT (지급번호, 아이디, 혜택번호, 지급일자, 지급상태) + 조인한 혜택 정보 */
public class BenefitGrant {
    public static final String WAITING = "지급 대기";
    public static final String GRANTED = "지급 완료";
    public static final String USED = "사용 완료";

    private int no;
    private String memberId;
    private int benefitNo;
    private LocalDate date;
    private String status;
    private String benefitName;   // BENEFIT 조인
    private String giftType;      // BENEFIT 조인

    public BenefitGrant() {}
    public BenefitGrant(int no, String memberId, int benefitNo, LocalDate date, String status) {
        this.no = no; this.memberId = memberId; this.benefitNo = benefitNo; this.date = date; this.status = status;
    }

    public boolean isWaiting() { return WAITING.equals(status); }
    public boolean isGranted() { return GRANTED.equals(status); }
    public boolean isUsed() { return USED.equals(status); }

    public int getNo() { return no; }
    public void setNo(int no) { this.no = no; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public int getBenefitNo() { return benefitNo; }
    public void setBenefitNo(int benefitNo) { this.benefitNo = benefitNo; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBenefitName() { return benefitName; }
    public void setBenefitName(String benefitName) { this.benefitName = benefitName; }
    public String getGiftType() { return giftType; }
    public void setGiftType(String giftType) { this.giftType = giftType; }
    public String getDateText() { return Fmt.date(date); }
}
