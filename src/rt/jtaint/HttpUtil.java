/*
 *  Copyright 2009-2012 Michael Dalton
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package jtaint;

import java.lang.reflect.Method;

import java.util.Map;

public final class HttpUtil
{

    public static void preService(Map requestParams, String remoteHost,
                                  String remoteAddr)
    {
       Thread t = Thread.currentThread();

       t.@internal@setRequestParams(requestParams);
       t.@internal@setRemoteHost(remoteHost);
       t.@internal@setRemoteAddr(remoteAddr);
    }

    public static void postService()
    {
       Thread t = Thread.currentThread();

       t.@internal@setRequestParams(null);
       t.@internal@setRemoteHost(null);
       t.@internal@setRemoteAddr(null);
    }

    public static String getPathTranslated(String res, Object o) 
    {
        try {
            Class c = o.getClass();
            Method pathInfo = c.getMethod("getPathInfo", (Class[]) null);
            String urlPath = (String) pathInfo.invoke(o, (Object[]) null);

            Method realPath = c.getMethod("getRealPath", 
                                          new Class[] { urlPath.getClass() });
            String s = (String) realPath.invoke(o, new Object[] { urlPath });
            return s;
        } catch (NoSuchMethodException e) {
            return res;
        } catch(Throwable th) {
            Log.error(th);
            return res;
        }
    }

    public static HtmlValidator getHtmlValidator(Object o) 
    {
        try {
            Class c = o.getClass();
            ClassLoader cl = c.getClassLoader();
            if (cl == null) 
                return null;

            Class servletResponse = cl.@internal@findLoadedClass("javax.servlet.ServletResponse");
            if (servletResponse == null || !servletResponse.isAssignableFrom(c))
                return null;

            Method getEncoding  = c.getMethod("getCharacterEncoding",  
                                              (Class[]) null);
            String charset = (String) getEncoding.invoke(o, (Object[]) null);

            String contentType = null;

            /* This method is present only in Servlet 2.4+ */
            try {
                Method getContentType = c.getMethod("getContentType", 
                                                    (Class[]) null);
                contentType = (String) getContentType.invoke(o, (Object[])null);
            } catch (NoSuchMethodException e) { }

            return new HtmlValidator(charset, contentType);
        } catch(Throwable th) {
            Log.error(th);
            return null;
        }
    }
}
