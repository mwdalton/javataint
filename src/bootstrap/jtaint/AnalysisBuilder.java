/* Copyright 2009 Michael Dalton */
package jtaint;

import org.objectweb.asm.ClassVisitor;

public interface AnalysisBuilder
{
    public ClassVisitor build();
}
