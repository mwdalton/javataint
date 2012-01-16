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

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.objectweb.asm.commons.SimpleAdviceAdapter;

import java.util.ArrayList;
import java.util.List;

/** Instrument java.lang.ProcessBuilder to prevent command injection attacks.
 * Forbid the pathname of the program to be tainted. Tainted arguments, however,
 * are allowed. Validation checks are inserted into the command methods and
 * the constructors.
 */

public class ProcessBuilderAdapter extends StubAdapter implements Opcodes 
{
    private static final List methodList;
    private String className;

    static {
        List l = new ArrayList();
        l.add(new MethodDecl(ACC_PUBLIC, "command", 
                "(Ljava/util/List;)Ljava/lang/ProcessBuilder;"));
        l.add(new MethodDecl(ACC_PUBLIC, "command",
                "([Ljava/lang/String;)Ljava/lang/ProcessBuilder;"));
        l.add(new MethodDecl(ACC_PUBLIC, "<init>", "(Ljava/util/List;)V"));
        l.add(new MethodDecl(ACC_PUBLIC, "<init>", "([Ljava/lang/String;)V"));
        methodList = l;

    }

    public ProcessBuilderAdapter(ClassVisitor cv) { 
        /* Constructors are not class members and should not have stubs */
        super(cv, methodList.subList(0,2)); 
    }

    public void visit(int version, int access, String name, 
                                String signature, String superName, 
                                String[] interfaces) 
    {
        className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    public MethodVisitor visitMethod(final int access, final String name, 
                                     final String desc, String signature, 
                                     final String[] exceptions) 
    {

        final MethodVisitor mv = super.visitMethod(access, name, desc, 
                                                   signature, exceptions);
        if (!methodList.contains(new MethodDecl(access, name, desc)))
            return mv;
        return new SimpleAdviceAdapter(mv, className, access, name, desc) {
            protected void onMethodEnter() {
                Type cmdType = Type.getArgumentTypes(desc)[0];

                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKESTATIC, "jtaint/ExecUtil",
                        "validateExec", "(" + cmdType.getDescriptor() + ")V");
            }
        };
    }

    public static InstrumentationBuilder builder() { 
        return Builder.getInstance(); 
    }

    private static class Builder implements InstrumentationBuilder {
        private static final Builder b = new Builder();

        public static InstrumentationBuilder getInstance() { return b; }

        public ClassVisitor build(ClassVisitor cv) {
            return new ProcessBuilderAdapter(cv);
        }
    }
}
