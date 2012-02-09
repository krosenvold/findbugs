package sfBugs;

import java.io.IOException;
import java.io.InputStream;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class RFE3485034 {

    @ExpectWarning("OBL_UNSATISFIED_OBLIGATION,OS_OPEN_STREAM")
    public int getByteBad(String s) throws IOException {
        InputStream in = RFE3485034.class.getResourceAsStream(s);
        return in.read();
    }

    @NoWarning("OBL_UNSATISFIED_OBLIGATION,OS_OPEN_STREAM")
    public int getByteGood(String s) throws IOException {
        InputStream in = RFE3485034.class.getResourceAsStream(s);
        try {
            return in.read();
        } finally {
            in.close();
        }
    }

}
