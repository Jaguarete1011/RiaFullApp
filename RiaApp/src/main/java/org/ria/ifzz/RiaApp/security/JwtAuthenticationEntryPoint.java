package org.ria.ifzz.RiaApp.security;

import com.google.gson.Gson;
import org.ria.ifzz.RiaApp.exception.InvalidLoginResponse;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint
//        implements AuthenticationEntryPoint
{
//
//    @Override
//    public void commence(HttpServletRequest httpServletRequest,
//                         HttpServletResponse httpServletResponse,
//                         AuthenticationException e) throws IOException, ServletException {
//
//        InvalidLoginResponse loginResponse = new InvalidLoginResponse();
//        String JsonLoginResponse = new Gson().toJson(loginResponse);
//
//        httpServletResponse.setContentType("application/json");
//        httpServletResponse.setStatus(401);
//        httpServletResponse.getWriter().print(JsonLoginResponse);
//    }
}
