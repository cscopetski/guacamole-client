/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.rest;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleUnauthorizedException;
import org.apache.guacamole.rest.auth.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that maps GuacamoleExceptions in a way that returns a
 * custom response to the user via JSON rather than allowing the default
 * web application error handling to take place.
 */
@Provider
@Singleton
public class GuacamoleExceptionMapper
        implements ExceptionMapper<GuacamoleException> {
    
    /**
     * The logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(GuacamoleExceptionMapper.class);
    
    /**
     * The request associated with this instance of this mapper.
     */
    @Context private HttpServletRequest request;
    
    /**
     * The authentication service associated with the currently active session.
     */
    @Inject
    private AuthenticationService authenticationService;
    
    /**
     * Returns the authentication token that is in use in the current session,
     * if present, or null if otherwise.
     *
     * @return
     *     The authentication token for the current session, or null if no
     *     token is present.
     */
    private String getAuthenticationToken() {

        @SuppressWarnings("unchecked")
        Map<String, String[]> parameters = request.getParameterMap();

        for (String paramName : parameters.keySet()) {
            if (paramName.equals("token")) {
                String tokenParams[] = parameters.get(paramName);
                if (tokenParams[0] != null && !tokenParams[0].isEmpty())
                    return tokenParams[0];
            }
        }
        
        return null;

    }
    
    @Override
    public Response toResponse(GuacamoleException e) {
        
        if (e instanceof GuacamoleUnauthorizedException) {
            String token = getAuthenticationToken();
            
            if (authenticationService.destroyGuacamoleSession(token))
                logger.debug("Implicitly invalidated session for token \"{}\"", token);
        }
        
        return Response
                .status(e.getHttpStatusCode())
                .entity(new APIError(e))
                .type(MediaType.APPLICATION_JSON)
                .build();
      
    }
    
}