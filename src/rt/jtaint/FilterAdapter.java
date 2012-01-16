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

import java.util.Set;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.objectweb.asm.commons.SimpleAdviceAdapter;

final class FilterAdapter extends ClassAdapter implements Opcodes
{
    private String className;
    private Set xssMethods;

    public FilterAdapter(ClassVisitor cv, FilterContextAdapter ftc) {
        super(cv);
    }

    public void visit(int version, int access, String name, String signature, 
                      String superName, String[] interfaces) 
    {
        cv.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
        this.xssMethods = (Set) Configuration.xssFilters.get(name);
    }

    public MethodVisitor visitMethod(final int access, final String name, 
                                     final String desc, String signature, 
                                     String[] exceptions) 
    {  
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, 
                                          exceptions);

        if (xssMethods == null || (!xssMethods.contains(name) 
                                   && !xssMethods.contains("*"))) 
            return mv;

        Type retType = Type.getReturnType(desc);
        if (retType.getSort() != Type.OBJECT 
                || !"java/lang/String".equals(retType.getInternalName())) 
            return mv;

        return new SimpleAdviceAdapter(mv, className, access, name, desc) {
            protected void onMethodExit(int opcode) {
                if (opcode != ARETURN) return;
                mv.visitMethodInsn(INVOKESTATIC, "jtaint/StringUtil",
                                   "toUntainted",
                                   "(Ljava/lang/String;)Ljava/lang/String;");
            }
        };
    }
}
