package ahito.bernadeth.oauth2integration.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home(Model model, Principal principal, HttpServletRequest request) {
        // show/hide UI bits
        model.addAttribute("loggedIn", principal != null);

        // read & clear the one-shot logout message
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object v = session.getAttribute("LOGOUT_MSG");
            if (Boolean.TRUE.equals(v)) {
                model.addAttribute("logout", true);
                session.removeAttribute("LOGOUT_MSG");
            }
        }

        return "home";
    }
}
