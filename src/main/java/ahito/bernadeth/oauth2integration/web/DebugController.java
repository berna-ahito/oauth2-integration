package ahito.bernadeth.oauth2integration.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DebugController {

    @GetMapping("/me")
    @ResponseBody
    public Object me(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) return "No principal (not logged in)";
        // This prints raw attributes as JSON so there’s no Thymeleaf involved.
        return principal.getAttributes();
    }
}
