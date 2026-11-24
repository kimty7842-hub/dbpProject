package bloodplus.controller;

import bloodplus.WebConfig;
import bloodplus.domain.Fmt;
import bloodplus.domain.Member;
import bloodplus.repository.DbException;
import bloodplus.service.MemberService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 내 정보: 회원 정보 조회(③), 수정(UPDATE), 탈퇴 */
@Controller
public class ProfileController {

    private final MemberService memberService;

    public ProfileController(MemberService memberService) { this.memberService = memberService; }

    @GetMapping("/profile")
    public String profile(@SessionAttribute(WebConfig.LOGIN_ID) String loginId, Model model) {
        Member me = memberService.find(loginId);
        fill(model, me, me.getCity(), detailOf(me.getAddress()));
        return "profile";
    }

    @PostMapping("/profile")
    public String update(@SessionAttribute(WebConfig.LOGIN_ID) String loginId,
                         @RequestParam("name") String name, @RequestParam("phone") String phone,
                         @RequestParam("city") String city, @RequestParam("detail") String detail,
                         @RequestParam(value = "newPassword", required = false) String newPassword,
                         @RequestParam(value = "passwordConfirm", required = false) String passwordConfirm,
                         HttpSession session, Model model, RedirectAttributes redirect) {
        Member m = new Member(loginId, null, name.trim(), phone.trim(), (city + " " + detail.trim()).trim());
        try {
            memberService.update(m, newPassword, passwordConfirm);
        } catch (DbException e) {
            model.addAttribute("error", e.getMessage());
            fill(model, m, city, detail);
            return "profile";
        }
        session.setAttribute(WebConfig.LOGIN_NAME, m.getName());
        redirect.addFlashAttribute("notice", "회원 정보를 저장했습니다.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/withdraw")
    public String withdraw(@SessionAttribute(WebConfig.LOGIN_ID) String loginId, HttpSession session, RedirectAttributes redirect) {
        try {
            memberService.withdraw(loginId);
        } catch (DbException e) {
            redirect.addFlashAttribute("error", "탈퇴하지 못했습니다. " + e.getMessage());
            return "redirect:/profile";
        }
        session.invalidate();
        redirect.addFlashAttribute("notice", "탈퇴했습니다. 그동안 헌혈에 참여해 주셔서 감사합니다.");
        return "redirect:/login";
    }

    private void fill(Model model, Member m, String city, String detail) {
        model.addAttribute("active", "profile");
        model.addAttribute("member", m);
        model.addAttribute("cities", Fmt.CITIES);
        model.addAttribute("city", city);
        model.addAttribute("detail", detail);
    }

    private static String detailOf(String address) {
        if (address == null) return "";
        int i = address.indexOf(' ');
        return i < 0 ? "" : address.substring(i + 1);
    }
}
