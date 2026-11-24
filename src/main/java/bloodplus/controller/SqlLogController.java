package bloodplus.controller;

import bloodplus.repository.SqlLog;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/** 실행된 SQL, 프로시저, 트리거 기록 화면 (발표, 결과보고서 캡처용) */
@Controller
public class SqlLogController {

    @GetMapping("/sql-log")
    public String log(Model model) {
        model.addAttribute("active", "sql");
        model.addAttribute("entries", SqlLog.recent());
        return "sql-log";
    }

    @PostMapping("/sql-log/clear")
    public String clear() {
        SqlLog.clear();
        return "redirect:/sql-log";
    }
}
