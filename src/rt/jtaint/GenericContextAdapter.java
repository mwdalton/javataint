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

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class GenericContextAdapter extends ClassAdapter implements Opcodes
{
    private String className;
    private boolean skip;
    private final Map methods;
    private Map instrumentedMethods;

    public GenericContextAdapter(ClassVisitor cv, Map methods) {
        super(cv);
        this.methods = methods;
    }

    public void visit(int version, int access, String name, 
            String signature, String superName, 
            String[] interfaces) 
    {
        /* We cannot insert instrumentation into class that have no method
         * bodies. We also do not instrument any Enums 
         */
        skip = (access & (ACC_INTERFACE|ACC_ANNOTATION|ACC_ENUM)) != 0;
        className = name;
        cv.visit(version, access, name, signature, superName, interfaces);
    }

    public MethodVisitor visitMethod(final int access, final String name, 
            final String desc, String signature, 
            String[] exceptions) 
    {
        final MethodVisitor mv = cv.visitMethod(access, name, desc, signature,
                                                exceptions);
        if (skip) return mv;

        /* Subtle point: Two MethodDecls are not considered equal if one is
         * abstract and the other is not
         */
        MethodDecl md = new MethodDecl(access, name, desc);

        if (methods.containsKey(md)) {
            Klass k = (Klass) methods.get(md);
            if (k.isExact() && !className.equals(k.internalName()))
                return mv;
            if (instrumentedMethods == null)
                instrumentedMethods = new HashMap();
            instrumentedMethods.put(md, k);
        }
        return mv;
    }

    public Map instrumentedMethods() { return instrumentedMethods; }

    public boolean instrumented() { return instrumentedMethods != null; }
}
