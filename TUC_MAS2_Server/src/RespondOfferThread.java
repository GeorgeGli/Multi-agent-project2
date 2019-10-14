import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;


/**
 * @author mamakos
 *
 */
public class RespondOfferThread implements Runnable
{
	UDPServer server = null;
	int agent = 0;
	
	public RespondOfferThread(UDPServer server, int agent)
	{
		this.server = server;
		this.agent = agent;		// agentID = agent + nDummies
	}

	@Override
	public void run()
	{	
		DatagramSocket socket = server.getServerSockets()[agent];
		DatagramPacket receivePacket = server.getReceivePackets()[agent];
		
		try
		{
			double start = System.currentTimeMillis();
			socket.receive(receivePacket);
			double end = System.currentTimeMillis();
			
			double time = end - start;
			
			server.getScreen().print("Time waiting for agent " + (agent+server.getNDummies()) + " (" 
									+ server.getNames()[agent + server.getNDummies()]
									+ ") to respond : " + (time/1000.0) + " s");
			
			String msg = new String(receivePacket.getData(), 0, receivePacket.getLength(), "UTF-8");
			int response = Integer.parseInt(msg);

			server.editResponses(response, agent);
		}
		catch (IOException e)
		{
			server.getScreen().print(e.getMessage());
		}
	}
}