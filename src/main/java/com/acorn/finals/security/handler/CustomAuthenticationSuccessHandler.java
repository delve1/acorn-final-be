package com.acorn.finals.security.handler;

import com.acorn.finals.config.properties.TokenPropertiesConfig;
import com.acorn.finals.mapper.MemberMapper;
import com.acorn.finals.service.TokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final TokenService tokenService;
    private final MemberMapper memberMapper;
    private final TokenPropertiesConfig tokenPropertiesConfig;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2User user = oauthToken.getPrincipal();

            Map<String, Object> attributes = user.getAttributes();
            String email = (String) attributes.get("email");

            // 로그인 성공 후 동작 예시: 이메일이나 사용자 정보를 기반으로 추가 작업 수행
            System.out.println("User logged in: " + email);

            String token = tokenService.generateToken(email);
            String refreshtoken = tokenService.createRefreshToken(email);

            System.out.println(token);

            Cookie cookie = new Cookie("Authorization", "Bearer+" + token);
            cookie.setMaxAge(tokenPropertiesConfig.getAccessToken().getExpiration());
            cookie.setHttpOnly(true); // JavaScript에서 쿠키에 접근할 수 없도록 설정
            cookie.setPath("/"); // 모든 경로에서 쿠키를 사용할수 있도록 설정
            Cookie cookie2 = new Cookie("Refresh", refreshtoken);
            cookie2.setMaxAge(tokenPropertiesConfig.getRefreshToken().getExpiration());
            cookie2.setHttpOnly(true); // JavaScript에서 쿠키에 접근할 수 없도록 설정
            cookie2.setPath("/"); // 모든 경로에서 쿠키를 사용할수 있도록 설정
            // 응답 헤더에 쿠키 추가
            response.addCookie(cookie);
            response.addCookie(cookie2);
            // set-header http only refresh token
            System.out.println("여기오냐?");
            //Oauth 로그인 후 Security 에서 부여되는 userName 을 다시 이메일로 지정하기 위한 로직
            UserDetails ud = new User(email, "", List.of());
            Authentication auth = new UsernamePasswordAuthenticationToken(ud, ud.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            // 가입정보가 있으면 인덱스 페이지
            if (memberMapper.findOneByEmail(email) != null) {
                response.sendRedirect("/api");
                return;
            }
            // 로그인 성공 후 리다이렉트 등 다른 동작을 수행할 수 있습니다.
            //Db 에 없을시 폼 데이터 페이지로 이동
            response.sendRedirect("https://dotori.site");

        } else {
            // 다른 인증 방식인 경우 다른 처리를 수행할 수 있습니다.
            response.sendRedirect("/api");
        }
    }
}