package ahito.bernadeth.oauth2integration.security;

import ahito.bernadeth.oauth2integration.user.AuthProvider;
import ahito.bernadeth.oauth2integration.user.AuthProviderRepository;
import ahito.bernadeth.oauth2integration.user.User;
import ahito.bernadeth.oauth2integration.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository users;
    private final AuthProviderRepository providers;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(req);
        String regId = req.getClientRegistration().getRegistrationId();
        Map<String, Object> attrs = oAuth2User.getAttributes();

        if ("google".equals(regId)) {
            String sub = (String) attrs.get("sub");
            String email = (String) attrs.get("email");
            String name = (String) attrs.getOrDefault("name", email);
            String picture = (String) attrs.get("picture");
            upsert(AuthProvider.Provider.GOOGLE, sub, email, name, picture);
        } else if ("github".equals(regId)) {
            String id = String.valueOf(attrs.get("id"));
            String email = (String) attrs.get("email"); // may be null if hidden
            String name = (String) attrs.getOrDefault("name", (String) attrs.get("login"));
            String avatar = (String) attrs.get("avatar_url");
            upsert(AuthProvider.Provider.GITHUB, id, email, name, avatar);
        }
        return oAuth2User;
    }

    private void upsert(AuthProvider.Provider provider, String providerUserId,
                        String providerEmail, String displayName, String avatarUrl) {
        var link = providers.findByProviderAndProviderUserId(provider, providerUserId).orElse(null);
        if (link != null) {
            var u = link.getUser();
            if (displayName != null && !displayName.isBlank()) u.setDisplayName(displayName);
            if (avatarUrl != null) u.setAvatarUrl(avatarUrl);
            if (providerEmail != null && !providerEmail.isBlank()) u.setEmail(providerEmail);
            users.save(u);
            return;
        }
        var user = (providerEmail == null || providerEmail.isBlank())
                ? users.save(User.builder()
                .email(provider.name().toLowerCase() + "-" + providerUserId + "@example.local")
                .displayName(displayName)
                .avatarUrl(avatarUrl)
                .build())
                : users.findByEmail(providerEmail).orElseGet(() ->
                users.save(User.builder()
                        .email(providerEmail)
                        .displayName(displayName != null ? displayName : providerEmail)
                        .avatarUrl(avatarUrl)
                        .build()));
        providers.save(AuthProvider.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .providerEmail(providerEmail)
                .build());
    }
}
