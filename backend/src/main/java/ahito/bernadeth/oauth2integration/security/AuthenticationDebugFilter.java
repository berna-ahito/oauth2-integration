package ahito.bernadeth.oauth2integration.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthenticationDebugFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Log OAuth2 related requests
        if (uri.contains("oauth2") || uri.contains("login")) {
            System.out.println("\n🔐 ========== AUTH REQUEST ==========");
            System.out.println("🔐 Method: " + method);
            System.out.println("🔐 URI: " + uri);
            System.out.println("🔐 Query: " + request.getQueryString());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("🔐 Current Auth: " + (auth != null ? auth.getClass().getSimpleName() : "null"));
            System.out.println("🔐 ====================================\n");
        }

        filterChain.doFilter(request, response);

        // Log authentication after the request
        if (uri.contains("oauth2") || uri.contains("login")) {
            Authentication authAfter = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("\n🔐 ========== AFTER AUTH REQUEST ==========");
            System.out.println("🔐 URI: " + uri);
            System.out.println("🔐 Auth After: " + (authAfter != null ? authAfter.getClass().getSimpleName() : "null"));
            if (authAfter != null) {
                System.out.println("🔐 Principal: " + authAfter.getPrincipal());
            }
            System.out.println("🔐 ==========================================\n");
        }
    }
}