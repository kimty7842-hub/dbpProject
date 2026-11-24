package bloodplus.domain;

import java.time.LocalDate;

/** 혜택 사용 이력 BENEFIT_USAGE (사용번호, 아이디, 혜택번호, 사용일자) + 조인한 혜택 정보 */
public class BenefitUsage {
    private int no;
    private String memberId;
    private int benefitNo;
    private LocalDate date;
    private String benefitName;
    private String giftType;

    public BenefitUsage() {}
    public BenefitUsage(int no, String memberId, int benefitNo, LocalDate date) {
        this.no = no; this.memberId = memberId; this.benefitNo = benefitNo; this.date = date;
    }

    public int getNo() { return no; }
    public void setNo(int no) { this.no = no; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public int getBenefitNo() { return benefitNo; }
    public void setBenefitNo(int benefitNo) { this.benefitNo = benefitNo; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getBenefitName() { return benefitName; }
    public void setBenefitName(String benefitName) { this.benefitName = benefitName; }
    public String getGiftType() { return giftType; }
    public void setGiftType(String giftType) { this.giftType = giftType; }
    public String getDateText() { return Fmt.date(date); }
}
