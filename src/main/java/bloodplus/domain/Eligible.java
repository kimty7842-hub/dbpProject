package bloodplus.domain;

/** 저장 프로시저 SP_FIND_ELIGIBLE_BENEFITS 결과 한 행: 지급 대상 혜택 + 내 헌혈 횟수 */
public class Eligible {
    private final Benefit benefit;
    private final int myCount;

    public Eligible(Benefit benefit, int myCount) { this.benefit = benefit; this.myCount = myCount; }
    public Benefit getBenefit() { return benefit; }
    public int getMyCount() { return myCount; }
}
