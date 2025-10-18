package ahito.bernadeth.oauth2integration.web;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public String handleOauth(OAuth2AuthenticationException ex, Model model) {
        model.addAttribute("status", 401);
        model.addAttribute("error", "OAuth2 Authentication Failed");
        model.addAttribute("message", ex.getError().getDescription());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleAll(Exception ex, Model model) {
        model.addAttribute("status", 500);
        model.addAttribute("error", "Unexpected Error");
        model.addAttribute("message", ex.getMessage());
        return "error";
    }
}
