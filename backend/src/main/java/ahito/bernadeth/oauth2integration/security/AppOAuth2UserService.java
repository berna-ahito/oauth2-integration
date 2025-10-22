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
        System.out.println("🏗️  AppOAuth2UserService CONSTRUCTOR called!");
        System.out.println("🏗️  Instance: " + this);
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest req) {
        System.out.println("\n========================================");
        System.out.println("🚀 OAUTH2 LOGIN STARTED - loadUser() CALLED!");
        System.out.println("🚀 This instance: " + this);
        System.out.println("🚀 Request: " + req);
        System.out.println("========================================");

        OAuth2User o = super.loadUser(req);

        // Which provider? ("google" | "github")
        final String regId = req.getClientRegistration().getRegistrationId();
        System.out.println("🔍 Provider Registration ID: " + regId);

        final AuthProvider.Provider providerType =
                "github".equalsIgnoreCase(regId) ? AuthProvider.Provider.GITHUB : AuthProvider.Provider.GOOGLE;
        System.out.println("📋 Provider Type Enum: " + providerType);

        // Normalize attributes
        Map<String, Object> a = o.getAttributes();
        System.out.println("📦 Raw OAuth2 Attributes: " + a.keySet());

        String providerUserId;
        String email;
        String name;
        String picture = null;

        if (providerType == AuthProvider.Provider.GITHUB) {
            System.out.println("\n--- Processing GITHUB Login ---");
            providerUserId = String.valueOf(a.get("id"));
            System.out.println("🆔 GitHub User ID: " + providerUserId);

            email = (String) a.get("email");
            System.out.println("📧 GitHub email from attributes: " + email);

            if (email == null) {
                System.out.println("⚠️  Email is null, fetching from GitHub API...");
                email = fetchGithubPrimaryEmail(req);
                System.out.println("📧 Fetched email from API: " + email);
            }
            if (email == null && a.get("login") != null) {
                email = a.get("login") + "@users.noreply.github.com";
                System.out.println("📧 Using fallback noreply email: " + email);
            }
            Object ghName = a.get("name");
            Object login = a.get("login");
            name = ghName != null ? ghName.toString() : (login != null ? login.toString() : "GitHub User");
            picture = (String) a.get("avatar_url");

        } else {
            System.out.println("\n--- Processing GOOGLE Login ---");
            providerUserId = (String) a.get("sub");
            System.out.println("🆔 Google User ID (sub): " + providerUserId);

            email = (String) a.get("email");
            System.out.println("📧 Google email: " + email);

            Object nm = a.get("name");
            name = nm != null ? nm.toString() : email;
            picture = (String) a.get("picture");
        }

        System.out.println("\n--- Extracted User Info ---");
        System.out.println("👤 Name: " + name);
        System.out.println("📧 Email: " + email);
        System.out.println("🖼️  Picture URL: " + picture);
        System.out.println("🆔 Provider User ID: " + providerUserId);

        // ----- Find or create local user -----
        System.out.println("\n--- Finding/Creating User ---");
        User user = users.findByEmail(email).orElse(null);

        if (user == null) {
            System.out.println("➕ User not found, creating new user...");
            User nu = new User();
            nu.setEmail(email);
            nu.setDisplayName(name);
            nu.setAvatarUrl(picture);
            nu.setBio(null);
            user = users.save(nu);
            System.out.println("✅ New user created with ID: " + user.getId());
        } else {
            System.out.println("✅ Existing user found with ID: " + user.getId());
        }

        // Keep user info up to date
        boolean changed = false;
        if (name != null && !name.equals(user.getDisplayName())) {
            System.out.println("🔄 Updating display name: " + user.getDisplayName() + " → " + name);
            user.setDisplayName(name);
            changed = true;
        }
        if (picture != null && (user.getAvatarUrl() == null || !picture.equals(user.getAvatarUrl()))) {
            System.out.println("🔄 Updating avatar URL");
            user.setAvatarUrl(picture);
            changed = true;
        }
        if (changed) {
            users.save(user);
            System.out.println("✅ User info updated");
        }

        // Ensure AuthProvider record exists for this user + provider
        System.out.println("\n--- Checking AuthProvider Record ---");
        System.out.println("🔍 Looking for: Provider=" + providerType + ", ProviderUserId=" + providerUserId);

        Optional<AuthProvider> existing =
                authProviders.findByProviderAndProviderUserId(providerType, providerUserId);

        System.out.println("🔍 Existing AuthProvider found: " + existing.isPresent());

        if (existing.isEmpty()) {
            System.out.println("➕ Creating new AuthProvider record...");
            System.out.println("   - Provider: " + providerType);
            System.out.println("   - Provider User ID: " + providerUserId);
            System.out.println("   - Provider Email: " + email);
            System.out.println("   - Linked to User ID: " + user.getId());

            try {
                AuthProvider link = AuthProvider.builder()
                        .provider(providerType)
                        .providerUserId(providerUserId)
                        .providerEmail(email)
                        .user(user)
                        .build();

                AuthProvider saved = authProviders.save(link);
                System.out.println("✅ AuthProvider saved with ID: " + saved.getId());

            } catch (Exception e) {
                System.err.println("❌ ERROR saving AuthProvider: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        } else {
            System.out.println("✅ AuthProvider record already exists with ID: " + existing.get().getId());
        }

        // Return unified principal
        System.out.println("\n--- Creating OAuth2User Principal ---");
        Map<String, Object> principalAttrs = Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getDisplayName(),
                "picture", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
        );
        System.out.println("📋 Principal attributes: " + principalAttrs);

        DefaultOAuth2User principal = new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                principalAttrs,
                "email"
        );

        System.out.println("✅ OAuth2User created successfully");
        System.out.println("========================================");
        System.out.println("🎉 OAUTH2 LOGIN COMPLETED");
        System.out.println("========================================\n");

        return principal;
    }

    /**
     * Call GitHub's /user/emails with the access token to get the primary/verified email.
     * Requires the "user:email" scope (you already configured this).
     */
    private String fetchGithubPrimaryEmail(OAuth2UserRequest req) {
        try {
            String token = req.getAccessToken().getTokenValue();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            RestTemplate rt = new RestTemplate();
            ResponseEntity<List<Map<String, Object>>> resp = rt.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                System.out.println("⚠️  GitHub API returned non-success status or null body");
                return null;
            }

            // Prefer primary+verified
            for (Map<String, Object> e : resp.getBody()) {
                Boolean primary = (Boolean) e.get("primary");
                Boolean verified = (Boolean) e.get("verified");
                String email = (String) e.get("email");
                if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                    System.out.println("✅ Found primary+verified email: " + email);
                    return email;
                }
            }
            // Else any verified
            for (Map<String, Object> e : resp.getBody()) {
                Boolean verified = (Boolean) e.get("verified");
                String email = (String) e.get("email");
                if (Boolean.TRUE.equals(verified)) {
                    System.out.println("✅ Found verified email: " + email);
                    return email;
                }
            }
            // Else first available
            if (!resp.getBody().isEmpty()) {
                Object email = resp.getBody().get(0).get("email");
                System.out.println("⚠️  Using first available email: " + email);
                return email != null ? email.toString() : null;
            }
        } catch (Exception e) {
            System.err.println("❌ Error fetching GitHub email: " + e.getMessage());
        }
        return null;
    }
}