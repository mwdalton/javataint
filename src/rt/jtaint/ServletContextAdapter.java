/* Copyright 2009 Michael Dalton */
package jtaint;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

final class ServletContextAdapter extends GenericContextAdapter 
                                  implements Opcodes
{

    public  static final Klass SERVLET;
    public  static final Klass SERVLETREQUEST;
    public  static final Klass SERVLETRESPONSE;
    public  static final Klass HTTPSERVLET;
    public  static final Klass HTTPSERVLETREQUEST;
    public  static final Klass COOKIE;
    public  static final Klass HTTPUTILS;
    private static final Map   methods; /* Map Method -> Klass */

    static {
        SERVLET            = new Klass("javax.servlet.Servlet");
        SERVLETREQUEST     = new Klass("javax.servlet.ServletRequest");
        SERVLETRESPONSE    = new Klass("javax.servlet.ServletResponse");
        HTTPSERVLET        = new Klass("javax.servlet.http.HttpServlet");
        HTTPSERVLETREQUEST = new Klass("javax.servlet.http.HttpServletRequest");
        COOKIE             = new Klass("javax.servlet.http.Cookie");

        /* We only instrument static methods in HttpUtils, thus all
         * instrument methods _must_ have the classname HttpUtils
         */ 
        HTTPUTILS          = new Klass("javax.servlet.http.HttpUtils", true);

        /* Initialize method map */
        HashMap h = new HashMap();

        /* javax.servlet.Servlet */
        h.put(new MethodDecl(ACC_PUBLIC, "service",
                    "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)V"),
                SERVLET);

        /* javax.servlet.http.HttpServlet */
        h.put(new MethodDecl(ACC_PUBLIC, "service",
                    "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"),
                HTTPSERVLET);

        h.put(new MethodDecl(ACC_PROTECTED, "service",
                    "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"),
                HTTPSERVLET);

        /* javax.servlet.ServletRequest */
        h.put(new MethodDecl(ACC_PUBLIC, "getCharacterEncoding", 
                    "()Ljava/lang/String;"), SERVLETREQUEST);
        h.put(new MethodDecl(ACC_PUBLIC, "getContentType", 
                    "()Ljava/lang/String;"), SERVLETREQUEST);
        h.put(new MethodDecl(ACC_PUBLIC, "getParameter", 
                    "(Ljava/lang/String;)Ljava/lang/String;"), SERVLETREQUEST);
        h.put(new MethodDecl(ACC_PUBLIC, "getParameterMap", 
                    "()Ljava/util/Map;"), SERVLETREQUEST);
        h.put(new MethodDecl(ACC_PUBLIC, "getParameterNames", 
                    "()Ljava/util/Enumeration;"), SERVLETREQUEST);
        h.put(new MethodDecl(ACC_PUBLIC, "getParameterValues", 
                    "(Ljava/lang/String;)[Ljava/lang/String;"), SERVLETREQUEST);
        h.put(new MethodDecl(ACC_PUBLIC, "getRemoteAddr", 
                    "()Ljava/lang/String;"), SERVLETREQUEST);
        h.put(new MethodDecl(ACC_PUBLIC, "getRemoteHost", 
                    "()Ljava/lang/String;"), SERVLETREQUEST);

        /* javax.servlet.ServletResponse */
        h.put(new MethodDecl(ACC_PUBLIC, "getWriter", 
                             "()Ljava/io/PrintWriter;"), SERVLETRESPONSE);
        h.put(new MethodDecl(ACC_PUBLIC, "getOutputStream", 
                             "()Ljavax/servlet/ServletOutputStream;"), 
                             SERVLETRESPONSE);

        /* javax.servlet.http.HttpServletRequest */
        h.put(new MethodDecl(ACC_PUBLIC, "getContextPath",
                    "()Ljava/lang/String;"), HTTPSERVLETREQUEST);
        h.put(new MethodDecl(ACC_PUBLIC, "getHeader",
                    "(Ljava/lang/String;)Ljava/lang/String;"),
                HTTPSERVLETREQUEST);
        h.put(new MethodDecl(ACC_PUBLIC, "getHeaderNames",
                    "()Ljava/util/Enumeration;"), HTTPSERVLETREQUEST);
        h.put(new MethodDecl(ACC_PUBLIC, "getHeaders",
                    "(Ljava/lang/String;)Ljava/util/Enumeration;"),
                HTTPSERVLETREQUEST);

        h.put(new MethodDecl(ACC_PUBLIC, "getPathInfo",
                    "()Ljava/lang/String;"), HTTPSERVLETREQUEST);
        h.put(new MethodDecl(ACC_PUBLIC, "getPathTranslated",
                    "()Ljava/lang/String;"), HTTPSERVLETREQUEST);
        h.put(new MethodDecl(ACC_PUBLIC, "getQueryString",
                    "()Ljava/lang/String;"), HTTPSERVLETREQUEST);
        h.put(new MethodDecl(ACC_PUBLIC, "getRemoteUser",
                    "()Ljava/lang/String;"), HTTPSERVLETREQUEST);
        h.put(new MethodDecl(ACC_PUBLIC, "getRequestedSessionId",
                    "()Ljava/lang/String;"), HTTPSERVLETREQUEST);
        h.put(new MethodDecl(ACC_PUBLIC, "getRequestURI",
                    "()Ljava/lang/String;"), HTTPSERVLETREQUEST);
        h.put(new MethodDecl(ACC_PUBLIC, "getRequestURL",
                    "()Ljava/lang/StringBuffer;"), HTTPSERVLETREQUEST);
        h.put(new MethodDecl(ACC_PUBLIC, "getServletPath",
                    "()Ljava/lang/String;"), HTTPSERVLETREQUEST);

        /* javax.servlet.http.Cookie */
        h.put(new MethodDecl(ACC_PUBLIC, "getComment",
                    "()Ljava/lang/String;"), COOKIE);
        h.put(new MethodDecl(ACC_PUBLIC, "getDomain",
                    "()Ljava/lang/String;"), COOKIE);
        h.put(new MethodDecl(ACC_PUBLIC, "getName",
                    "()Ljava/lang/String;"), COOKIE);
        h.put(new MethodDecl(ACC_PUBLIC, "getPath",
                    "()Ljava/lang/String;"), COOKIE);
        h.put(new MethodDecl(ACC_PUBLIC, "getValue",
                    "()Ljava/lang/String;"), COOKIE);

        /* javax.servlet.http.HttpUtils */
        h.put(new MethodDecl(ACC_PUBLIC + ACC_STATIC, "getRequestURL",
                    "(Ljavax/servlet/http/HttpServletRequest;)Ljava/lang/StringBuffer;"), 
                HTTPUTILS);

        h.put(new MethodDecl(ACC_PUBLIC + ACC_STATIC, "parsePostData",
                "(ILjavax/servlet/ServletInputStream;)Ljava/util/Hashtable;"), 
                HTTPUTILS);

        h.put(new MethodDecl(ACC_PUBLIC + ACC_STATIC, "parseQueryString",
                    "(Ljava/lang/String;)Ljava/util/Hashtable;"), HTTPUTILS);
        methods = h;
    }

    public ServletContextAdapter(ClassVisitor cv) {
        super(cv, methods);
    }
}

