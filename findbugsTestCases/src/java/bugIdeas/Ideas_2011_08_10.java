package bugIdeas;

import java.io.IOException;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public abstract class Ideas_2011_08_10 {
    
    public abstract void doIO() throws IOException;
    
    @NoWarning("DE")
    public void doNotReport() {
        try {
            doIO();
        } catch (IOException ignore) {}
        System.out.println("foobar");
    }
    
    @ExpectWarning("DE")
    public void report() {
        try {
            doIO();
        } catch (IOException veryImportant) {}
        System.out.println("foobar");
        
    }
    @DesireWarning("DE")
    public void report2() {
        try {
            doIO();
        } catch (IOException veryImportant) {}
        
    }
    
    public static
    void setSecurityManager(final SecurityManager s) {
        try {
            s.checkPackageAccess("java.lang");
        } catch (Exception e) {
            // no-op
        }
        setSecurityManager0(s);
    }
    public static
    void setSecurityManager0(final SecurityManager s) {
    }
    
    @NoWarning("NP")
    public static void turnOffSecurity() {
        setSecurityManager(null);
    }
}
