package bloodplus.domain;

import java.time.LocalDate;

/** 혜택 검색 조건 (화면의 검색 폼) */
public class BenefitQuery {
    private String keyword = "";
    private String giftType;       // null = 전체
    private String donationType;   // null = 전체
    private String region;         // null = 전체
    private LocalDate onDate;      // 이 날 응모 가능한 혜택만 (null = 전체)

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword == null ? "" : keyword.trim(); }
    public String getGiftType() { return giftType; }
    public void setGiftType(String giftType) { this.giftType = blank(giftType); }
    public String getDonationType() { return donationType; }
    public void setDonationType(String donationType) { this.donationType = blank(donationType); }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = blank(region); }
    public LocalDate getOnDate() { return onDate; }
    public void setOnDate(LocalDate onDate) { this.onDate = onDate; }

    private static String blank(String s) { return s == null || s.isBlank() ? null : s; }
}
