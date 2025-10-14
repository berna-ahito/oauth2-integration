package ahito.bernadeth.oauth2integration.web;

import ahito.bernadeth.oauth2integration.user.AuthProvider;
import ahito.bernadeth.oauth2integration.user.AuthProviderRepository;
import ahito.bernadeth.oauth2integration.user.User;
import ahito.bernadeth.oauth2integration.user.UserRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Validated
public class ProfileController {

    private final UserRepository users;
    private final AuthProviderRepository providers;

    @GetMapping("/profile")
    public String viewProfile(@AuthenticationPrincipal OAuth2User principal, Model model) {
        User u = resolveOrCreateUser(principal.getAttributes());
        model.addAttribute("user", u);
        model.addAttribute("form", new ProfileForm(u.getDisplayName(), u.getBio()));
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal OAuth2User principal, @Validated ProfileForm form) {
        User u = resolveOrCreateUser(principal.getAttributes());
        u.setDisplayName(form.getDisplayName());
        u.setBio(form.getBio());
        users.save(u);
        return "redirect:/profile";
    }

    private User resolveOrCreateUser(Map<String, Object> a) {
        // 1) Try email if present
        String email = (String) a.get("email");
        if (email != null && !email.isBlank()) {
            return users.findByEmail(email).orElseGet(() -> createFromAttrs(a, email));
        }

        // 2) Fallback by provider user id (Google uses "sub", GitHub uses "id")
        String googleSub = a.get("sub") != null ? String.valueOf(a.get("sub")) : null;
        if (googleSub != null) {
            return providers.findByProviderAndProviderUserId(AuthProvider.Provider.GOOGLE, googleSub)
                    .map(AuthProvider::getUser)
                    .orElseGet(() -> createFromAttrs(a, "google-" + googleSub + "@example.local"));
        }

        String ghId = a.get("id") != null ? String.valueOf(a.get("id")) : null;
        if (ghId != null) {
            return providers.findByProviderAndProviderUserId(AuthProvider.Provider.GITHUB, ghId)
                    .map(AuthProvider::getUser)
                    .orElseGet(() -> createFromAttrs(a, "github-" + ghId + "@example.local"));
        }

        // 3) Last resort (shouldn’t happen with Google/GitHub)
        return users.save(User.builder()
                .email("anon-" + System.currentTimeMillis() + "@example.local")
                .displayName((String) a.getOrDefault("name", "User"))
                .avatarUrl(pickAvatar(a))
                .build());
    }

    private User createFromAttrs(Map<String, Object> a, String email) {
        User u = users.save(User.builder()
                .email(email)
                .displayName((String) a.getOrDefault("name", email))
                .avatarUrl(pickAvatar(a))
                .build());

        // Link provider if we can
        if (a.get("sub") != null) {
            providers.save(AuthProvider.builder()
                    .user(u)
                    .provider(AuthProvider.Provider.GOOGLE)
                    .providerUserId(String.valueOf(a.get("sub")))
                    .providerEmail((String) a.get("email"))
                    .build());
        } else if (a.get("id") != null) {
            providers.save(AuthProvider.builder()
                    .user(u)
                    .provider(AuthProvider.Provider.GITHUB)
                    .providerUserId(String.valueOf(a.get("id")))
                    .providerEmail((String) a.get("email"))
                    .build());
        }
        return u;
    }

    private String pickAvatar(Map<String, Object> a) {
        String pic = (String) a.get("picture");     // Google
        if (pic == null) pic = (String) a.get("avatar_url"); // GitHub
        return pic;
    }

    @Data
    public static class ProfileForm {
        @NotBlank private String displayName;
        private String bio;
        public ProfileForm(String displayName, String bio) { this.displayName = displayName; this.bio = bio; }
    }
}
