package bloodplus.controller;

import bloodplus.WebConfig;
import bloodplus.domain.Donation;
import bloodplus.domain.Fmt;
import bloodplus.domain.Member;
import bloodplus.repository.DbException;
import bloodplus.service.BenefitService;
import bloodplus.service.DonationService;
import bloodplus.service.MemberService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/** 헌혈 기록: 아이디 기준 조회, 등록, 수정, 삭제 (③) */
@Controller
public class DonationController {

    private final DonationService donationService;
    private final BenefitService benefitService;
    private final MemberService memberService;

    public DonationController(DonationService donationService, BenefitService benefitService, MemberService memberService) {
        this.donationService = donationService; this.benefitService = benefitService; this.memberService = memberService;
    }

    @GetMapping("/donations")
    public String list(@SessionAttribute(WebConfig.LOGIN_ID) String loginId, Model model) {
        model.addAttribute("active", "donations");
        model.addAttribute("donations", donationService.list(loginId));
        return "donations/list";
    }

    @GetMapping("/donations/new")
    public String createForm(@SessionAttribute(WebConfig.LOGIN_ID) String loginId, Model model) {
        Member me = memberService.find(loginId);
        fillForm(model, new Donation(0, loginId, LocalDate.now(), "전혈", Fmt.regionOf(me.getAddress())), false);
        return "donations/form";
    }

    @PostMapping("/donations/new")
    public String create(@SessionAttribute(WebConfig.LOGIN_ID) String loginId,
                         @RequestParam("date") String date, @RequestParam("type") String type, @RequestParam("region") String region,
                         Model model, RedirectAttributes redirect) {
        try {
            donationService.add(loginId, date, type, region);
        } catch (DbException e) {
            model.addAttribute("error", e.getMessage());
            fillForm(model, new Donation(0, loginId, null, type, region), false);
            model.addAttribute("dateText", date);
            return "donations/form";
        }
        // 새 헌혈로 응모할 수 있게 된 혜택이 있는지 저장 프로시저로 확인
        int count = donationService.list(loginId).size();
        int eligible = benefitService.eligible(loginId).size();
        redirect.addFlashAttribute("notice", count + "번째 헌혈 기록을 등록했습니다."
                + (eligible > 0 ? " 지금 응모할 수 있는 혜택이 " + eligible + "개 있습니다." : ""));
        if (eligible > 0) redirect.addFlashAttribute("noticeLink", "/benefits/eligible");
        return "redirect:/donations";
    }

    @GetMapping("/donations/{no}/edit")
    public String editForm(@SessionAttribute(WebConfig.LOGIN_ID) String loginId, @PathVariable("no") int no, Model model) {
        fillForm(model, donationService.find(no, loginId), true);
        return "donations/form";
    }

    @PostMapping("/donations/{no}/edit")
    public String edit(@SessionAttribute(WebConfig.LOGIN_ID) String loginId, @PathVariable("no") int no,
                       @RequestParam("date") String date, @RequestParam("type") String type, @RequestParam("region") String region,
                       Model model, RedirectAttributes redirect) {
        try {
            donationService.update(no, loginId, date, type, region);
        } catch (DbException e) {
            model.addAttribute("error", e.getMessage());
            fillForm(model, new Donation(no, loginId, null, type, region), true);
            model.addAttribute("dateText", date);
            return "donations/form";
        }
        redirect.addFlashAttribute("notice", "헌혈번호 " + no + " 기록을 고쳤습니다.");
        return "redirect:/donations";
    }

    @PostMapping("/donations/{no}/delete")
    public String delete(@SessionAttribute(WebConfig.LOGIN_ID) String loginId, @PathVariable("no") int no, RedirectAttributes redirect) {
        try {
            donationService.delete(no, loginId);
            redirect.addFlashAttribute("notice", "헌혈번호 " + no + " 기록을 삭제했습니다.");
        } catch (DbException e) {
            redirect.addFlashAttribute("error", "삭제하지 못했습니다. " + e.getMessage());
        }
        return "redirect:/donations";
    }

    private void fillForm(Model model, Donation d, boolean editing) {
        model.addAttribute("active", "donations");
        model.addAttribute("donation", d);
        model.addAttribute("editing", editing);
        model.addAttribute("pageTitle", editing ? "헌혈 기록 수정" : "헌혈 기록 추가");
        model.addAttribute("dateText", d.getDate() == null ? "" : d.getDate().toString());
        model.addAttribute("today", LocalDate.now().toString());
        model.addAttribute("types", Fmt.DONATION_TYPES);
        model.addAttribute("regions", Fmt.REGIONS);
    }
}
