import java.util.regex.*;

class BadRegEx {

	boolean f(String s) {
		return s.matches("][");
	}

	String g(String s) {
		return s.replaceAll("][", "xx");
	}

	String h(String s) {
		return s.replaceFirst("][", "xx");
	}

	void x(String s) throws Exception {
		Pattern.matches("][", s);
	}

	Pattern y(String s) throws Exception {
		return Pattern.compile("][", Pattern.CASE_INSENSITIVE);
	}

	Pattern z(String s) {
		return Pattern.compile("][");
	}

}
