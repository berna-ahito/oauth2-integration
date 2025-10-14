package ahito.bernadeth.oauth2integration.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "auth_provider",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "providerUserId"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthProvider {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Provider provider; // GOOGLE or GITHUB

    @Column(nullable = false)
    private String providerUserId;

    private String providerEmail;

    public enum Provider { GOOGLE, GITHUB }
}
