package io.bdrc.iiif.presentation;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.bdrc.auth.Access;
import io.bdrc.auth.TokenValidation;
import io.bdrc.auth.UserProfile;
import io.bdrc.auth.model.Endpoint;

@Component
@Order(1)
public class IIIFPresAuthFilter implements Filter {

	public final static Logger log = LoggerFactory.getLogger(IIIFPresAuthFilter.class.getName());

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		final String token = getToken(((HttpServletRequest) request).getHeader("Authorization"));
		if (token != null) {
			// User is logged on
			// Getting his profile
			final TokenValidation validation = new TokenValidation(token);
			final UserProfile prof = validation.getUser();
			request.setAttribute("access", new Access(prof, new Endpoint()));
		} else {
			request.setAttribute("access", new Access());
		}
		chain.doFilter(request, response);
	}

	public static String getToken(final String header) {
        if (header == null) {
            return null;
        }
		if (!header.startsWith("Bearer ")) {
			log.error("invalid Authorization header: {}", header);
			return null;
		}
		return header.substring(7);
	}

}