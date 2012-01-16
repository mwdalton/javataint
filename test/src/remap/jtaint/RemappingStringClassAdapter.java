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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingMethodAdapter;
import org.objectweb.asm.commons.RemappingClassAdapter;

public class RemappingStringClassAdapter extends RemappingClassAdapter
        implements Opcodes
{
    private static final Map stringMap;
    private static File baseDir;
    private String className;

    static {
        HashMap h = new HashMap();

        h.put("java/lang/CharSequence", "java/lang/OrigCharSequence");
        h.put("java/lang/String", "java/lang/OrigString");
        h.put("java/lang/String$CaseInsensitiveComparator",
              "java/lang/OrigString$CaseInsensitiveComparator");
        h.put("java/lang/StringBuffer", "java/lang/OrigStringBuffer");

        if (VmInfo.version() >= VmInfo.VERSION1_5) {
            h.put("java/lang/AbstractStringBuilder", "java/lang/OrigAbstractStringBuilder");
            h.put("java/lang/StringBuilder", "java/lang/OrigStringBuilder");
        }

        stringMap = h;
    }

    public static String remapType(Type t) {
        if (t.getSort() != Type.OBJECT && t.getSort() != Type.ARRAY) 
            return null;

        if (t.getSort() == Type.ARRAY) {
            String s = remapType(t.getElementType());
            if (s != null)
                return "[" + s;
            return null;
        }

        String typeName = t.getInternalName();
        if (stringMap.containsKey(typeName)) {
            Type u = Type.getObjectType((String) stringMap.get(typeName));
            return u.getDescriptor();
        }

        return null;
    }

    public RemappingStringClassAdapter(ClassVisitor cv) {
        super(cv, new Remapper() {
            public String map(String typeName) {
                if (stringMap.containsKey(typeName))
                    return (String) stringMap.get(typeName);
               return typeName;
           }
        });
    }

    public void visit(int version, int access, String name, String signature, 
                     String superName, String[] interfaces)
    {
        className = remapper.map(name);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    private void buildToString(int access, boolean hasOffset) {
        /* Do not call super.visitMethod, we don't want remapping done here */
        MethodVisitor mv = cv.visitMethod(access, "toString",  
                                          "()Ljava/lang/String;", null, null);
        if (mv == null) return;

        if ((access & ACC_ABSTRACT) != 0) {
            mv.visitEnd();
            return;
        }
            
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "jtaint/OrigStringUtil",
                           "toBaseString", 
                           "(L" + className + ";)Ljava/lang/String;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    public MethodVisitor visitMethod(int access, String name, String desc, 
                                    String signature, String[] exceptions)
    {
        if ("toString".equals(name) && "()Ljava/lang/String;".equals(desc)) {
            buildToString(access, "java/lang/OrigString".equals(className));
            name = "toOrigString";
        }

        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    protected MethodVisitor createRemappingMethodAdapter(int access,
                                                         String newDesc,
                                                         MethodVisitor mv) {
        return new RemappingStringMethodAdapter(access, newDesc, mv, remapper);
    }

    private static void remapClass(String baseClass, String outputName) 
        throws IOException
    {
        ClassReader cr = new ClassReader(baseClass);
        ClassWriter cw = new ClassWriter(0);

        cr.accept(new RemappingStringClassAdapter(cw), 0);

        File f = new File(baseDir, outputName);
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(cw.toByteArray());
        fos.close();
    }

    private static void usage() {
        System.err.println("Usage: jt-remap <dir>");
        System.err.println("Outputs remapped String classes to the "
                           + "directory specified by dir");
        System.exit(-1);
    }

    private static class RemappingStringMethodAdapter 
        extends RemappingMethodAdapter
    {
        public RemappingStringMethodAdapter(int access, String desc, 
                                            MethodVisitor mv, Remapper remapper)
        {
            super(access, desc, mv, remapper);
        }

        /* Convert all String literals to OrigString */
        public void visitLdcInsn(Object cst) {
            mv.visitLdcInsn(cst);
            if (cst instanceof String) {
                mv.visitMethodInsn(INVOKESTATIC, "jtaint/OrigStringUtil",
                                  "toOrig", 
                                  "(Ljava/lang/String;)Ljava/lang/OrigString;");
            }
        }

        private void convertParams(Type[] args) {
            int[] locals = new int[args.length];

            /* Store all args to locals, converting as needed */
            for (int i = args.length-1; i >= 0; i--) {
                    String newDesc = remapType(args[i]);
                    if (newDesc != null) 
                        mv.visitMethodInsn(INVOKESTATIC, 
                                "jtaint/OrigStringUtil", "toBase",
                                "(" + newDesc + ")" + args[i].getDescriptor());

                locals[i] = newLocal(args[i]);
                mv.visitVarInsn(args[i].getOpcode(ISTORE), locals[i]);
            }

            /* Load all locals back to the stack */
            for (int i = 0; i < args.length; i++) 
                mv.visitVarInsn(args[i].getOpcode(ILOAD), locals[i]);
        }

        public void visitMethodInsn(int opcode, String owner, String name,
                                    String desc) 
        {

            if (stringMap.containsKey(owner)) {
                super.visitMethodInsn(opcode, owner, name, desc);
                return;
            }

            /* Do we actually pass in a string parameter? */
            Type[] args = Type.getArgumentTypes(desc);
            for (int i = 0; i < args.length; i++) {
                if (args[i].getSort() == Type.ARRAY ||
                        (args[i].getSort() == Type.OBJECT && 
                        stringMap.containsKey(args[i].getInternalName()))) {
                    convertParams(args);
                    break;
                }
            }

            /* Can't remap parameter types here, external method invocation */
            mv.visitMethodInsn(opcode, owner, name, desc);

            Type ret = Type.getReturnType(desc);
            String newRet = remapType(ret);
            if (newRet == null)
                return;
            /* Convert return value to the appropriate type */
            mv.visitMethodInsn(INVOKESTATIC, "jtaint/OrigStringUtil", 
                               "toOrig", "(" + ret.getDescriptor() + ")" + 
                               newRet);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1)
            usage();
        baseDir = new File(args[0]);

        remapClass("java.lang.String", "OrigString.class");
        remapClass("java.lang.String$CaseInsensitiveComparator",
                   "OrigString$CaseInsensitiveComparator.class");
        remapClass("java.lang.StringBuffer", "OrigStringBuffer.class");

        if (VmInfo.version() >= VmInfo.VERSION1_5) {
            remapClass("java.lang.AbstractStringBuilder", 
                       "OrigAbstractStringBuilder.class");
            remapClass("java.lang.StringBuilder", "OrigStringBuilder.class");
        }
    }
}
