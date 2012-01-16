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
import org.objectweb.asm.Opcodes;

final class FilterContextAdapter extends ClassAdapter implements Opcodes
{
    private boolean instrumented;

    public FilterContextAdapter(ClassVisitor cv) {
        super(cv);
    }

    public void visit(int version, int access, String name, 
            String signature, String superName, 
            String[] interfaces) 
    {
        cv.visit(version, access, name, signature, superName, interfaces);

        if ((access & (ACC_INTERFACE|ACC_ANNOTATION)) != 0) 
            return;

        if (Configuration.xssFilters.containsKey(name)) 
            instrumented = true;
    }

    public boolean instrumented()        { return instrumented; }
}
