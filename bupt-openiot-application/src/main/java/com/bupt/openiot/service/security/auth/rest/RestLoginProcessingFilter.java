package com.bupt.openiot.service.security.auth.rest;

import com.bupt.openiot.conf.OpenIoTServerConfig;
import com.bupt.openiot.dto.UserInfo;
import com.bupt.openiot.internalsdk.util.HttpClientUtil;
import com.bupt.openiot.model.LoginToken;
import com.bupt.openiot.service.security.exception.AuthMethodNotSupportedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import net.sf.json.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RestLoginProcessingFilter extends AbstractAuthenticationProcessingFilter {
		
	private static Logger logger = LoggerFactory.getLogger(RestLoginProcessingFilter.class);

    private final AuthenticationSuccessHandler successHandler;
    private final AuthenticationFailureHandler failureHandler;

    private final ObjectMapper objectMapper;
    private final OpenIoTServerConfig openIoTServerConfig;

    public RestLoginProcessingFilter(String defaultProcessUrl, AuthenticationSuccessHandler successHandler,
                                     AuthenticationFailureHandler failureHandler, ObjectMapper mapper,
                                     OpenIoTServerConfig openIoTServerConfig) {
        super(defaultProcessUrl);
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.objectMapper = mapper;
        this.openIoTServerConfig = openIoTServerConfig;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {
        if (!HttpMethod.POST.name().equals(request.getMethod())) {
            if(logger.isDebugEnabled()) {
                logger.debug("Authentication method not supported. Request method: " + request.getMethod());
            }
            throw new AuthMethodNotSupportedException("Authentication method not supported");
        }

        LoginRequest loginRequest;
        try {
            loginRequest = objectMapper.readValue(request.getReader(), LoginRequest.class);
        } catch (Exception e) {
            throw new AuthenticationServiceException("Invalid login request payload");
        }

        if (StringUtils.isBlank(loginRequest.getUsername()) || StringUtils.isBlank(loginRequest.getPassword())) {
            throw new AuthenticationServiceException("Username or Password not provided");
        }

        System.out.println(loginRequest.toString());
        loginToOpenIoT(loginRequest, request);
        
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());
        Authentication authentication = this.getAuthenticationManager().authenticate(token);
        return authentication;
    }

    @SuppressWarnings("unchecked")
	private void loginToOpenIoT(LoginRequest loginRequest, HttpServletRequest request) {
        if (loginRequest == null || StringUtils.isEmpty(loginRequest.getUsername()) ||
                StringUtils.isEmpty(loginRequest.getPassword())) {
            return ;
        }
        Map<String, String> maps = new HashMap<String, String>();
        maps.put("username", loginRequest.getUsername());
        maps.put("password", loginRequest.getPassword());
        String responseContent = HttpClientUtil.getInstance()
                .sendHttpPost("http://" + openIoTServerConfig.getServer() + "/api/auth/login", maps);
        Gson gson = new Gson();
        LoginToken loginToken = gson.fromJson(responseContent, LoginToken.class);
        HttpSession session = request.getSession();
        session.setAttribute("token", loginToken.getToken());
        session.setAttribute("refreshToken", loginToken.getRefreshToken());
        responseContent = HttpClientUtil.getInstance().sendHttpGet("http://" + openIoTServerConfig.getServer() + "/api/auth/user", loginToken.getToken());
        JSONObject json = JSONObject.fromObject(responseContent);
        UserInfo userInfo = new UserInfo();
        userInfo.setTenantId((String) ((Map<String,Object>)json.get("tenantId")).get("id"));;
        userInfo.setCustomId((String) ((Map<String,Object>)json.get("customerId")).get("id"));
        userInfo.setAuthority((String) json.get("authority"));
        session.setAttribute("userInfo", userInfo);
       
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {
        successHandler.onAuthenticationSuccess(request, response, authResult);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        failureHandler.onAuthenticationFailure(request, response, failed);
    }
    
}
