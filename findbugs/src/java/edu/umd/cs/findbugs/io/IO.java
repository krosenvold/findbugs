/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


//   Copyright (C) 2003  William Pugh <pugh@cs.umd.edu>
//   http://www.cs.umd.edu/~pugh
//   
//   This library is free software; you can redistribute it and/or
//   modify it under the terms of the GNU Lesser General Public
//   License as published by the Free Software Foundation; either
//   version 2.1 of the License, or (at your option) any later version.
//
//   This library is distributed in the hope that it will be useful,
//   but WITHOUT ANY WARRANTY; without even the implied warranty of
//   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//   Lesser General Public License for more details.
//

package edu.umd.cs.pugh.io;

import java.io.*;

public class IO {
	static ThreadLocal myByteBuf = new ThreadLocal() {
		protected Object initialValue() {
             		return new byte[4096];
			}
		};
	static ThreadLocal myCharBuf = new ThreadLocal() {
		protected Object initialValue() {
             		return new char[4096];
			}
		};

	public static String readAll(InputStream in) throws IOException {
			return readAll(new InputStreamReader(in));
			}
	public static String readAll(Reader reader) throws IOException {
		BufferedReader r = new BufferedReader(reader);
		StringWriter w = new StringWriter();
		copy(r,w);
		return w.toString();
		}
	public static long copy(InputStream in, OutputStream out)  
			throws IOException {
			return copy(in, out, Long.MAX_VALUE);
			}
	public static long copy(Reader in, Writer out)
			throws IOException {
			return copy(in, out, Long.MAX_VALUE);
			}


	public static long copy(InputStream in, OutputStream out, 
					long maxBytes)  

			throws IOException {
		long total = 0;

		int sz;

		byte buf [] = (byte[]) myByteBuf.get();

		while(maxBytes > 0 &&
			(sz = in.read(buf, 0, 
				(int) Math.min(maxBytes, (long) buf.length)))
				> 0) {
			total += sz;
			maxBytes -= sz;
			out.write(buf, 0, sz);
			}
		return total;
		}
	public static long copy(Reader in, Writer out, 
					long maxChars)  

			throws IOException {
		long total = 0;

		int sz;

		char buf [] = (char[]) myCharBuf.get();

		while(maxChars > 0 &&
			(sz = in.read(buf, 0, 
				(int) Math.min(maxChars, (long) buf.length)))
				> 0) {
			total += sz;
			maxChars -= sz;
			out.write(buf, 0, sz);
			}
		return total;
		}
}


