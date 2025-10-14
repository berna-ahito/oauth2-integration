package ahito.bernadeth.oauth2integration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Public vs protected endpoints
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/home", "/css/**", "/images/**", "/h2-console/**").permitAll()
                        .anyRequest().authenticated()
                )
                // Use our home page as the login page
                .oauth2Login(oauth -> oauth.loginPage("/"))

                // CSRF + frames so H2 console works in dev (safe to keep)
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                // Logout -> set a one-time session flag, then redirect to clean "/"
                .logout(logout -> logout
                        .logoutSuccessHandler((request, response, authentication) -> {
                            request.getSession(true).setAttribute("LOGOUT_MSG", Boolean.TRUE);
                            response.sendRedirect("/"); // clean URL, no query params
                        })
                        .permitAll()
                );

        return http.build();
    }
}
