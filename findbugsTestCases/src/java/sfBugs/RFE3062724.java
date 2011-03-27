package sfBugs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.Priority;

public class RFE3062724 {

    static abstract class A implements Serializable {

    }

    static class B extends A {
        @ExpectWarning(value="SE_BAD_FIELD", priority=Priority.HIGH)
        RFE3062724 notSerializable;
    }

    static class C extends A {
        @ExpectWarning(value="SE_BAD_FIELD", priority=Priority.HIGH)
        RFE3062724 notSerializable;
    }

    public void writeB(ObjectOutputStream o, B b) throws IOException {
        o.writeObject(b);
    }

    public C readC(ObjectInputStream i) throws IOException, ClassNotFoundException {
        return (C) i.readObject();
    }

}
