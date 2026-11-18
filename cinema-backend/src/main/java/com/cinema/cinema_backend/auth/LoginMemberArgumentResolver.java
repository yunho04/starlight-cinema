package com.cinema.cinema_backend.auth;

import com.cinema.cinema_backend.exception.CinemaException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginMember.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object memberId = webRequest.getAttribute(AuthInterceptor.MEMBER_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (memberId == null) {
            // 인터셉터 경로 설정이 빠진 API에서 @LoginMember를 쓴 경우에도 인증 없이 통과되지 않게 한다.
            throw CinemaException.unauthorized("로그인이 필요합니다.");
        }
        return memberId;
    }
}
