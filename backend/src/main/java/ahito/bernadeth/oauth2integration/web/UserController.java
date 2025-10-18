package ahito.bernadeth.oauth2integration.web;

import ahito.bernadeth.oauth2integration.user.User;
import ahito.bernadeth.oauth2integration.user.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class UserController {

    private final UserRepository users;

    public UserController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of("app", "oauth2integration", "status", "ok");
    }

    @GetMapping("/api/public/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok");
    }

    @GetMapping(value = "/api/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        String email = principal.getAttribute("email");
        String provName = principal.getAttribute("name");
        String provPicture = principal.getAttribute("picture");

        if (email != null) {
            User u = users.findByEmail(email).orElse(null);
            if (u != null) {
                return ResponseEntity.ok(Map.of(
                        "authenticated", true,
                        "email", u.getEmail(),
                        "name",  u.getDisplayName() != null ? u.getDisplayName() : provName,
                        "picture", u.getAvatarUrl() != null ? u.getAvatarUrl() : provPicture,
                        "bio", u.getBio() != null ? u.getBio() : ""
                ));
            }
        }

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "email", email,
                "name", provName,
                "picture", provPicture,
                "bio", ""
        ));
    }

    @PostMapping(value = "/api/profile", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> updateProfile(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody Map<String, String> body) {

        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("ok", false, "error", "unauthorized"));
        }

        String email = principal.getAttribute("email");
        if (email == null) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "no-email"));
        }

        // Find user; if missing (rare), initialize from principal so avatar/name are set
        User u = users.findByEmail(email).orElse(null);
        if (u == null) {
            u = new User();
            u.setEmail(email);
            String provName = principal.getAttribute("name");
            String provPicture = principal.getAttribute("picture");
            u.setDisplayName(provName);
            u.setAvatarUrl(provPicture); // initialize avatar for Google/GitHub-created users
        }

        String displayName = body.getOrDefault("displayName", u.getDisplayName());
        String bio = body.getOrDefault("bio", u.getBio());

        u.setDisplayName(displayName);
        u.setBio(bio);
        users.save(u);

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "email", u.getEmail(),
                "displayName", u.getDisplayName(),
                "bio", u.getBio(),
                "avatarUrl", u.getAvatarUrl()
        ));
    }
}
