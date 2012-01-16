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
