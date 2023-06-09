package com.ting.ting.configuration.filter;

import com.ting.ting.dto.UserDto;
import com.ting.ting.dto.response.Response;
import com.ting.ting.exception.ErrorCode;
import com.ting.ting.exception.ServiceType;
import com.ting.ting.exception.TingApplicationException;
import com.ting.ting.service.UserService;
import com.ting.ting.util.JwtTokenUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractTokenFromHeader(request);
            jwtTokenUtil.validateToken(token);
            authenticateUser(token, request);
        } catch (TingApplicationException e) {
            sendErrorResponse(response, ErrorCode.INVALID_ACCESS_TOKEN, e.getMessage());
        } catch (RuntimeException e) {
            sendErrorResponse(response, ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer")) {
            throw new TingApplicationException(ErrorCode.INVALID_ACCESS_TOKEN, ServiceType.AUTHENTICATION, "Header is null or invalid");
        }
        return header.split(" ")[1].trim();
    }

    private void authenticateUser(String token, HttpServletRequest request) {
        Long userId = jwtTokenUtil.getIdByToken(token);
//        UserDto user = userService.getUserById(userId); // 레디스와 같은 캐시를 통해 조회해야 함. 지금은 레디스가 없으므로 db 접근을 줄이기 위해 token 안 userId에 대한 검증은 안한다. TODO: redis 도입 후 수정
        UserDto user = new UserDto(userId, null, null, null, null, null, null, null, null, null);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities()
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        TingApplicationException e = new TingApplicationException(errorCode, ServiceType.AUTHENTICATION, message);
        Response<?> body = new Response<>(e);

        log.error(e.getMessageForServer());
        response.setStatus(e.getErrorCode().getHttpStatus());
        response.getWriter().write(body.toStream());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        List<String> excludedUrls = List.of("/ting", "/kakao", "/api-docs", "/swagger");
        return excludedUrls.stream().anyMatch(request.getRequestURI()::contains);
    }
}
