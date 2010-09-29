package edu.umd.cs.findbugs.flybush;

import java.util.Set;

public interface DbEvaluation extends Comparable<DbEvaluation> {

    String getDesignation();

    void setDesignation(String designation);

    String getComment();

    void setComment(String comment);

    DbIssue getIssue();

    void setIssue(DbIssue issue);

    long getWhen();

    void setWhen(long when);

    Comparable<?> getWho();

    String getWhoId();

    void setWho(Object user);

    String getEmail();

    void setEmail(String email);

    String getPrimaryClass();

    void setPrimaryClass(String primaryClass);

    Set<String> getPackages();

    void setPackages(Set<String> packages);

    void setInvocation(DbInvocation invocation);

    Object getInvocationKey();
}
