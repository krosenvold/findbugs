import java.util.*;
import java.io.*;

public class UnneededInstanceOf {
	public void test1(ArrayList l) {
		if (l instanceof List)
			System.out.println("It's a List");
	}

	public void test2(BufferedOutputStream bos) {
		if (bos instanceof OutputStream)
			System.out.println("It's an OutputStream");
	}

	public void test3(SortedSet s) {
		if (s instanceof Set)
			System.out.println("It's a Set");
	}
}