package bloodplus.controller;

import bloodplus.WebConfig;
import bloodplus.domain.Fmt;
import bloodplus.domain.Member;
import bloodplus.repository.DbException;
import bloodplus.service.MemberService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/** 로그인, 로그아웃, 회원가입 */
@Controller
public class LoginController {

    private final MemberService memberService;

    public LoginController(MemberService memberService) { this.memberService = memberService; }

    @GetMapping("/login")
    public String loginForm(HttpSession session) {
        if (session.getAttribute(WebConfig.LOGIN_ID) != null) return "redirect:/";
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("id") String id, @RequestParam("password") String password, HttpSession session, Model model) {
        if (id.isBlank() || password.isEmpty()) {
            model.addAttribute("error", "아이디와 비밀번호를 입력해 주시기 바랍니다.");
            model.addAttribute("id", id);
            return "login";
        }
        Optional<Member> m = memberService.login(id.trim(), password);
        if (m.isEmpty()) {
            model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
            model.addAttribute("id", id);
            return "login";
        }
        session.setAttribute(WebConfig.LOGIN_ID, m.get().getId());
        session.setAttribute(WebConfig.LOGIN_NAME, m.get().getName());
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("cities", Fmt.CITIES);
        model.addAttribute("form", new Member());
        model.addAttribute("city", Fmt.CITIES[0]);
        model.addAttribute("detail", "");
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam("name") String name, @RequestParam("id") String id,
                         @RequestParam("password") String password, @RequestParam("passwordConfirm") String passwordConfirm,
                         @RequestParam("phone") String phone, @RequestParam("city") String city, @RequestParam("detail") String detail,
                         Model model, RedirectAttributes redirect) {
        Member m = new Member(id.trim(), password, name.trim(), phone.trim(), (city + " " + detail.trim()).trim());
        try {
            memberService.join(m, passwordConfirm);
        } catch (DbException e) {
            m.setPassword(null);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("form", m);
            model.addAttribute("cities", Fmt.CITIES);
            model.addAttribute("city", city);
            model.addAttribute("detail", detail);
            return "signup";
        }
        redirect.addFlashAttribute("notice", m.getName() + "님, 가입을 환영합니다. 로그인해 주시기 바랍니다.");
        redirect.addFlashAttribute("id", m.getId());
        return "redirect:/login";
    }
}
