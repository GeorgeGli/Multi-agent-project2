import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * @author mamakos
 *
 */
public class UDPServer
{
	// game-related fields
	private int nAgents = 1;				// the number of non-dummy agents
	private int N = 12;						// the total number of agents
	private int nDummies = N - nAgents;		// the number of dummy agents
	private int nSubtypes = 3;				// the number of sub-types (good-medium-bad)
	private int nSubPerMain = 2;			// each main type has # of each sub-type
	private int nTypes = N/nSubtypes/nSubPerMain;	// the number of main types
	private Dummy[] dummies = null;				// a vector containing all of the dummies
	private Random rng = null;					// random number generator
	private ArrayList<String> typeset = null;	// containing N elements
	private int[][] typeMatrix = null;			// matrix containing all of the types
	private String[] names = null;				// vector containing all of the names
	private int[] ids = null;					// vector containing all of the id's
	private int iters = 300;
	private TaskGenerator tg = null;
	private int reqAgents = 3;					// requested agents per task
	private Task[] tasks = null;				// current tasks 
	private double[] scores = null;
	private int stepReward = 5;					// reward have to be a multiple of 0.05 of the value
	private int[] proposers = null;				// needed as field due to Threads
	private String[] proposals = null;
	private boolean allReceived = false;		// true when all of the proposals have been received
	private int[] responses = null;				// the vector with the agent+dummies responses
	private int nResponses = 0;					// the number of proposed agents
	private int cResponses = 0;					// the number of agents who have responded
	private static boolean properOperation = false;	// the iterations have finished successfully
	
	// connection-related fields
	private static final int PORT1 = 9876;
	private DatagramSocket serverSocket1 = null;
	private byte[] receiveData1 = null;
	private DatagramPacket receivePacket1 = null;
	private int size = 200;
	private static int[] PORTS = null;
	private DatagramSocket[] serverSockets = null;
	private byte[][] receiveData = null;
	private byte[][] sendData = null;
	private DatagramPacket[] receivePackets = null;
	private DatagramPacket[] sendPackets = null;	
	private int checkDelay = 200;
	private int delayNewRound = 300;
	
	private Screen screen = null;
	

	// the empty constructor needs to be defined
	private UDPServer()
	{
		
	}
	
	private UDPServer(int iters)
	{
		this.iters = iters;
	}
	
	private UDPServer(int iters, int nAgents)
	{
		this.iters = iters;
		this.nAgents = nAgents;
		nDummies = N - nAgents;
	}
	
	private UDPServer(int iters, int nAgents, int delayNewRound)
	{
		this.iters = iters;
		this.nAgents = nAgents;
		nDummies = N - nAgents;
		this.delayNewRound = delayNewRound;
	}
	
	private UDPServer(int iters, int nAgents, int delayNewRound, int N)
	{
		this.iters = iters;
		this.nAgents = nAgents;
		this.N = N;
		nDummies = N - nAgents;
		this.delayNewRound = delayNewRound;
	}
	
	private void initializePackets()
	{
		try
		{
			// creating connection fields for the first time of communication
			serverSocket1 = new DatagramSocket(PORT1);
			receiveData1 = new byte[size];
			receivePacket1 = new DatagramPacket(receiveData1, receiveData1.length);
			
			// creating connection fields for each one of the agents
			PORTS = new int[nAgents];
			serverSockets = new DatagramSocket[nAgents];
			receiveData = new byte[nAgents][size];
			sendData = new byte[nAgents][size];
			receivePackets = new DatagramPacket[nAgents];
			sendPackets = new DatagramPacket[nAgents];

			for(int i=0; i<nAgents; i++)
			{
				PORTS[i] = PORT1 - i - 1;
				serverSockets[i] = new DatagramSocket(PORTS[i]);
				receiveData[i] = new byte[size];
				sendData[i] = new byte[size];
				receivePackets[i] = new DatagramPacket(receiveData[i], receiveData[i].length);
				sendPackets[i] = new DatagramPacket(sendData[i], sendData[i].length);
			}				
		}
		catch(SocketException e)
		{
			// print the occured exception
			System.out.println(e.getClass().getName() + " : " + e.getMessage());
		}
	}
	
	private void generateDummies()
	{
		// game-related fields initialization
		dummies = new Dummy[nDummies];
		rng = new Random();
		typeset = new ArrayList<String>();
		typeMatrix = new int[N][2];
		names = new String[N];
		ids = new int[N];
		tg = new TaskGenerator(N, nTypes, ids, typeMatrix);
		tasks = new Task[tg.getNTasks()];
		
		scores = new double[N];
		
		for(int i=0; i<nTypes; i++)
			for(int j=0; j<nSubtypes; j++)
				for(int k=0; k< nSubPerMain; k++)
					typeset.add(i + "," + j);		// "main,sub"
		
		for(int i=0; i<nDummies; i++)
		{
			// get a type from the typeset
			int rand = rng.nextInt(typeset.size());
			String type = typeset.get(rand);
			String[] types = type.split(",");
			int mainType = Integer.parseInt(types[0]);
			int subType = Integer.parseInt(types[1]);
			typeset.remove(rand);
			typeMatrix[i][0] = mainType;
			typeMatrix[i][1] = subType;
			
			ids[i] = i;
			
			// create the dummy
			dummies[i] = new Dummy(i, N, mainType, subType);
			names[i] = dummies[i].getName();
		}
		
		screen = new Screen();
		screen.print(nDummies + " dummies have been created!\nReady to receive the agents!");
	}
	
	private void receivePlayers()
	{
		for(int i=0; i<nAgents; i++)
		{
			try
			{
				// the first message of all of the agents is to same server socket
				serverSocket1.receive(receivePacket1);
				
				// configure packages for agent i
				sendPackets[i].setAddress(receivePacket1.getAddress());
				sendPackets[i].setPort(receivePacket1.getPort());
				
				// save his name
				names[i+nDummies] = new String(receivePacket1.getData(), 0, receivePacket1.getLength(), "UTF-8");
				
				// assign him a type (main-sub)
				int rand = rng.nextInt(typeset.size());
				String type = typeset.get(rand);
				String[] types = type.split(",");
				int mainType = Integer.parseInt(types[0]);
				int subType = Integer.parseInt(types[1]);
				typeset.remove(rand);
				typeMatrix[i+nDummies][0] = mainType;
				typeMatrix[i+nDummies][1] = subType;
				
				ids[i + nDummies] = i + nDummies;
				
				// inform agent #(i+nDummies) about the port he is sending to and his type
				// Form of the message : P,PORTS[i],iters,N,id,Main,Sub
				String msg = "P,";
				msg += PORTS[i] + ",";
				msg += iters + ",";
				msg += N + ",";
				msg += ids[i + nDummies] + ",";
				msg += type;
				
				sendData[i] = msg.getBytes("UTF-8");
				sendPackets[i].setData(sendData[i]);
				sendPackets[i].setLength(sendData[i].length);
				serverSockets[i].send(sendPackets[i]);
				
				screen.print(names[i + nDummies] + " with id " + ids[i + nDummies] + " has joined!");				
			}
			catch(IOException e)
			{
				screen.print(e.getClass().getName() + " : " + e.getMessage());
			}		
		}
		
		tg.setIds(ids);
		tg.setTypeMatrix(typeMatrix);
	}
	
	private void doWait(int time)
	{		
		synchronized(this)
		{
			try
			{
				this.wait(time);
			} 
			catch (InterruptedException e)
			{
				screen.print(e.getClass().getName() + " : " + e.getMessage());
			}
		}
	}
	
	private void sendTypes()
	{
		screen.print("Printing  main types------------");
		
		String s1 = Integer.toString(typeMatrix[0][0]);
		for(int i=1; i<N; i++)
			s1 += "," + Integer.toString(typeMatrix[i][0]);
		
		screen.print(s1);

		// define the types string
		String types = Integer.toString(typeMatrix[0][0]);
		
		for(int i=1; i<N; i++)
			types += "," + typeMatrix[i][0];
		
		// set the main types on the dummies
		for(int i=0; i<nDummies; i++)
			dummies[i].setTypes(types);
		
		types = "M," + types;
		// inform agent each agent about the main types of all of the agents
		// Form of the message : M,main0,main1,...,mainN-1
		
		for(int i=0; i<nAgents; i++)
		{
			try
			{
				sendData[i] = types.getBytes("UTF-8");
				sendPackets[i].setData(sendData[i]);
				sendPackets[i].setLength(sendData[i].length);
				serverSockets[i].send(sendPackets[i]);
			}
			catch(IOException e)
			{
				screen.print(e.getClass().getName() + " : " + e.getMessage());
			}				
		}		
	}
	
	private void loop()
	{	
		screen.print("Time pause at each new round : " + delayNewRound + " ms");
		
		for(int iter=0; iter<iters; iter++)	// iters
		{
			// short pause at every new iteration
			this.doWait(delayNewRound);
			
			tasks = tg.generateTasks();

			screen.print("----------------   Round  " + (iter+1) + " ----------------");
			
			// get the proposers
			proposers = new int[tg.getNTasks()];
			
			// proposer has id value
			for(int i=0; i<tasks.length; i++)
				proposers[i] = tasks[i].getProposer();
			
			// form the string to be sent to any agent
			// Form of the message : 
			// "T|proposer0,value0,#type0_0,#type0_1,taskType0,taskId0|...|
			//    proposer3,value3,#type3_0,#type3_1,taskType3,taskId3"
			String msg = "T";
			
			for(int i=0; i<tasks.length; i++)
			{
				msg += "#";
				msg += Integer.toString(tasks[i].getProposer()) + ",";
				msg += Integer.toString(tasks[i].getValue()) + ",";
				
				for(int j=0; j<nTypes; j++)
					msg += Integer.toString(tasks[i].getDemand()[j]) + ",";
				
				msg += Integer.toString(tasks[i].getType()) + ",";
				msg += i;
			}
			
			// send message to all of the dummies
			for(int i=0; i<nDummies; i++)
				dummies[i].receiveMsg(msg);
			
			// send message to all of the agents
			for(int i=0; i<nAgents; i++)
			{
				try
				{
					sendData[i] = msg.getBytes("UTF-8");
					sendPackets[i].setData(sendData[i]);
					sendPackets[i].setLength(sendData[i].length);
					serverSockets[i].send(sendPackets[i]);
				}
				catch(IOException e)
				{
					screen.print(e.getClass().getName() + " : " + e.getMessage());
				}
			}
			
			proposals = new String[proposers.length];
			
			for(int i=0; i<proposers.length; i++)
				proposals[i] = "";
			
			allReceived = false;
			
			// create a thread to listen from every proposer
			for(int proposerIndex=0; proposerIndex<proposers.length; proposerIndex++)
			{	
				if(!(proposers[proposerIndex] < nDummies)) 	// if the proposer is an agent
				{
					Runnable r = new ProposerThread(this, proposers[proposerIndex]);
					new Thread(r).start();
				}
				else	// if the proposer is a dummy
				{
					int dummyId = proposers[proposerIndex];
					String proposal = dummies[dummyId].makeProposal();
					this.editProposals(proposal, dummyId);
				}
			}
			
			while(!this.allProposalsReceived())
			{
				this.doWait(checkDelay);
			}
			
			// check for the validity of the proposals
			if(!this.validProposals())
			{
				screen.print("Fatal error in proposal!");
				return;
			}
			
			// inform all of the potential members of the coalitions
			// Form of the message :
			// for potential members "C|id0,int0*5,id1,int1*5,proposer,value,taskId|..."
			// for non-potential members : "C"
			String[] messages = new String[N];
			
			for(int i=0; i<N; i++)
				messages[i] = "C";
			
			for(int j=0; j<proposals.length; j++)
			{
				String message = "#" + proposals[j];
				
				String[] fields = proposals[j].split(",");
				int proposer = proposers[j];
				int value = tasks[j].getValue();
				
				message += "," + proposer;
				message += "," + value;
				message += "," + j;		// the task id

				int member1 = Integer.parseInt(fields[0]);
				messages[member1] += message;
				
				int member2 = Integer.parseInt(fields[2]);
				messages[member2] += message;
			}
			
			responses = new int[N];
			for(int i=0; i<N; i++)
				responses[i] = -1;
			
			// send message to all of the dummies,
			// no need to send to the dummies that are not potential members
			// (no need for synchronization at the responses of the dummies)
			for(int i=0; i<nDummies; i++)
				if(!messages[i].equals("C"))
					responses[i] = dummies[i].proposalRespond(messages[i]);
			
			// send message to all of the agents 
			for(int i=0; i<nAgents; i++)
			{
				try
				{
					sendData[i] = messages[i+nDummies].getBytes("UTF-8");
					sendPackets[i].setData(sendData[i]);
					sendPackets[i].setLength(sendData[i].length);
					serverSockets[i].send(sendPackets[i]);
				}
				catch(IOException e)
				{
					screen.print(e.getClass().getName() + " : " + e.getMessage());
				}
			}
			
			nResponses = 0;
			cResponses = 0;
			
			// receive message from all of the agents
			// who are potential coalition members
			for(int i=0; i<nAgents; i++)
				if(!messages[i+nDummies].equals("C"))
				{
					nResponses++;
					Runnable r = new RespondOfferThread(this, i);
					new Thread(r).start();
				}
			
			while(!this.allResponsesReceived())
			{
				this.doWait(checkDelay);
			}
			
			// all of the responses have been received
			
			// determine success for formed coalitions
			// -1 is for non-formed coalitions
			int[] success = new int[tasks.length];
			
			for(int i=0; i<success.length; i++)
				success[i] = -1;
			
			for(int i=0; i<tasks.length; i++)
			{
				int proposer = tasks[i].getProposer();
				int taskId = i;
				
				String[] proposal = proposals[i].split(",");
				
				int member1 = Integer.parseInt(proposal[0]);
				int member2 = Integer.parseInt(proposal[2]);
				
				if( (responses[member1] == taskId) && (responses[member2] == taskId) )
					success[i] = this.determineSuccess(proposer, member1, member2);
			}
		
			// inform the proposers about the responses
			for(int i=0; i<tasks.length; i++)
			{
				int proposer = tasks[i].getProposer();
				int wantedTask = i;		// proposers want the task they have offered
				
				String message = "F," + success[wantedTask];

				// if there was not success inform the proposer
				// about the agents that didn't accept his proposal
				if(success[wantedTask] == -1)
				{
					String[] fields = proposals[i].split(",");
					
					int member1 = Integer.parseInt(fields[0]);
					if(responses[member1] != wantedTask)
						message += "," + member1;
					
					int member2 = Integer.parseInt(fields[2]);
					if(responses[member2] != wantedTask)
						message += "," + member2;
				}
				
				if(proposer < nDummies)	// if the proposer is a dummy
					dummies[proposer].informOfOutcome(message);
				else
				{
					try
					{
						int agent = proposer - nDummies;
						
						sendData[agent] = message.getBytes("UTF-8");			
						sendPackets[agent].setData(sendData[agent]);
						sendPackets[agent].setLength(sendData[agent].length);				
						serverSockets[agent].send(sendPackets[agent]);
					}
					catch(IOException e)
					{
						screen.print(e.getClass().getName() + " : " + e.getMessage());
					}
				}
				
				// update score
				if(success[wantedTask] == 1)
				{
					int value = tasks[i].getValue();
					
					String[] fields = proposals[i].split(",");
					
					int perc1 = Integer.parseInt(fields[1]);
					int perc2 = Integer.parseInt(fields[3]);
					
					int percP = 100 - perc1 - perc2;
					
					scores[proposer] += (double)value * ((double)percP/100.0);
				}
				
			}
			
			// inform the rest about the outcome
			// note : if messages[i] == "C" then
			// i is either a proposer or a not-ponential member
			for(int i=0; i<N; i++)
			{
				if(!messages[i].equals("C"))
				{
					String message = "F,";
					
					int wantedTask = responses[i];
					
					if(wantedTask == -1)	// I did not want any task
						message += "-1";	// failure
					else
						message += success[wantedTask];
					
					if(i < nDummies)	// if it is a dummy
						dummies[i].informOfOutcome(message);
					else
					{
						try
						{
							int agent = i - nDummies;
							
							sendData[agent] = message.getBytes("UTF-8");			
							sendPackets[agent].setData(sendData[agent]);
							sendPackets[agent].setLength(sendData[agent].length);				
							serverSockets[agent].send(sendPackets[agent]);
						}
						catch(IOException e)
						{
							screen.print(e.getClass().getName() + " : " + e.getMessage());
						}
					}
					
					// update score
					if( (wantedTask != -1) && (success[wantedTask] == 1))
					{
						int value = tasks[wantedTask].getValue();
						
						String[] fields = proposals[wantedTask].split(",");
						
						int member1 = Integer.parseInt(fields[0]);
						int perc1 = Integer.parseInt(fields[1]);
						int member2 = Integer.parseInt(fields[2]);
						int perc2 = Integer.parseInt(fields[3]);
						
						int perc = 0;
						
						if(i == member1)
							perc = perc1;
						else if(i == member2)
							perc = perc2;
						else
						{
							screen.print("Agent " + i + " (" + names[i] + ") not found in the successful coalition!");
							return;
						}
						
						scores[i] += (double)value * ((double)perc/100.0);
					}
				}
			}			
		}
		
		properOperation = true;
	}

	public synchronized void editProposals(String proposal, int proposer)
	{
		int proposerIndex = -1;
		
		for(int i=0; i<proposers.length; i++)
			if(proposers[i] == proposer)
			{
				proposerIndex = i;
				break;
			}
		
		if(proposerIndex == -1)
			screen.print("Fatal error : Proposer not found");
		
		proposals[proposerIndex] = proposal;
		
		// check if all of the proposals have been received
		int counter = 0;
		
		for(int i=0; i<proposals.length; i++)
			if(!(proposals[i].equals("")))
				counter++;
		
		if(counter == proposals.length)
			allReceived = true;
	}
	
	private synchronized boolean allProposalsReceived()
	{
		return allReceived;
	}
	
	private boolean validProposals()
	{	
		for(int i=0; i<proposals.length; i++)
		{
			int proposer = tasks[i].getProposer();
			int type  = tasks[i].getType();
			
			String[] proposal = proposals[i].split(",");
			
			int member1 = Integer.parseInt(proposal[0]);
			int perc1 = Integer.parseInt(proposal[1]);
			int member2 = Integer.parseInt(proposal[2]);
			int perc2 = Integer.parseInt(proposal[3]);
			
			// check if the percentages of the value are valid
			if((perc1 % stepReward) != 0)
			{
				screen.print("Proposer with id " + proposer + " has offered to " 
						+ member1 + " " + perc1 + "% of the value!\n"
						+ "It should be a multiple of" + stepReward + ".");
				
				return false;
			}
			
			if(perc1 <= 0)
			{
				screen.print("Proposer with id " + proposer + " has offered to " 
						+ member1 + " " + perc1 + "% of the value!\n"
						+ "It should a positive number!");
				
				return false;
			}
			
			if((perc2 % stepReward) != 0)
			{
				screen.print("Proposer with id " + proposer + " has offered to " 
						+ member2 + " " + perc2 + "% of the value!\n"
						+ "It should be a multiple of" + stepReward + ".");
				
				return false;
			}
			
			if(perc2 <= 0)
			{
				screen.print("Proposer with id " + proposer + " has offered to " 
						+ member2 + " " + perc2 + "% of the value!\n"
						+ "It should a positive number!");
				
				return false;
			}
			
			if((perc1 + perc2) > (100-stepReward))
			{
				screen.print("Proposer with id " + proposer + " has offered to " 
						+ member1 + " " + perc1 + "% of the value and to "
						+ member2 + " " + perc2 + "% of the value, which"
						+ "is greater than 100%.");
				
				return false;
			}
			
			// check if the member types are valid
			if( (type == 0) || (type == 1) )
			{
				if(typeMatrix[member1][0] == typeMatrix[member2][0])
				{
					screen.print("Proposer with id " + proposer + " (" + names[proposer] +
							" has proposed to  two members of the same type for task type "
							+ type);
					
					return false;
				}
			}
			else	// task type 2
			{
				int typeP = typeMatrix[proposer][0];
				
				if(typeP == typeMatrix[member1][0])
					if(typeP == typeMatrix[member2][0])
					{
						screen.print("Proposer with id " + proposer +  " (" + names[proposer] +
								") has proposed to " 
								+ " a coalition in which all of the agents are "
								+ " of the same type.");
						
						return false;
					}
			}
			
			// check if any of the two members is a proposer
			for(int j=0; j<tasks.length; j++)
			{
				if(member1 == tasks[i].getProposer())
				{
					screen.print("Proposer with id " + proposer + " has proposed to " 
							+ member1 + " (" + names[member1] + ") who is the proposer of task "
							+ j + ".");
					
					return false;
				}
				
				if(member2 == tasks[i].getProposer())
				{
					screen.print("Proposer with id " + proposer + " has proposed to " 
							+ member2 +  " (" + names[member1] + ") who is the proposer of task "
							+ j + ".");
					
					return false;
				}
			}
			
			// check if member1 == member2
			if(member1 == member2)
			{
				screen.print("Proposer with id " + proposer + " has proposed to " 
						+ member1 + " twice at the same time");
				
				return false;
			}
		}
		
		return true;
	}
	
	public synchronized void editResponses(int response, int agent)
	{
		int agentId = agent + nDummies;
		
		// check if the agent responded for a valid coalition
		if(response != -1)
		{
			String[] prop = proposals[response].split(",");
			int member1 = Integer.parseInt(prop[0]);
			int member2 = Integer.parseInt(prop[2]);
			
			if(agentId != member1)
				if(agentId != member2)
				{
					screen.print("Agent " + agentId +" (" + names[agentId] + ") has responded for task "
								+ response +" but he is not a member of it!");
					return;
				}
		}
		
		cResponses++;
		
		responses[agentId] = response;
	}
	
	private synchronized boolean allResponsesReceived()
	{
		return cResponses == nResponses;
	}
	
	private int determineSuccess(int proposer, int member1, int member2)
	{		
		int[] subs = new int[reqAgents];
		subs[0] = typeMatrix[proposer][1];
		subs[1] = typeMatrix[member1][1];
		subs[2] = typeMatrix[member2][1];
		
		double quality = 0.0;
		
		// good : 2
		// medium : 1.5
		// bad : 1
		
		for(int i=0; i<subs.length; i++)
			if(subs[i] == 0)
				quality += 2.0;
			else if(subs[i] == 1)
				quality += 1.5;
			else
				quality += 1.0;
				
		double val = rng.nextDouble();
		
		if(val < (quality/7.0))
			return 1;
		
		return 0;
	}
	
	private void end()
	{
		screen.print("End of the game!");
		
		screen.print("Printing main and secondary types -----------------");
		
		for(int i=0; i<N; i++)
			screen.print("Agent " + i + " (" + names[i] + ") : " + typeMatrix[i][0] + " " + typeMatrix[i][1]);
		
		screen.print("Printing scores ------------------------------------------------");
		
		double[] scores1 = new double[N];
		double epsilon = 0.0001;
		
		// deep copy
		for(int i=0; i<N; i++)
			scores1[i] = scores[i];
		
		Arrays.sort(scores1);
		
		scores1 = this.reverse(scores1);
		
		String[] names1 = new String[N];
		
		for(int i=0; i<N; i++)
			for(int j=0; j<N; j++)
				if(Math.abs(scores1[i]-scores[j]) < epsilon)
				{
					names1[i] = names[j];
					break;
				}
		
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
		
		for(int i=0; i<N; i++)
			screen.print((i+1) + ". " + names1[i] + " : " + df.format(scores1[i]));
	}
	
	private double[] reverse(double[] scores)
	{
		double[] scores1 = new double[scores.length];
		
		for(int i=scores.length-1; i>-1; i--)
			scores1[i] = scores[scores.length-1-i];
		
		return scores1;
	}
	
	public void errorExit()
	{
		screen.print("Exiting with error...");
	}
	
	public DatagramSocket[] getServerSockets()
	{
		return serverSockets;
	}
	
	public DatagramPacket[] getReceivePackets()
	{
		return receivePackets;
	}
	
	public Screen getScreen()
	{
		return screen;
	}
	
	public int getNDummies()
	{
		return nDummies;
	}
	
	public String[] getNames()
	{
		return names;
	}
	
	// optional use of arguments
	// the first has to be the number of iterations
	// the second has to be the number of non-dummy agents
	// the third has to be the delay time in ms per round
	// the fourth has to be the total number of agents
	public static void main(String[] args)
	{
		UDPServer server = null;
		
		if (args.length == 0)
			server = new UDPServer();
		else if(args.length == 1)
			server = new UDPServer(Integer.parseInt(args[0]));
		else if(args.length == 2)
			server = new UDPServer(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
		else if(args.length == 3)
		{
			server = new UDPServer(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
		}
		
		/*
		else if(args.length == 4)
		{
			int N = Integer.parseInt(args[2]);
			System.out.println((N%6));
			if((N % 6) != 0)
			{
				System.out.println("N mod 6 must be 0!\nExiting...");
				return;
			}
			server = new UDPServer(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]),
									Integer.parseInt(args[3]));
		}
		*/
		
		server.initializePackets();
		server.generateDummies();
		server.receivePlayers();
		
		// 1 second pause
		server.doWait(500);
		
		server.sendTypes();
		
		// 1 second pause
		server.doWait(500);
		
		server.loop();
		
		if(properOperation)
			server.end();
		else
			server.errorExit();
	}

}
