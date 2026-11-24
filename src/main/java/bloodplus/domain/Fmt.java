package bloodplus.domain;

import java.time.LocalDate;

public final class Fmt {
    private Fmt() {}
    public static final String[] DONATION_TYPES = {"전혈", "혈장", "혈소판"};
    public static final String[] REGIONS = {"서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종", "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주"};
    public static final String[] CITIES = {"서울특별시", "부산광역시", "대구광역시", "인천광역시", "광주광역시", "대전광역시", "울산광역시", "세종특별자치시",
            "경기도", "강원특별자치도", "충청북도", "충청남도", "전북특별자치도", "전라남도", "경상북도", "경상남도", "제주특별자치도"};
    public static final String[] GIFT_TYPES = {"기념품", "유공장", "모바일 쿠폰", "영화관람권"};

    public static String date(LocalDate d) {
        return d == null ? "-" : String.format("%04d.%02d.%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }

    /** 시/도 이름 → 헌혈 지역 이름 (서울특별시 → 서울, 충청북도 → 충북) */
    public static String regionOf(String address) {
        if (address == null) return REGIONS[0];
        int i = 0;
        for (String c : CITIES) { if (address.startsWith(c)) return REGIONS[i]; i++; }
        return REGIONS[0];
    }
}
