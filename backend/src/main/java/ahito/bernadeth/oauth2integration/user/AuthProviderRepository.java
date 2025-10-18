package ahito.bernadeth.oauth2integration.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AuthProviderRepository extends JpaRepository<AuthProvider, Long> {
    Optional<AuthProvider> findByProviderAndProviderUserId(
            AuthProvider.Provider provider, String providerUserId);
}
