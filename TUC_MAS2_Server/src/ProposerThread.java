import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;


/**
 * @author mamakos
 *
 */
public class ProposerThread implements Runnable
{
	UDPServer server = null;
	int proposer = 0;
	
	public ProposerThread(UDPServer server, int proposer)
	{
		this.server = server;
		this.proposer = proposer;	// proposer has an id value
	}

	@Override
	public void run()
	{
		int nDummies = server.getNDummies();
		int i = proposer - nDummies;
		
		DatagramSocket socket = server.getServerSockets()[i];
		DatagramPacket receivePacket = server.getReceivePackets()[i];
		
		try
		{
			double start = System.currentTimeMillis();
			socket.receive(receivePacket);
			double end = System.currentTimeMillis();
			
			double time = end - start;
			
			server.getScreen().print("Time waiting for agent " + proposer + " (" + server.getNames()[proposer] 
									+ ") to propose : " + (time/1000.0) + " s");
			
			String msg = new String(receivePacket.getData(), 0, receivePacket.getLength(), "UTF-8");
			
			server.editProposals(msg, proposer);
		}
		catch (IOException e)
		{
			server.getScreen().print(e.getMessage());
		}
	}
}
