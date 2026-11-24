package bloodplus;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 로그인하지 않았으면 로그인 화면으로 보낸다. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    public static final String LOGIN_ID = "loginId";
    public static final String LOGIN_NAME = "loginName";

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                if (request.getSession().getAttribute(LOGIN_ID) != null) return true;
                response.sendRedirect(request.getContextPath() + "/login");
                return false;
            }
        }).addPathPatterns("/**").excludePathPatterns("/login", "/signup", "/css/**", "/error", "/favicon.ico");
    }
}
