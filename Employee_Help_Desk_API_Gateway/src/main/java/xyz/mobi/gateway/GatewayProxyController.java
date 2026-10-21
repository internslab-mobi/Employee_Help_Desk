package xyz.mobi.gateway;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Enumeration;
import java.util.Set;

@RestController
public class GatewayProxyController {

    private static final Logger log = LoggerFactory.getLogger(GatewayProxyController.class);

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade",
            "host",
            "content-length"
    );

    private static final Set<String> CORS_HEADERS = Set.of(
            "access-control-allow-origin",
            "access-control-allow-credentials",
            "access-control-allow-methods",
            "access-control-allow-headers",
            "access-control-expose-headers",
            "access-control-max-age"
    );

    private final HttpClient httpClient;
    private final String authServiceUrl;
    private final String backendServiceUrl;

    public GatewayProxyController(
            @Value("${gateway.services.auth-url:http://localhost:8081}") String authServiceUrl,
            @Value("${gateway.services.backend-url:http://localhost:8082}") String backendServiceUrl) {
        this.authServiceUrl = authServiceUrl.endsWith("/") ? authServiceUrl.substring(0, authServiceUrl.length() - 1) : authServiceUrl;
        this.backendServiceUrl = backendServiceUrl.endsWith("/") ? backendServiceUrl.substring(0, backendServiceUrl.length() - 1) : backendServiceUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @RequestMapping(value = {
            "/user", "/user/**",
            "/auth", "/auth/**",
            "/helpdesk/**",
            "/tickets", "/tickets/**",
            "/analytics", "/analytics/**",
            "/reports", "/reports/**",
            "/notifications", "/notifications/**",
            "/departments", "/departments/**",
            "/categories", "/categories/**",
            "/sub-categories", "/sub-categories/**",
            "/api/**"
    })
    public void proxy(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestUri = request.getRequestURI();
        String targetBaseUrl;
        String forwardedPath = requestUri;

        // Strip /api prefix if present
        if (forwardedPath.startsWith("/api/")) {
            forwardedPath = forwardedPath.substring(4);
        } else if (forwardedPath.equals("/api")) {
            forwardedPath = "";
        }

        // Route to auth service or backend service
        if (isAuthServicePath(forwardedPath)) {
            targetBaseUrl = authServiceUrl;
        } else {
            targetBaseUrl = backendServiceUrl;
        }

        String queryString = request.getQueryString();
        String targetUrl = targetBaseUrl + forwardedPath + (queryString != null && !queryString.isBlank() ? "?" + queryString : "");

        log.debug("Proxying {} {} -> {}", request.getMethod(), requestUri, targetUrl);

        try {
            HttpRequest.BodyPublisher bodyPublisher;
            String method = request.getMethod().toUpperCase();
            if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
                bodyPublisher = HttpRequest.BodyPublishers.noBody();
            } else {
                byte[] bodyBytes = request.getInputStream().readAllBytes();
                bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(bodyBytes);
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .method(method, bodyPublisher)
                    .timeout(Duration.ofSeconds(60));

            // Copy request headers
            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    if (!HOP_BY_HOP_HEADERS.contains(headerName.toLowerCase())) {
                        Enumeration<String> values = request.getHeaders(headerName);
                        while (values.hasMoreElements()) {
                            String headerValue = values.nextElement();
                            try {
                                requestBuilder.header(headerName, headerValue);
                            } catch (IllegalArgumentException e) {
                                // restricted or invalid header for java.net.http, ignore
                            }
                        }
                    }
                }
            }

            HttpResponse<InputStream> upstreamResponse = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            // Set response status
            response.setStatus(upstreamResponse.statusCode());

            // Copy response headers (excluding hop-by-hop and duplicate CORS headers)
            upstreamResponse.headers().map().forEach((key, values) -> {
                String lowerKey = key.toLowerCase();
                if (!HOP_BY_HOP_HEADERS.contains(lowerKey) && !CORS_HEADERS.contains(lowerKey)) {
                    for (String value : values) {
                        response.addHeader(key, value);
                    }
                }
            });

            // Stream response body
            try (InputStream in = upstreamResponse.body(); OutputStream out = response.getOutputStream()) {
                in.transferTo(out);
                out.flush();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Proxy request interrupted: {}", e.getMessage());
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Request interrupted");
        } catch (Exception e) {
            String detail = e.getMessage();
            if (detail == null || detail.isBlank()) {
                detail = e.getCause() != null && e.getCause().getMessage() != null
                        ? e.getCause().getMessage()
                        : e.getClass().getSimpleName();
            }
            log.error("Failed to proxy request to {}: {}", targetUrl, detail);
            response.setStatus(HttpStatus.BAD_GATEWAY.value());
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(String.format("{\"error\": \"Bad Gateway\", \"message\": \"Failed to reach upstream service at %s (%s)\"}", targetBaseUrl, detail));
        }
    }

    private boolean isAuthServicePath(String path) {
        return path.startsWith("/user/") || path.equals("/user")
                || path.startsWith("/auth/") || path.equals("/auth")
                || path.startsWith("/helpdesk/auth/") || path.equals("/helpdesk/auth");
    }
}
