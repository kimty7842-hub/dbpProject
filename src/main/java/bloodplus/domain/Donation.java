package bloodplus.domain;

import java.time.LocalDate;

/** 헌혈 이력 DONATION (헌혈번호, 아이디, 헌혈일자, 헌혈종류, 헌혈지역) */
public class Donation {
    private int no;
    private String memberId;
    private LocalDate date;
    private String type;
    private String region;

    public Donation() {}
    public Donation(int no, String memberId, LocalDate date, String type, String region) {
        this.no = no; this.memberId = memberId; this.date = date; this.type = type; this.region = region;
    }

    public int getNo() { return no; }
    public void setNo(int no) { this.no = no; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getDateText() { return Fmt.date(date); }
}
