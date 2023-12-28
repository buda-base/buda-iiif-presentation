package io.bdrc.iiif.presentation;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class CommonHeadersFilter implements Filter {

    static final int ACCESS_CONTROL_MAX_AGE_IN_SECONDS = 24 * 60 * 60;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        ((HttpServletResponse) response).addHeader("Access-Control-Allow-Origin", "*");
        ((HttpServletResponse) response).addHeader("Access-Control-Allow-Credentials", "true");
        ((HttpServletResponse) response).addHeader("Access-Control-Allow-Methods", "PUT, GET, HEAD, OPTIONS");
        ((HttpServletResponse) response).addHeader("Access-Control-Allow-Headers",
                "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers, Authorization, Keep-Alive, User-Agent, If-Modified-Since, If-None-Match, Cache-Control, Accept-Encoding");
        ((HttpServletResponse) response).addHeader("Access-Control-Expose-Headers",
                "Cache-Control, ETag, Last-Modified, Content-Type, Cache-Control, Vary, Access-Control-Max-Age, Content-Encoding");
        ((HttpServletResponse) response).addHeader("Access-Control-Max-Age", Integer.toString(ACCESS_CONTROL_MAX_AGE_IN_SECONDS));
        chain.doFilter(request, response);
    }
}