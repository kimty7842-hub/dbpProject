package bloodplus.controller;

import bloodplus.WebConfig;
import bloodplus.repository.DbException;
import bloodplus.repository.SqlLog;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;

/** 모든 화면 공통: 상단에 보일 이름, 그리고 예상하지 못한 DB 오류 처리 */
@ControllerAdvice
public class GlobalControllerAdvice {


    @ModelAttribute
    public void common(HttpSession session, Model model) {
        model.addAttribute("loginName", session.getAttribute(WebConfig.LOGIN_NAME));
    }

    @ExceptionHandler(DbException.class)
    public String dbError(DbException e, HttpSession session, Model model) {
        common(session, model);   // 오류 화면에도 상단 메뉴가 보이도록
        model.addAttribute("message", e.getMessage());
        return "error-page";
    }

    /** DB 에 연결하지 못했을 때 등 (CannotGetJdbcConnectionException 포함). 자세한 원인은 SQL 로그 화면에 표시한다. */
    @ExceptionHandler(DataAccessException.class)
    public String dataAccessError(DataAccessException e, HttpSession session, Model model) {
        common(session, model);
        Throwable root = e.getMostSpecificCause();
        SqlLog.error(root.getMessage() == null ? root.toString() : root.getMessage().trim());
        model.addAttribute("message", "지금은 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해 주시기 바랍니다.");
        return "error-page";
    }
}
