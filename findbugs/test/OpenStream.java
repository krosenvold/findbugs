import java.io.*;

public class OpenStream {
	public OutputStream os;

	public static void main(String[] argv) throws Exception {
		FileInputStream in = null;

		try {
			in = new FileInputStream(argv[0]);
		} finally {
			// Not guaranteed to be closed here!
			if (Boolean.getBoolean("inscrutable"))
				in.close();
		}

		FileInputStream in2 = null;
		try {
			in2 = new FileInputStream(argv[1]);
		} finally {
			// This one will be closed
			if (in2 != null)
				in2.close();
		}

		// oops!  exiting the method without closing the stream
	}

	public void byteArrayStreamDoNotReport() {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(b);

		out.println("Hello, world!");
	}

	public void systemInDoNotReport() throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.println(reader.readLine());
	}

	public void socketDoNotReport(java.net.Socket socket) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		System.out.println(reader.readLine());
	}

	public void paramStreamDoNotReport(java.io.OutputStream os) throws IOException {
		PrintStream ps = new PrintStream(os);
		ps.println("Hello");
	}

	public void loadFromFieldDoNotReport() throws IOException {
		PrintStream ps = new PrintStream(os);
		ps.println("Hello");
	}
}

// vim:ts=4
