/* Copyright 2009 Michael Dalton */
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
