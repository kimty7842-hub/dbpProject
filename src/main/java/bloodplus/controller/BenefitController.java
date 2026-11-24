package bloodplus.controller;

import bloodplus.WebConfig;
import bloodplus.domain.*;
import bloodplus.repository.DbException;
import bloodplus.service.BenefitService;
import bloodplus.service.MemberService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 혜택 찾기
 *  GET  /benefits           : ③ 조건 검색 (+ no 로 상세 조회)
 *  GET  /benefits/eligible  : ④ CallableStatement 로 저장 프로시저 호출
 *  POST /benefits/{no}/apply: ⑤ INSERT → 트리거가 조건 검사
 */
@Controller
public class BenefitController {

    private final BenefitService benefitService;
    private final MemberService memberService;

    public BenefitController(BenefitService benefitService, MemberService memberService) {
        this.benefitService = benefitService; this.memberService = memberService;
    }

    @GetMapping("/benefits")
    public String search(@SessionAttribute(WebConfig.LOGIN_ID) String loginId,
                         @RequestParam(value = "keyword", required = false) String keyword,
                         @RequestParam(value = "giftType", required = false) String giftType,
                         @RequestParam(value = "donationType", required = false) String donationType,
                         @RequestParam(value = "region", required = false) String region,
                         @RequestParam(value = "openOnly", required = false) String openOnly,
                         @RequestParam(value = "searched", required = false) String searched,
                         @RequestParam(value = "no", required = false) Integer no,
                         Model model) {
        BenefitQuery q = new BenefitQuery();
        q.setKeyword(keyword); q.setGiftType(giftType); q.setDonationType(donationType); q.setRegion(region);
        boolean open = searched == null || "on".equals(openOnly);   // 처음 들어오면 '응모 가능한 혜택만' 이 켜진 상태
        if (open) q.setOnDate(benefitService.today());
        List<Benefit> list = benefitService.search(q);

        model.addAttribute("q", q);
        model.addAttribute("openOnly", open);
        model.addAttribute("openFlag", open ? "on" : "off");
        model.addAttribute("eligibleMode", false);
        model.addAttribute("resultTitle", "검색 결과 " + list.size() + "건");
        fillPage(loginId, list, no, model);
        return "benefits/list";
    }

    @GetMapping("/benefits/eligible")
    public String eligible(@SessionAttribute(WebConfig.LOGIN_ID) String loginId,
                           @RequestParam(value = "no", required = false) Integer no, Model model) {
        List<Benefit> list = new ArrayList<>();
        for (Eligible e : benefitService.eligible(loginId)) list.add(e.getBenefit());     // ④ 저장 프로시저 결과
        model.addAttribute("q", new BenefitQuery());
        model.addAttribute("openOnly", true);
        model.addAttribute("openFlag", "on");
        model.addAttribute("eligibleMode", true);
        model.addAttribute("resultTitle", "지금 받을 수 있는 혜택 " + list.size() + "건");
        fillPage(loginId, list, no, model);
        return "benefits/list";
    }

    private void fillPage(String loginId, List<Benefit> list, Integer no, Model model) {
        LocalDate today = benefitService.today();
        List<String> statuses = new ArrayList<>();
        for (Benefit b : list) statuses.add(b.statusOn(today));
        Integer selected = no != null ? no : (list.isEmpty() ? null : list.get(0).getNo());
        model.addAttribute("active", "benefits");
        model.addAttribute("benefits", list);
        model.addAttribute("statuses", statuses);
        model.addAttribute("selected", selected);
        model.addAttribute("detail", selected == null ? null : benefitService.detail(memberService.find(loginId), selected));
        model.addAttribute("giftTypes", Fmt.GIFT_TYPES);
        model.addAttribute("donationTypes", Fmt.DONATION_TYPES);
        model.addAttribute("regions", Fmt.REGIONS);
    }

    @PostMapping("/benefits/{no}/apply")
    public String apply(@SessionAttribute(WebConfig.LOGIN_ID) String loginId, @PathVariable("no") int no,
                        @RequestParam(value = "back", required = false) String back, RedirectAttributes redirect) {
        String name = benefitService.benefitName(no);
        try {
            String status = benefitService.apply(loginId, no);                                  // ⑤ INSERT → 트리거
            redirect.addFlashAttribute("notice", "'" + name + "'에 응모했습니다. "
                    + (BenefitGrant.GRANTED.equals(status) ? "바로 지급돼서 내 혜택에서 사용할 수 있습니다." : "지급 대기 중입니다. 헌혈의집에서 받을 수 있습니다."));
        } catch (DbException e) {
            redirect.addFlashAttribute("error", "응모할 수 없습니다. " + e.getMessage());          // 트리거가 막은 이유
        }
        return "redirect:" + safeBack(back, "/benefits?no=" + no);
    }


    /** 다른 사이트로 보내지 않도록 우리 화면 주소만 허용 */
    static String safeBack(String back, String fallback) {
        return back != null && back.startsWith("/") && !back.startsWith("//") ? back : fallback;
    }
}
