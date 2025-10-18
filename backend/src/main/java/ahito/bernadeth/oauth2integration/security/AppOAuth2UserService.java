package ahito.bernadeth.oauth2integration.security;

import ahito.bernadeth.oauth2integration.user.AuthProvider;
import ahito.bernadeth.oauth2integration.user.AuthProviderRepository;
import ahito.bernadeth.oauth2integration.user.User;
import ahito.bernadeth.oauth2integration.user.UserRepository;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AppOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository users;
    private final AuthProviderRepository authProviders;

    public AppOAuth2UserService(UserRepository users, AuthProviderRepository authProviders) {
        this.users = users;
        this.authProviders = authProviders;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest req) {
        OAuth2User o = super.loadUser(req);

        // Which provider? ("google" | "github")
        final String regId = req.getClientRegistration().getRegistrationId();
        final AuthProvider.Provider providerType =
                "github".equalsIgnoreCase(regId) ? AuthProvider.Provider.GITHUB : AuthProvider.Provider.GOOGLE;

        // Normalize attributes
        Map<String, Object> a = o.getAttributes();
        String providerUserId;
        String email;
        String name;
        String picture = null;

        if (providerType == AuthProvider.Provider.GITHUB) {
            providerUserId = String.valueOf(a.get("id"));
            email = (String) a.get("email");              // often null when email is private
            if (email == null) {
                email = fetchGithubPrimaryEmail(req);     // <-- fetch primary/verified via /user/emails
            }
            if (email == null && a.get("login") != null) {
                // last-resort stable fallback (should rarely be used after the API call above)
                email = a.get("login") + "@users.noreply.github.com";
            }
            Object ghName = a.get("name");
            Object login = a.get("login");
            name = ghName != null ? ghName.toString() : (login != null ? login.toString() : "GitHub User");
            picture = (String) a.get("avatar_url");
        } else {
            providerUserId = (String) a.get("sub");
            email = (String) a.get("email");
            Object nm = a.get("name");
            name = nm != null ? nm.toString() : email;
            picture = (String) a.get("picture");
        }

        // ----- Find or create local user (no lambdas to avoid "effectively final" issues) -----
        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            User nu = new User();
            nu.setEmail(email);
            nu.setDisplayName(name);
            nu.setAvatarUrl(picture);
            nu.setBio(null);
            user = users.save(nu);
        }

        // Keep user info up to date
        boolean changed = false;
        if (name != null && !name.equals(user.getDisplayName())) {
            user.setDisplayName(name);
            changed = true;
        }
        if (picture != null && (user.getAvatarUrl() == null || !picture.equals(user.getAvatarUrl()))) {
            user.setAvatarUrl(picture);
            changed = true;
        }
        if (changed) {
            users.save(user);
        }

        // Ensure AuthProvider record exists for this user + provider
        Optional<ahito.bernadeth.oauth2integration.user.AuthProvider> existing =
                authProviders.findByProviderAndProviderUserId(providerType, providerUserId);
        if (existing.isEmpty()) {
            ahito.bernadeth.oauth2integration.user.AuthProvider link =
                    ahito.bernadeth.oauth2integration.user.AuthProvider.builder()
                            .provider(providerType)
                            .providerUserId(providerUserId)
                            .providerEmail(email)
                            .user(user)
                            .build();
            authProviders.save(link);
        }

        // Return unified principal
        return new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "name", user.getDisplayName(),
                        "picture", user.getAvatarUrl()
                ),
                "email"
        );
    }

    /**
     * Call GitHub's /user/emails with the access token to get the primary/verified email.
     * Requires the "user:email" scope (you already configured this).
     */
    private String fetchGithubPrimaryEmail(OAuth2UserRequest req) {
        try {
            String token = req.getAccessToken().getTokenValue();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token); // or "token " works too
            headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            RestTemplate rt = new RestTemplate();
            ResponseEntity<List<Map<String, Object>>> resp = rt.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return null;

            // Prefer primary+verified
            for (Map<String, Object> e : resp.getBody()) {
                Boolean primary = (Boolean) e.get("primary");
                Boolean verified = (Boolean) e.get("verified");
                String email = (String) e.get("email");
                if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) return email;
            }
            // Else any verified
            for (Map<String, Object> e : resp.getBody()) {
                Boolean verified = (Boolean) e.get("verified");
                String email = (String) e.get("email");
                if (Boolean.TRUE.equals(verified)) return email;
            }
            // Else first available
            if (!resp.getBody().isEmpty()) {
                Object email = resp.getBody().get(0).get("email");
                return email != null ? email.toString() : null;
            }
        } catch (Exception ignored) {
            // swallow and return null → we'll fall back to noreply
        }
        return null;
    }
}
