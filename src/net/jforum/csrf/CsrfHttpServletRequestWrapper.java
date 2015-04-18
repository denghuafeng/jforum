package net.jforum.csrf;

import javax.servlet.http.*;

/**
 * Pass method instead of requestUri to match unportected logic from csrf.properties
 * @author Jeanne Boyarsky
 * @version $Id: $
 */
public class CsrfHttpServletRequestWrapper extends HttpServletRequestWrapper {
    
    private String actionMethodName;

    public CsrfHttpServletRequestWrapper(HttpServletRequest request, String actionMethodName) {
        super(request);
        this.actionMethodName = actionMethodName;
    }
    @Override
    public String getRequestURI() {
        return actionMethodName;
    }
}

