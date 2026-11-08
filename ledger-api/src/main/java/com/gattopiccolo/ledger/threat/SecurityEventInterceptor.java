package com.gattopiccolo.ledger.threat;

import com.gattopiccolo.ledger.domain.Account;
import com.gattopiccolo.ledger.repository.AccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.time.Instant;
import java.util.Map;

/**
 * The "sensor": observes every account API call after it has completed and
 * turns it into a {@link SecurityEvent}. It never changes the response and
 * never fails the request; it only reports.
 *
 * <p>Note that it detects, it does not prevent: the controllers do not check
 * that the caller owns the account (see docs/threat-observability.md).
 */
@Component
public class SecurityEventInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventInterceptor.class);
    private static final String ANONYMOUS = "anonymous";

    private final DetectionEngine engine;
    private final AccountRepository accounts;
    private final SecurityProperties props;

    public SecurityEventInterceptor(DetectionEngine engine, AccountRepository accounts, SecurityProperties props) {
        this.engine = engine;
        this.accounts = accounts;
        this.props = props;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            engine.onEvent(toEvent(request, response));
        } catch (RuntimeException failure) {
            log.warn("Security sensor failed for {} {}", request.getMethod(), request.getRequestURI(), failure);
        }
    }

    SecurityEvent toEvent(HttpServletRequest request, HttpServletResponse response) {
        String actor = header(request, "X-User-Id");
        if (actor == null) {
            actor = ANONYMOUS;
        }
        Long accountId = pathAccountId(request);
        Long owner = accountId == null ? null
                : accounts.findById(accountId).map(Account::getUserId).orElse(null);

        int status = response.getStatus();
        String method = request.getMethod();
        String path = request.getRequestURI();

        EventType type;
        if (owner != null && !String.valueOf(owner).equals(actor)) {
            type = EventType.FOREIGN_ACCOUNT_ACCESS;
        } else if ("POST".equals(method) && path.endsWith("/debit")) {
            type = EventType.DEBIT;
        } else if (status >= 400 && status < 500) {
            type = EventType.CLIENT_ERROR;
        } else {
            type = EventType.REQUEST;
        }
        return new SecurityEvent(Instant.now(), actor, sourceIp(request), method, path,
                status, accountId, owner, type);
    }

    private String sourceIp(HttpServletRequest request) {
        if (props.trustForwardedFor()) {
            String forwarded = header(request, "X-Forwarded-For");
            if (forwarded != null) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    @SuppressWarnings("unchecked")
    private static Long pathAccountId(HttpServletRequest request) {
        Object vars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(vars instanceof Map<?, ?> map)) {
            return null;
        }
        Object id = ((Map<String, String>) map).get("id");
        if (id == null) {
            return null;
        }
        try {
            return Long.valueOf(id.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
