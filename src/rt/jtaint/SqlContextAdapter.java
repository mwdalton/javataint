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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;


final class SqlContextAdapter extends GenericContextAdapter 
                              implements Opcodes
{
    public  static final Klass CONNECTION;
    public  static final Klass STATEMENT;
    public  static final Klass ROWSET;
    private static final Map methods;

    static {
        CONNECTION = new Klass("java.sql.Connection");
        STATEMENT  = new Klass("java.sql.Statement");
        ROWSET     = new Klass("javax.sql.RowSet");

        /* Initialize method map */
        HashMap h = new HashMap();

        /* java.sql.Connection */
        h.put(new MethodDecl(ACC_PUBLIC, "prepareCall",
                    "(Ljava/lang/String;)Ljava/sql/CallableStatement;"),
                CONNECTION);
        h.put(new MethodDecl(ACC_PUBLIC, "prepareCall",
                    "(Ljava/lang/String;II)Ljava/sql/CallableStatement;"),
                CONNECTION);
        h.put(new MethodDecl(ACC_PUBLIC, "prepareCall",
                    "(Ljava/lang/String;III)Ljava/sql/CallableStatement;"),
                CONNECTION);
        h.put(new MethodDecl(ACC_PUBLIC, "prepareStatement",
                    "(Ljava/lang/String;)Ljava/sql/PreparedStatement;"),
                CONNECTION);
        h.put(new MethodDecl(ACC_PUBLIC, "prepareStatement",
                    "(Ljava/lang/String;I)Ljava/sql/PreparedStatement;"),
                CONNECTION);
        h.put(new MethodDecl(ACC_PUBLIC, "prepareStatement",
                    "(Ljava/lang/String;[I)Ljava/sql/PreparedStatement;"),
                CONNECTION);
        h.put(new MethodDecl(ACC_PUBLIC, "prepareStatement",
                    "(Ljava/lang/String;II)Ljava/sql/PreparedStatement;"),
                CONNECTION);
        h.put(new MethodDecl(ACC_PUBLIC, "prepareStatement",
                    "(Ljava/lang/String;III)Ljava/sql/PreparedStatement;"),
                CONNECTION);
        h.put(new MethodDecl(ACC_PUBLIC, "prepareStatement",
                    "(Ljava/lang/String;[Ljava/lang/String;)Ljava/sql/PreparedStatement;"),
                CONNECTION);

        /* java.sql.Statement */
        h.put(new MethodDecl(ACC_PUBLIC, "addBatch",
                    "(Ljava/lang/String;)V"), STATEMENT);
        h.put(new MethodDecl(ACC_PUBLIC, "execute",
                    "(Ljava/lang/String;)Z"), STATEMENT);
        h.put(new MethodDecl(ACC_PUBLIC, "execute",
                    "(Ljava/lang/String;I)Z"), STATEMENT);
        h.put(new MethodDecl(ACC_PUBLIC, "execute",
                    "(Ljava/lang/String;[I)Z"), STATEMENT);
        h.put(new MethodDecl(ACC_PUBLIC, "execute",
                    "(Ljava/lang/String;[Ljava/lang/String;)Z"), STATEMENT);
        h.put(new MethodDecl(ACC_PUBLIC, "executeQuery",
                    "(Ljava/lang/String;)Ljava/sql/ResultSet;"), STATEMENT);
        h.put(new MethodDecl(ACC_PUBLIC, "executeUpdate",
                    "(Ljava/lang/String;)I"), STATEMENT);
        h.put(new MethodDecl(ACC_PUBLIC, "executeUpdate",
                    "(Ljava/lang/String;I)I"), STATEMENT);
        h.put(new MethodDecl(ACC_PUBLIC, "executeUpdate",
                    "(Ljava/lang/String;[I)I"), STATEMENT);
        h.put(new MethodDecl(ACC_PUBLIC, "executeUpdate",
                    "(Ljava/lang/String;[Ljava/lang/String;)I"), STATEMENT);

        /* javax.sql.RowSet */
        h.put(new MethodDecl(ACC_PUBLIC, "setCommand", 
                    "(Ljava/lang/String;)V"), ROWSET);
        methods = h;
    }

    public SqlContextAdapter(ClassVisitor cv) {
        super(cv, methods);
    }
}
