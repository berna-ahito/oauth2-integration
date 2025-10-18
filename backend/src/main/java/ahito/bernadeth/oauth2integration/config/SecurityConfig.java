package ahito.bernadeth.oauth2integration.config;

import ahito.bernadeth.oauth2integration.security.AppOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    // React dev server origin
    private static final String FRONTEND = "http://localhost:5173";

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, AppOAuth2UserService appOAuth2UserService) throws Exception {
        http
                // SPA dev: disable CSRF for now (we're using same-site session cookie)
                .csrf(csrf -> csrf.disable())

                // CORS so the React app can call the API with credentials
                .cors(Customizer.withDefaults())

                // H2 console needs frames; allow same-origin if you use /h2-console
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))

                .authorizeHttpRequests(auth -> auth
                        // public health & info
                        .requestMatchers(HttpMethod.GET, "/", "/error", "/api/public/**").permitAll()
                        // OAuth2 endpoints used during login
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        // H2 console (dev only)
                        .requestMatchers("/h2-console/**").permitAll()
                        // everything else requires auth
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth -> oauth
                        // make sure our user provisioning runs (creates/updates DB user + avatar, etc.)
                        .userInfoEndpoint(ui -> ui.userService(appOAuth2UserService))
                        // after successful login, go back to the React profile page
                        .defaultSuccessUrl(FRONTEND + "/profile", true)
                        // on failure, land on Home with an error flag
                        .failureUrl(FRONTEND + "/?error=oauth")
                )

                .logout(logout -> logout
                        // when logging out, send the browser back to Home with a logout flag
                        .logoutSuccessUrl(FRONTEND + "/?logout=1")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(FRONTEND));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true); // send the session cookie

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
