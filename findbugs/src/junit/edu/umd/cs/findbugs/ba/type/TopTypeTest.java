package edu.umd.cs.findbugs.ba.type;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TopTypeTest extends TestCase {
	private TopType top;

	protected void setUp() {
		top = new TopType();
	}

	public void testNotReferenceType() {
		Assert.assertFalse(top.isReferenceType());
	}

	public void testNotBasicType() {
		Assert.assertFalse(top.isBasicType());
	}

	public void testNotValidArrayElementType() {
		Assert.assertFalse(top.isValidArrayElementType());
	}

	public void testEquals() {
		TopType otherTop = new TopType();
		BottomType bottom = new BottomType();

		Assert.assertEquals(top, otherTop);
		Assert.assertFalse(top.equals(bottom));
	}
}

// vim:ts=4
