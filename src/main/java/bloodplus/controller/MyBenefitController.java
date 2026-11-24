package bloodplus.controller;

import bloodplus.WebConfig;
import bloodplus.domain.BenefitGrant;
import bloodplus.domain.BenefitUsage;
import bloodplus.repository.DbException;
import bloodplus.service.BenefitService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 내 혜택
 *  GET  /mine                 : ③ 지급 이력, 사용 이력 (BENEFIT 조인)
 *  POST /mine/{grantNo}/use   : ⑤ UPDATE → 트리거가 상태 검사 + 사용 이력 자동 기록
 *  POST /mine/{grantNo}/cancel: ⑤ DELETE → 트리거가 삭제 가능 여부 검사
 */
@Controller
public class MyBenefitController {

    private final BenefitService benefitService;

    public MyBenefitController(BenefitService benefitService) { this.benefitService = benefitService; }

    @GetMapping("/mine")
    public String mine(@SessionAttribute(WebConfig.LOGIN_ID) String loginId,
                       @RequestParam(value = "tab", required = false, defaultValue = "grant") String tab, Model model) {
        List<BenefitGrant> grants = benefitService.grants(loginId);
        List<BenefitUsage> usages = benefitService.usages(loginId);
        model.addAttribute("active", "mine");
        model.addAttribute("tab", "usage".equals(tab) ? "usage" : "grant");
        model.addAttribute("grants", grants);
        model.addAttribute("usages", usages);
        return "mine";
    }

    @PostMapping("/mine/{grantNo}/use")
    public String use(@SessionAttribute(WebConfig.LOGIN_ID) String loginId, @PathVariable("grantNo") int grantNo,
                      @RequestParam(value = "name", required = false) String name, RedirectAttributes redirect) {
        try {
            benefitService.use(grantNo, loginId);
            redirect.addFlashAttribute("notice", (name == null ? "혜택" : "'" + name + "'") + "을(를) 사용했습니다. 사용 이력에 기록되었습니다.");
        } catch (DbException e) {
            redirect.addFlashAttribute("error", "사용할 수 없습니다. " + e.getMessage());
        }
        return "redirect:/mine";
    }

    @PostMapping("/mine/{grantNo}/cancel")
    public String cancel(@SessionAttribute(WebConfig.LOGIN_ID) String loginId, @PathVariable("grantNo") int grantNo,
                         @RequestParam(value = "name", required = false) String name, RedirectAttributes redirect) {
        try {
            benefitService.cancel(grantNo, loginId);
            redirect.addFlashAttribute("notice", (name == null ? "혜택" : "'" + name + "'") + " 응모를 취소했습니다.");
        } catch (DbException e) {
            redirect.addFlashAttribute("error", "취소할 수 없습니다. " + e.getMessage());
        }
        return "redirect:/mine";
    }
}
