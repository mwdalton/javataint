/* Copyright 2009 Michael Dalton */
package jtaint;

import org.objectweb.asm.AnnotationVisitor;

/** Allow multiple adapters to visit a single annotation. Required by
 * MultiMethodAdapter.
 */

public class MultiAnnotationAdapter implements AnnotationVisitor
{
    private final AnnotationVisitor av1, av2;

    public MultiAnnotationAdapter(AnnotationVisitor av1, 
            AnnotationVisitor av2) {
        this.av1 = av1;
        this.av2 = av2;
    }

    public void visit(String name, Object value) {
        av1.visit(name, value);
        av2.visit(name, value);
    }

    public void visitEnum(String name, String desc, String value) {
        av1.visitEnum(name, desc, value);
        av2.visitEnum(name, desc, value);
    }

    public AnnotationVisitor visitAnnotation(String name, String desc) {
        return new MultiAnnotationAdapter(
                av1.visitAnnotation(name, desc),
                av2.visitAnnotation(name, desc));
    }

    public AnnotationVisitor visitArray(String name) {
        return new MultiAnnotationAdapter(
                av1.visitArray(name),
                av2.visitArray(name));
    }

    public void visitEnd() {
        av1.visitEnd();
        av2.visitEnd();
    }
}
