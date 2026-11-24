package bloodplus.controller;

import bloodplus.WebConfig;
import bloodplus.domain.BenefitGrant;
import bloodplus.domain.Donation;
import bloodplus.domain.Member;
import bloodplus.service.BenefitService;
import bloodplus.service.DonationService;
import bloodplus.service.MemberService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.util.List;

/** 홈: 누적 헌혈, 지금 응모할 수 있는 혜택(저장 프로시저), 사용할 수 있는 혜택, 다음 혜택까지, 최근 헌혈 */
@Controller
public class HomeController {

    private final MemberService memberService;
    private final DonationService donationService;
    private final BenefitService benefitService;

    public HomeController(MemberService memberService, DonationService donationService, BenefitService benefitService) {
        this.memberService = memberService; this.donationService = donationService; this.benefitService = benefitService;
    }

    @GetMapping("/")
    public String home(@SessionAttribute(WebConfig.LOGIN_ID) String loginId, Model model) {
        Member me = memberService.find(loginId);
        List<Donation> donations = donationService.list(loginId);
        List<BenefitGrant> grants = benefitService.grants(loginId);
        model.addAttribute("active", "home");
        model.addAttribute("me", me);
        model.addAttribute("donationCount", donations.size());
        model.addAttribute("recent", donations.subList(0, Math.min(4, donations.size())));
        model.addAttribute("lastDate", donations.isEmpty() ? null : donations.get(0).getDateText());
        model.addAttribute("eligible", benefitService.eligible(loginId));                 // ④ 저장 프로시저
        model.addAttribute("usableCount", grants.stream().filter(BenefitGrant::isGranted).count());
        model.addAttribute("waitingCount", grants.stream().filter(BenefitGrant::isWaiting).count());
        model.addAttribute("next", benefitService.nextGoal(me, grants));
        return "home";
    }
}
