
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

public class BadlyOverriddenAdapterTest extends JFrame
{
	public BadlyOverriddenAdapterTest() {
		addWindowListener( new WindowAdapter() {
			@SuppressWarnings("DM_EXIT")
			public void windowClosing() {
				dispose();
				System.exit(0);
			}		
		});
		
		Container cp = getContentPane();
		cp.add( new JButton( "Click Me" ));
	}
}

class GoodlyOverridenAdapterTest extends JFrame
{
	public GoodlyOverridenAdapterTest() {
		addWindowListener( new WindowAdapter() {
			public void windowClosing() {
				dispose();
				System.exit(0);
			}	
			
			public void windowClosing( WindowEvent we ) {
				windowClosing();
			}
		});
		
		Container cp = getContentPane();
		cp.add( new JButton( "Click Me" ));
	}
}