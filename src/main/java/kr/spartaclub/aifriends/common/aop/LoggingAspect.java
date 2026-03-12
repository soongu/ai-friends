package kr.spartaclub.aifriends.common.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * REST API 요청/응답 로깅 Aspect.
 * - Trace ID로 요청 단위 추적 (MDC + 로그 메시지에 명시)
 * - key=value 구조로 출력해 로그 수집기(ELK, Datadog 등)에서 파싱·검색 용이
 * - 비밀번호·토큰 등 민감 정보는 로그에 *** 로 마스킹
 * - 예외 발생 시 한 곳에서 traceId 와 함께 error 로그
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /** 로그에 남길 문자열 최대 길이. */
    private static final int MAX_STRING_LENGTH = 500;
    private static final int MAX_SINGLE_VALUE_LENGTH = 100;

    /** 요청 바디 문자열 안에서 민감 필드 값 마스킹용 (password=xxx → password=***) */
    private static final Pattern SENSITIVE_FIELD_PATTERN = Pattern.compile(
            "(?i)(password|token|apiKey|secret|authorization|credential)=[^,\\s}\\]]+"
    );

    /** 파라미터 이름이 이 중 하나를 포함하면 값 마스킹 (대소문자 무시) */
    private static final List<String> SENSITIVE_PARAM_NAMES = List.of(
            "password", "token", "apikey", "secret", "authorization", "credential", "key"
    );

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logApi(ProceedingJoinPoint joinPoint) throws Throwable {

        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);

        try {
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            Optional<Object> requestBodyOpt = extractRequestBody(signature, args);
            List<String> otherParamNames = new ArrayList<>();
            List<Object> otherArgs = new ArrayList<>();

            Parameter[] parameters = signature.getMethod().getParameters();
            for (int i = 0; i < parameters.length && i < args.length; i++) {
                if (parameters[i].isAnnotationPresent(RequestBody.class)) {
                    continue;
                }
                Object arg = args[i];
                if (!isLoggable(arg)) continue;
                otherParamNames.add(parameterNames != null && i < parameterNames.length ? parameterNames[i] : "arg" + i);
                otherArgs.add(arg);
            }

            String otherParamsString = buildParamsString(otherParamNames.toArray(new String[0]), otherArgs.toArray());

            // RequestContextHolder 는 요청 스레드에서만 유효. 비동기/스케줄러 등에서는 null 일 수 있음
            String httpMethod = "";
            String requestUri = "";
            var attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttributes) {
                HttpServletRequest request = servletAttributes.getRequest();
                httpMethod = request.getMethod();
                requestUri = request.getRequestURI();
            }

            String requestBodyLog = requestBodyOpt
                    .map(body -> maskSensitiveInString(toShortString(body)))
                    .orElse("");

            if (requestBodyOpt.isPresent()) {
                log.info("traceId={} phase=REQUEST method={} uri={} className={} methodName={} requestBody={} params={}",
                        traceId, httpMethod, requestUri, className, methodName, requestBodyLog, otherParamsString);
            } else {
                log.info("traceId={} phase=REQUEST method={} uri={} className={} methodName={} params={}",
                        traceId, httpMethod, requestUri, className, methodName, otherParamsString);
            }

            long startTime = System.currentTimeMillis();

            Object result;
            try {
                result = joinPoint.proceed();
            } catch (Throwable t) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("traceId={} phase=ERROR method={} uri={} className={} methodName={} durationMs={} error={}",
                        traceId, httpMethod, requestUri, className, methodName, duration, t.getMessage(), t);
                throw t;
            }

            long duration = System.currentTimeMillis() - startTime;

            Object responseBody = result;
            if (result instanceof ResponseEntity<?> responseEntity) {
                responseBody = responseEntity.getBody();
            }

            String responseLog = maskSensitiveInString(toShortString(responseBody));
            log.info("traceId={} phase=RESPONSE method={} uri={} className={} methodName={} durationMs={} response={}",
                    traceId, httpMethod, requestUri, className, methodName, duration, responseLog);

            return result;
        } finally {
            MDC.remove("traceId");
        }
    }

    private boolean isLoggable(Object arg) {
        if (arg == null) {
            return true;
        }
        Class<?> clazz = arg.getClass();
        if (isWrapperOrPrimitiveType(clazz) || clazz == String.class) {
            return true;
        }
        if (clazz.getPackageName().startsWith("kr.spartaclub.aifriends")) {
            return true;
        }
        if (arg instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item != null) {
                    Class<?> itemClass = item.getClass();
                    if (!isWrapperOrPrimitiveType(itemClass) && itemClass != String.class &&
                            !itemClass.getPackageName().startsWith("kr.spartaclub.aifriends")) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    private Optional<Object> extractRequestBody(MethodSignature signature, Object[] args) {
        if (args == null || args.length == 0) {
            return Optional.empty();
        }
        Parameter[] parameters = signature.getMethod().getParameters();
        for (int i = 0; i < parameters.length && i < args.length; i++) {
            if (parameters[i].isAnnotationPresent(RequestBody.class)) {
                return Optional.ofNullable(args[i]);
            }
        }
        return Optional.empty();
    }

    private boolean isWrapperOrPrimitiveType(Class<?> clazz) {
        return clazz == Boolean.class || clazz == Character.class ||
                clazz == Byte.class || clazz == Short.class ||
                clazz == Integer.class || clazz == Long.class ||
                clazz == Float.class || clazz == Double.class;
    }

    /** 파라미터 이름이 민감 필드면 값은 *** 로 마스킹 */
    private String buildParamsString(String[] names, Object[] args) {
        if (names == null || args == null || names.length != args.length) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            Object value = i < args.length ? args[i] : null;
            String valueStr = isSensitiveParamName(name) ? "***" : toShortString(value);
            parts.add(name + "=" + valueStr);
        }
        String joined = String.join(", ", parts);
        return joined.length() > MAX_STRING_LENGTH ? joined.substring(0, MAX_STRING_LENGTH) + "..." : joined;
    }

    private boolean isSensitiveParamName(String paramName) {
        if (paramName == null) return false;
        String lower = paramName.toLowerCase();
        return SENSITIVE_PARAM_NAMES.stream().anyMatch(lower::contains);
    }

    /** 요청/응답 문자열 안의 password=xxx, token=xxx 등을 *** 로 치환 */
    private String maskSensitiveInString(String s) {
        if (s == null || s.isEmpty()) return s;
        return SENSITIVE_FIELD_PATTERN.matcher(s).replaceAll("$1=***");
    }

    private String toShortString(Object obj) {
        if (obj == null) {
            return "null";
        }
        String s = obj.toString();
        return s.length() > MAX_SINGLE_VALUE_LENGTH
                ? s.substring(0, MAX_SINGLE_VALUE_LENGTH) + "..."
                : s;
    }
}
