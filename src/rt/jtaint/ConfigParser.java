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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/* Parse JavaTaint configuration file */
public final class ConfigParser extends DefaultHandler
{
    private String  configPath     = "";

    private Policy  execPolicy    = new Policy();
    private Policy  filePolicy    = new Policy();
    private Policy  sqlPolicy     = new Policy();
    private Policy  xssPolicy     = new Policy();


    private Map xssFilters = new HashMap();

    private Map execWhitelist = new HashMap();
    private Map fileWhitelist = new HashMap();
    private Map sqlWhitelist = new HashMap();
    private Map xssWhitelist = new HashMap();
    
    private Map elemMap = new HashMap();

    public ConfigParser() {
        elemMap.put("jt-config", null);
        elemMap.put("policy", new PolicyHandler());
        elemMap.put("filter", new FilterHandler());
        elemMap.put("whitelist", new WhitelistHandler());
    }

    public void parse() {
        try {
            /* Parse the config file to initialize all configuration params */
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            InputStream is = cl.getResourceAsStream("jtaint/InstallPath");
            if (is == null)
                throw new RuntimeException("JavaTaint install corrupted");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            configPath = br.readLine();
            br.close();

            configPath = configPath + File.separator + "jt-config.xml";
            Log.debug("Using Config file path " + configPath);

            SAXParserFactory fact = SAXParserFactory.newInstance();
            fact.setValidating(true);
            fact.newSAXParser().parse(new File(configPath), this); 
            Log.debug("JavaTaint @version@ initialized");
        } catch (SAXParseException e) {
            /* already reported by ConfigErrorHandler */
        } catch (Throwable th) {
            Log.warn(th);
        }
    }

    /* Override ContentHandler methods for parsing */
    public void startElement(String ns, String lName, String qName, 
                             Attributes attr) throws SAXException 
    {
        /* Namespace-aware XML parsers put name in qName, others use lName */
        String name = lName == null || lName.length() == 0 ? qName : lName;

        if (!elemMap.containsKey(name)) {
            Log.warn("Unknown XML element " + name + " found in "
                            + "configuration file (skipping)\n");
            return;
        }

        ElemHandler eh = (ElemHandler) elemMap.get(name);
        if (eh != null)
            eh.start(name, attr);
    }

    public void endElement(String ns, String lName, String qName)
                throws SAXException 
    {
        /* Namespace-aware XML parsers put name in qName, others use lName */
        String name = lName == null || lName.length() == 0 ? qName : lName;

        ElemHandler eh = (ElemHandler) elemMap.get(name);
        if (eh != null) 
            eh.end(name);
    }

    public void endDocument() {
        if (xssFilters.size() == 0 && xssPolicy.getLogVuln()) {
            Log.error("Parse configuration error: Logging of XSS "
                      + "vulnerabilities enabled but no XSS filters "
                      + "specified.\nPlease specify your XSS filters.\n");
            xssPolicy().setLogVuln(false);
            return;
        }
    }

    /* Override ErrorHandler methods for logging errors */
    public void error(SAXParseException e) {
        log("Continuable parsing error ", e);
    }

    public void fatalError(SAXParseException e) throws SAXParseException {
        log("Fatal parsing error ", e);
        throw e;
    }

    public void warning(SAXParseException e) {
        log("Parsing warning ", e);
    }

    private void log(String msg, SAXParseException e) {
        Log.warn("Parsing config file" + configPath + "\n" + "Line " + 
                        e.getLineNumber() + " Column " + e.getColumnNumber() + 
                        ": " + msg + "(" + e.getMessage() + ")");
    }

    private static void addMethod(Map m, String className, String methodName) {
        Set s = (Set) m.get(className);
        if (s == null)
            s = new HashSet();
        s.add(methodName);
        m.put(className, s);
    }

    private abstract class ElemHandler
    {
        void start(String name, Attributes attrs) { } 
        void end(String name) { }
    }

    private final class PolicyHandler extends ElemHandler
    {
        public void start(String name, Attributes attrs) {
            Policy p;

            String type = attrs.getValue("type");
            if (type == null) {
                Log.warn("Configuration file parser error: policy " 
                                  + "specified without type attribute " 
                                  + "(skipping)\n");
                return;
            }

            if ("exec".equals(type))
                p = execPolicy;
            else if ("file".equals(type))
                p = filePolicy;
            else if ("sql".equals(type))
                p = sqlPolicy;
            else if ("xss".equals(type))
                p = xssPolicy;
            else {
                Log.warn("Configuration file parser error: policy has " 
                                  + "unknown type " + type + "(skipping)\n");
                return;
            }

            String enabled   = attrs.getValue("enabled");
            String logAttack = attrs.getValue("log-attack");
            String logVuln   = attrs.getValue("log-vuln");

            if (enabled != null) 
                p.setEnabled(Boolean.valueOf(enabled).booleanValue());
            if (logAttack != null)
                p.setLogAttack(Boolean.valueOf(logAttack).booleanValue());
            if (logVuln != null)
                p.setLogVuln(Boolean.valueOf(logVuln).booleanValue());
        }

    }

    private final class FilterHandler extends ElemHandler
    {
        public void start(String name, Attributes attrs) {
            String className  = attrs.getValue("class");
            String methodName = attrs.getValue("method");
            String type       = attrs.getValue("type");

            if (type == null || className == null || methodName == null 
                    || !"xss".equals(type)) {
                Log.warn("Invalid filter (skipping)");
                return;
            } 
      
            /* Store class names in internal format */ 
            addMethod(xssFilters, className.replace('.', '/'), methodName);
        }
    }

    private final class WhitelistHandler extends ElemHandler
    {
        public void start(String name, Attributes attrs) {
            String className  = attrs.getValue("class");
            String methodName = attrs.getValue("method");
            String type       = attrs.getValue("type");
            Map m = null;

            if (type == null) type = "";
            if ("exec".equals(type))
                m = execWhitelist;
            else if ("file".equals(type))
                m = fileWhitelist;
            else if ("sql".equals(type))
                m = sqlWhitelist;
            else if ("xss".equals(type))
                m = xssWhitelist;

            if (m == null || className == null || methodName == null) {
                Log.warn("Invalid filter (skipping)");
                return;
            } 

            addMethod(m, className, methodName);
        }
    }

    /* Accessors for all configuration options */
    public Policy       execPolicy()    { return execPolicy; }
    public Policy       filePolicy()    { return filePolicy; }
    public Policy       sqlPolicy()     { return sqlPolicy;  }
    public Policy       xssPolicy()     { return xssPolicy;  }

    public Map          xssFilters()    { return xssFilters; }

    public Map          execWhitelist() { return execWhitelist; }
    public Map          fileWhitelist() { return fileWhitelist; }
    public Map          sqlWhitelist()  { return sqlWhitelist; }
    public Map          xssWhitelist()  { return xssWhitelist; }
}
