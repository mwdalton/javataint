/* Copyright 2009 Michael Dalton */
package jtaint;

/* For concurrency reasons, all fields in our Configuration must be final.
 * However, it is very inconvenient to build a config file parser with
 * final fields for parsed output, as the parsing process can be dynamic and 
 * fields may be * set once, twice, etc (or not at all - and then must be set to
 * a default value). For this reason, we perform parsing in the ConfigParser
 * class, and then store all parsing results in the final fields of the
 * Configuration class. By using only final fields, we ensure that all threads
 * will see up-to-date values for our fields without requiring additional
 * locking or synchronization
 */

import java.util.Map;

public final class Configuration
{
    /* We keep track of policy states using booleans rather than policy 
     * objects so that all configuration checks can be constant-folded by the 
     * JVM
     */
    public static final boolean     execPolicyEnabled;
    public static final boolean     execPolicyLogAttack;

    public static final boolean     filePolicyEnabled;
    public static final boolean     filePolicyLogAttack;

    public static final boolean     sqlPolicyEnabled;
    public static final boolean     sqlPolicyLogAttack;
    public static final boolean     sqlPolicyLogVuln;

    public static final boolean     xssPolicyEnabled;
    public static final boolean     xssPolicyLogAttack;
    public static final boolean     xssPolicyLogVuln;

    public static final Map xssFilters;

    public static final Map execWhitelist;
    public static final Map fileWhitelist;
    public static final Map sqlWhitelist;
    public static final Map xssWhitelist;

    static {
        ConfigParser cp = new ConfigParser();
        cp.parse();

        Policy p = cp.execPolicy();
        execPolicyEnabled = p.getEnabled();
        execPolicyLogAttack = p.getLogAttack();

        p = cp.filePolicy();
        filePolicyEnabled = p.getEnabled();
        filePolicyLogAttack = p.getLogAttack();

        p = cp.sqlPolicy();
        sqlPolicyEnabled = p.getEnabled();
        sqlPolicyLogAttack = p.getLogAttack();
        sqlPolicyLogVuln = p.getLogVuln();

        p = cp.xssPolicy();
        xssPolicyEnabled = p.getEnabled();
        xssPolicyLogAttack = p.getLogAttack();
        xssPolicyLogVuln = p.getLogVuln();

        xssFilters  = cp.xssFilters();

        execWhitelist = cp.execWhitelist();
        fileWhitelist = cp.fileWhitelist();
        sqlWhitelist =  cp.sqlWhitelist();
        xssWhitelist =  cp.xssWhitelist();
    }
}
