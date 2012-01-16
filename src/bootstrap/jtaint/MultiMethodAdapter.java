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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Label;

/** Allow multiple MethodAdapters to visit a single method. Ensures that
 * each MethodAdapter receives its own set of Label objects by cloning 
 * Labels when appropriate.
 */

public class MultiMethodAdapter implements MethodVisitor
{
    private final MethodVisitor mv1, mv2;

    /* Methods cannot share labels */
    private Map labelMap = new HashMap();

    private Label remap(Label in) {
        Label out = (Label) labelMap.get(in);
        if (out == null) {
            out = new Label();
            labelMap.put(in, out);
        }
        return out;
    }

    private Label[] remapArray(Label[] in) {
        Label[] out = new Label[in.length];

        for (int i = 0; i < in.length; i++)
            out[i] = remap(in[i]);
        return out;
    }

    public MultiMethodAdapter(MethodVisitor mv1, MethodVisitor mv2) {
        this.mv1 = mv1;
        this.mv2 = mv2;
    }

    public AnnotationVisitor visitAnnotationDefault() {
        return new MultiAnnotationAdapter(
                mv1.visitAnnotationDefault(),
                mv2.visitAnnotationDefault());
    }

    public AnnotationVisitor visitAnnotation(String desc, 
            boolean visible) 
    {
        return new MultiAnnotationAdapter(
                mv1.visitAnnotation(desc, visible),
                mv2.visitAnnotation(desc, visible));
    }

    public AnnotationVisitor visitParameterAnnotation(
            int parameter, String desc, boolean visible) 
    {
        return new MultiAnnotationAdapter(
                mv1.visitParameterAnnotation(parameter, desc, visible),
                mv2.visitParameterAnnotation(parameter, desc, visible));
    }

    /* XXX TODO - Is there a good generic way to 'wrap' attr and ensure that
     * its getLabel routine returns remap()'d?.
     * This issue does not currently apply to our codebase because asm 
     * strips all optional attributes by default, and none of our classes
     * override org.object.asm.Attribute */
    public void visitAttribute(Attribute attr) {
        mv1.visitAttribute(attr);
        mv2.visitAttribute(attr);
    }

    public void visitCode() {
        mv1.visitCode();
        mv2.visitCode();
    }

    public void visitFrame(int type, int nLocal, Object[] local, 
            int nStack, Object[] stack)
    {
        mv1.visitFrame(type, nLocal, local, nStack, stack);
        mv2.visitFrame(type, nLocal, local, nStack, stack);
    }

    public void visitInsn(int opcode) {
        mv1.visitInsn(opcode);
        mv2.visitInsn(opcode);
    }

    public void visitIntInsn(int opcode, int operand) {
        mv1.visitIntInsn(opcode, operand);
        mv2.visitIntInsn(opcode, operand);
    }

    public void visitVarInsn(int opcode, int var) {
        mv1.visitVarInsn(opcode, var);
        mv2.visitVarInsn(opcode, var);
    }

    public void visitTypeInsn(int opcode, String type) {
        mv1.visitTypeInsn(opcode, type);
        mv2.visitTypeInsn(opcode, type);
    }

    public void visitFieldInsn(int opcode, String owner, String name, 
            String desc)
    {
        mv1.visitFieldInsn(opcode, owner, name, desc);
        mv2.visitFieldInsn(opcode, owner, name, desc);
    }

    public void visitMethodInsn(int opcode, String owner, String name, 
            String desc)
    {
        mv1.visitMethodInsn(opcode, owner, name, desc);
        mv2.visitMethodInsn(opcode, owner, name, desc);
    }

    public void visitJumpInsn(int opcode, Label label) {
        mv1.visitJumpInsn(opcode, label);
        mv2.visitJumpInsn(opcode, remap(label));
    }

    public void visitLabel(Label label) {
        mv1.visitLabel(label);
        mv2.visitLabel(remap(label));
    }

    public void visitLdcInsn(Object cst) {
        mv1.visitLdcInsn(cst);
        mv2.visitLdcInsn(cst);
    }

    public void visitIincInsn(int var, int increment) {
        mv1.visitIincInsn(var, increment);
        mv2.visitIincInsn(var, increment);
    }

    public void visitTableSwitchInsn(int min, int max, Label dflt, 
            Label[] labels) 
    {
        mv1.visitTableSwitchInsn(min, max, dflt, labels);
        mv2.visitTableSwitchInsn(min, max, remap(dflt), 
                remapArray(labels));
    }

    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        mv1.visitLookupSwitchInsn(dflt, keys, labels);
        mv2.visitLookupSwitchInsn(remap(dflt), keys, remapArray(labels));
    }

    public void visitMultiANewArrayInsn(String desc, int dims) {
        mv1.visitMultiANewArrayInsn(desc, dims);
        mv2.visitMultiANewArrayInsn(desc, dims);
    }

    public void visitTryCatchBlock(Label start, Label end, Label handler, 
            String type) 
    {
        mv1.visitTryCatchBlock(start, end, handler, type);
        mv2.visitTryCatchBlock(remap(start), remap(end), 
                               remap(handler), type);
    }

    public void visitLocalVariable(String name, String desc, String signature,
        Label start, Label end, int index) 
    {
        mv1.visitLocalVariable(name, desc, signature, start, end, index);
        mv2.visitLocalVariable(name, desc, signature, 
                               remap(start), remap(end), index);
    }

    public void visitLineNumber(int line, Label start) {
        mv1.visitLineNumber(line, start);
        mv2.visitLineNumber(line, remap(start));
    }

    public void visitMaxs(int nStack, int nLocals) {
        mv1.visitMaxs(nStack, nLocals);
        mv2.visitMaxs(nStack, nLocals);
    }

    public void visitEnd() {
        mv1.visitEnd();
        mv2.visitEnd();
    }
}
