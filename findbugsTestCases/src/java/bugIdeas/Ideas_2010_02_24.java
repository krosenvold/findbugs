package bugIdeas;

import java.util.Date;

import org.joda.time.DateTimeConstants;
import org.joda.time.Instant;
import org.joda.time.Interval;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2010_02_24 {

	@ExpectWarning("ICAST_INT_2_LONG_AS_INSTANT")
	public Date bad1(int x) {
		return new Date(x * 1000);
	}
	@ExpectWarning("ICAST_INT_2_LONG_AS_INSTANT")
	public Date bad2(int x) {
		return new Date(1000 * x);
	}
	@ExpectWarning("ICAST_INT_2_LONG_AS_INSTANT")
	public java.sql.Date bad3(int x) {
		return new java.sql.Date(1000 * x);
	}
	@ExpectWarning("ICAST_INT_2_LONG_AS_INSTANT")
	public Instant bad4(int x) {
		return new Instant(x * 1000);
	}
	@ExpectWarning("ICAST_INT_2_LONG_AS_INSTANT")
	public Date bad5(int x) {
		return new Date(x);
	}
	
	@ExpectWarning("ICAST_INT_2_LONG_AS_INSTANT")
	public java.sql.Date bad6(int x) {
		return new java.sql.Date(x);
	}
	@ExpectWarning("ICAST_INT_2_LONG_AS_INSTANT")
	public Instant bad7(int x) {
		return new Instant(x);
	}

	public Interval maybeOK(int x) {
		return new Interval(x * DateTimeConstants.MILLIS_PER_HOUR, (x+1) * DateTimeConstants.MILLIS_PER_HOUR  );
	}
	public Interval bad8(int x, int y) {
		return new Interval(x * 1000, y*1000  );
	}
	public static void main(String args[]) {
		long x = System.currentTimeMillis();
		System.out.println(Long.toHexString(x));
		System.out.println(Long.toHexString(x/1000));
		System.out.println(Integer.MAX_VALUE - x/1000);
		System.out.println(new Date(Integer.MIN_VALUE));
		System.out.println(new Date(0));
		System.out.println(new Date(Integer.MAX_VALUE));
		System.out.println(new Date(Integer.MAX_VALUE * 1000L));
	}

	
	/**
	 * Checking FP NPE warnings generated by Eclipse to ensure we don't generate them
	 * 
	 * 	https://bugs.eclipse.org/bugs/show_bug.cgi?id=195638
	 * 
	 */


	
	@ExpectWarning(value = "NP_NULL_ON_SOME_PATH_EXCEPTION")
	@NoWarning(value = "NP_ALWAYS_NULL,NP_NULL_ON_SOME_PATH")
	public static void falsePositive() {
		String str = null;
		for (int i = 0; i < 2; i++) {
			try {
				str = new String("Test");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			str.charAt(i); // Error : "Null pointer access: The
			// variable str can only be null at
			// this location"
			str = null;
		}
	}
	@ExpectWarning(value = "NP_NULL_ON_SOME_PATH")
	public static void truePositive(boolean b) {
		String str = null;
		for (int i = 0; i < 2; i++) {
			if (b)
				str = new String("Test");

			str.charAt(i); // Error : "Null pointer access: The
			// variable str can only be null at
			// this location"
			str = null;
		}
	}
}
