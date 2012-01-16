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
import org.objectweb.asm.Opcodes;

public class ConnectionAdapter extends ClassAdapter implements Opcodes
{
    public ConnectionAdapter(ClassVisitor cv) { super (cv); }

    public void visitEnd() {
        cv.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, 
                       ByteCodeUtil.internalName("sqlValidator"),
                       "()Ljtaint/SqlValidator;", null, null).visitEnd();
        cv.visitEnd();
    }

    public static InstrumentationBuilder builder() { 
        return Builder.getInstance(); 
    }

    private static class Builder implements InstrumentationBuilder {
        private static final Builder b = new Builder();

        public static InstrumentationBuilder getInstance() { return b; }

        public ClassVisitor build(ClassVisitor cv) {
            return new ConnectionAdapter(cv);
        }
    }
}
