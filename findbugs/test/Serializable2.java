import java.io.*;

class Serializable2  {
	int x;
	Serializable2(int i) {
		x = i;
		}

	static class Inner extends Serializable2 implements Serializable {
		Inner() {
			super(42);
			}
		}
	static public void main(String args[]) throws Exception {
		ByteArrayOutputStream pout = new ByteArrayOutputStream();
		ObjectOutputStream oout = new ObjectOutputStream(pout);
		oout.writeObject(new Inner());
		oout.close();
		byte b[] = pout.toByteArray();
		ByteArrayInputStream pin = new ByteArrayInputStream(b);
		ObjectInputStream oin = new ObjectInputStream( pin);
		Object o = oin.readObject();
		System.out.println("read object");
		System.out.println(o);
		}
	}
