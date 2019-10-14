import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class Screen extends JFrame
{
	JTextArea ta = null;
	JScrollPane sp = null;
	private static final long serialVersionUID = 1L-100;
	
	public Screen()
	{
		ta = new JTextArea("Welcome!");
		ta.setBackground(Color.WHITE);
		ta.setEditable(false);
		
		sp = new JScrollPane();
		sp.setViewportView(ta);
		
		this.add(sp);
				
		this.setBounds(400, 50, 600, 650);
		this.setVisible(true);
		this.setTitle("TUC Coaltion Formation Game");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	public synchronized void print(String s1)
	{
		ta.append("\n" + s1);
		
		int length = ta.getText().length();
		ta.setCaretPosition(length);
	}
	
}
