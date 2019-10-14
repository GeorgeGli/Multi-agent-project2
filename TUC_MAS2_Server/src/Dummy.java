import java.util.ArrayList;
import java.util.Random;



/**
 * @author mamakos
 *
 */
public class Dummy
{
	private int id = 0;
	private int N = 0;
	private int mainType = 0;
	private int subType = 0;
	private String name = "dummy";
	private int[][] typeMatrix = null;
	private int[] types0 = null;		// agents of type 0
	private int[] types1 = null;		// agents of type 1
	private boolean proposer = false;	// I am a proposer
	private int taskValue = 0;			// task value
	private int nType0 = 0;				// agents needed of type0
	private int nType1 = 0;				// agents needed of type1
	private int taskType = -1;			// the type of the task (0,1 or 2)
	private int taskId = -1;			// id of the task
	private int[] proposed = null;		// the two agents that I have proposed to
	private int[] proposedShares = null;	// the values that I have proposed (multiples of 5)
	private ArrayList<Integer> proposersList = null;	// list with the proposers (excluding myself)
	private double score = 0.0;
	private Random rng = null;
	private int stepReward = 5;
	private ArrayList<int[]> receivedProposals = null;	// having the role of a proposed member
	private ArrayList<Integer> memberIndex = null;		// the index (0 or 1) of my membership of a proposal
	private int[] acceptedProposal = null;
	private int memberOfAccProp = 0;					// member (0 or 1) of the proposal I have accepted
	
	
	public Dummy(int id, int N, int mainType, int subType)
	{
		this.id = id;
		this.N = N;
		this.mainType = mainType;
		this.subType = subType;
		name += id;
		typeMatrix = new int[N][2];
		types0 = new int[N/2];
		types1 = new int[N/2];
		proposed = new int[N/6];
		proposedShares = new int[N/6];
		proposersList = new ArrayList<Integer>();
		rng = new Random();
		receivedProposals = new ArrayList<int[]>();
		memberIndex = new ArrayList<Integer>();
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setTypes(String types)
	{		
		// types contains the N main types
		String[] s1 = types.split(",");
		
		int t0Index = -1;
		int t1Index = -1;
		
		for(int i=0; i<N; i++)
		{
			if(i != id)		// don't count yourself
			{
				typeMatrix[i][0] = Integer.parseInt(s1[i]);
				
				if(typeMatrix[i][0] == 0)
				{
					t0Index++;
					types0[t0Index] = i;
				}
				else
				{
					t1Index++;
					types1[t1Index] = i;
				}
			}
		}
		
		// setting the right values of types0 and types1
		// by excluding myself from their size,
		// that is one has to be of size 5 and one of 6
		
		int[] types0Temp = new int[t0Index + 1];
		for(int i=0; i<types0Temp.length; i++)
			types0Temp[i] = types0[i];
		
		types0 = new int[t0Index + 1];	// assuring consistency
		types0 = types0Temp;
		
		int[] types1Temp = new int[t1Index + 1];
		for(int i=0; i<types1Temp.length; i++)
			types1Temp[i] = types1[i];
		
		types1 = new int[t1Index + 1];	// assuring consistency
		types1 = types1Temp;
	}
	
	public void receiveMsg(String msg)
	{
		// new tasks have been received so reset
		this.reset();
		
		System.out.println("(" + name + ") Message from server : " + msg);
		
		String[] tasks = msg.split("#");	// tasks[0] == "T"
		
		if(!tasks[0].equals("T"))
		{
			System.out.println("This message should start with T!\nExiting now...");
			return;
		}
		
		for(int i=1; i<tasks.length; i++)
		{
			String[] task = tasks[i].split(",");
			
			int tProposer = Integer.parseInt(task[0]);	// task i-1 proposer
			
			if(tProposer == id)		// I am the proposer
			{
				proposer = true;
				taskValue = Integer.parseInt(task[1]);
				nType0 = Integer.parseInt(task[2]);
				nType1 = Integer.parseInt(task[3]);
				taskType = Integer.parseInt(task[4]);
				taskId = Integer.parseInt(task[5]);
			}
			else
			{
				proposersList.add(tProposer);
				// there is more exploitable information
			}		
		}
	}
	
	public void reset()
	{
		proposer = false;	
		taskValue = 0;			
		nType0 = 0;		
		nType1 = 0;			
		taskType = -1;		
		taskId = -1;
		proposed = new int[N/6];
		proposedShares = new int[N/6];
		proposersList = new ArrayList<Integer>();
		receivedProposals = new ArrayList<int[]>();
		memberIndex = new ArrayList<Integer>();
		acceptedProposal = null;
		memberOfAccProp = 0;
	}
	
	public String makeProposal()
	{
		// exclude proposers from possible potential member
		ArrayList<Integer> types0List = new ArrayList<Integer>();
		ArrayList<Integer> types1List = new ArrayList<Integer>();
		
		for(int i=0; i<types0.length; i++)
		{
			boolean toJoin = true;
			
			for(int j=0; j<proposersList.size(); j++)
				if(types0[i] == proposersList.get(j))
					toJoin = false;
			
			if(toJoin)
				types0List.add(types0[i]);
		}
		
		for(int i=0; i<types1.length; i++)
		{
			boolean toJoin = true;
			
			for(int j=0; j<proposersList.size(); j++)
				if(types1[i] == proposersList.get(j))
					toJoin = false;
			
			if(toJoin)
				types1List.add(types1[i]);
		}
		
		
		if(taskType == 0 || taskType == 1)
		{
			// taskType = 0 or 1, so I need to find two agents 
			// of different type to propose to
			
			// choose an agent of type 0
			int rand = rng.nextInt(types0List.size());
			proposed[0] = types0List.get(rand);
			types0List.remove(rand);
			
			// define the share of proposed[0]
			// value is from [10,40], with step of 5
			rand = rng.nextInt(7) + 2;		// generate a value in [0,6] and add 2
			proposedShares[0] = rand * stepReward;	// stepReward = 5
			
			// choose an agent of type 1
			rand = rng.nextInt(types1List.size());
			proposed[1] = types1List.get(rand);
			types1List.remove(rand);
			
			// define the share of proposed[1]
			rand = rng.nextInt(7) + 2;
			proposedShares[1] = rand * stepReward;
		}
		else	// task is of type 2
		{
			// the first potential member can be of any type
			int randType = rng.nextInt(2);
			int rand = 0;
			
			if(randType == 0)
			{
				rand = rng.nextInt(types0List.size());
				proposed[0] = types0List.get(rand);
				types0List.remove(rand);
			}
			else
			{
				rand = rng.nextInt(types1List.size());
				proposed[0] = types1List.get(rand);
				types1List.remove(rand);
			}
			
			// define the share of proposed[0]
			rand = rng.nextInt(7) + 2;
			proposedShares[0] = rand * stepReward;
			
			// there can't be 3 members of the same type
			if(mainType == randType)
			{
				if(randType == 0)
				{
					// choose an agent of type 1
					rand = rng.nextInt(types1List.size());
					proposed[1] = types1List.get(rand);
					types1List.remove(rand);
				}
				else
				{
					// choose an agent of type 0
					rand = rng.nextInt(types0List.size());
					proposed[1] = types0List.get(rand);
					types0List.remove(rand);
				}
			}
			else
			{
				// choose any type
				rand = rng.nextInt(2);
				
				if(rand == 0)
				{
					// choose an agent of type 0
					rand = rng.nextInt(types0List.size());
					proposed[1] = types0List.get(rand);
					types0List.remove(rand);
				}
				else
				{
					// choose an agent of type 1
					rand = rng.nextInt(types1List.size());
					proposed[1] = types1List.get(rand);
					types1List.remove(rand);
				}
			}
			
			// define the share of proposed[1]
			rand = rng.nextInt(7) + 2;
			proposedShares[1] = rand * stepReward;
		}
		
		String s1 = proposed[0] + ",";
		s1 += proposedShares[0] + ",";
		s1 += proposed[1] + ",";
		s1 += proposedShares[1];
		
		System.out.println("(" + name + ") My proposal is " + s1);
		System.out.println("(" + name + ") Task type : " + taskType + ", my type : " + mainType + ", agent's "
				+ proposed[0] + " type : " + typeMatrix[proposed[0]][0] + ", agent's "
				+ proposed[1] + " type : " + typeMatrix[proposed[1]][0]);
		
		return s1;
	}

	public int proposalRespond(String msg)
	{
		System.out.println("(" + name + ") Message from server : " + msg);
		
		String[] proposals = msg.split("#");
		
		// proposals[0] == "C"
		if(!proposals[0].equals("C"))
			System.out.println("The message should start with C.\nExiting with error.");
		
		// check if I have not been proposed for any coalition
		// (cannot happen in server's dummies, since such a message would not have been sent)
		if(proposals.length == 1)
		{
			System.out.println("I have not been been proposed for any coalition.");
			return -2;
		}

		
		// process all of the proposals
		for(int i=1; i<proposals.length; i++)
		{
			String[] proposal = proposals[i].split(",");
			
			int member1 = Integer.parseInt(proposal[0]);
			int perc1 = Integer.parseInt(proposal[1]);
			int member2 = Integer.parseInt(proposal[2]);
			int perc2 = Integer.parseInt(proposal[3]);
			int proposer = Integer.parseInt(proposal[4]);
			int value = Integer.parseInt(proposal[5]);
			int taskId = Integer.parseInt(proposal[6]);
			
			int[] tempVec = {member1,perc1,member2,perc2,proposer,value,taskId};
			receivedProposals.add(tempVec);
			
			if(id == member1)
				memberIndex.add(0);
			else
				memberIndex.add(1);
		}

		int response = this.chooseProposal();
		
		if(response != -1)
		{
			for(int i=0; i<receivedProposals.size(); i++)
				if(receivedProposals.get(i)[6] == response)
				{
					acceptedProposal = new int[receivedProposals.size()];
					acceptedProposal = receivedProposals.get(i);
						
					System.out.println("(" + name + ") I have accepted task " + acceptedProposal[6]);
					memberOfAccProp = memberIndex.get(i);
				}
		}
		else
		{
			System.out.println("I didn't accept any of the tasks");
		}
		
		return response;
	}
	
	public int chooseProposal()
	{
		double[] expectedVal = new double[receivedProposals.size()];
		double max = -1;
		int chosenProposal = -1;
		int taskVal = 0;
		int myPerc = 0;
		int chosenPerc = 0;
		
		for(int i=0; i<receivedProposals.size(); i++)
		{
			if(memberIndex.get(i) == 0)
				myPerc = receivedProposals.get(i)[1]; 
			else
				myPerc = receivedProposals.get(i)[3];
			
			taskVal = receivedProposals.get(i)[5];
			expectedVal[i] = (double)taskVal * ((double)myPerc / 100.0);
					
			if(expectedVal[i] > max)
			{
				max = expectedVal[i];
				chosenProposal = receivedProposals.get(i)[6];
				chosenPerc = myPerc;
			}
		}
			
		double rand = rng.nextDouble();
		
		// dummy's acceptance strategy
		
		if(chosenPerc >= 30)
		{
			return chosenProposal;
		}
		else if(chosenPerc == 25)
		{
			if(rand < 0.8)
			{
				return chosenProposal;
			}
		}
		else if(chosenPerc == 20)
		{
			if(rand < 0.75)
			{
				return chosenProposal;
			}
		}
		else if(chosenPerc == 15)
		{
			if(rand < 0.6)
			{
				return chosenProposal;
			}
		}
		else if(chosenPerc == 10)
		{
			if(rand < 0.5)
			{
				return chosenProposal;
			}
		}
		else if(chosenPerc == 5)
		{
			if(rand < 0.25)
			{
				return chosenProposal;
			}
		}
		else
		{
			System.out.println("My share of proposal " + chosenProposal +" is " + chosenPerc + " (not valid).");
		}
		
		// I do not accept any proposal
		return -1;
	}
	
	public void informOfOutcome(String outcome)
	{
		System.out.println("(" + name + ") Message from server : " + outcome);
		
		String[] fields = outcome.split(",");
		
		// first letter must "F"
		if(!fields[0].equals("F"))
		{
			System.out.println("The first letter should be F, but it is " + fields[0] + ".");
			return;
		}
		
		if(fields[1].equals("-1"))		// the coalition has not been formed
		{
			if(proposer)
			{
				if(fields.length == 4)
				{
					System.out.println("(" + name + ") Both of the proposed members did not accept");		
					// process information about the other members
				}
				else
				{
					System.out.println("(" + name + ") One of the other agents did not accept.");
					// fields[2] is the agent that did not accept
					// process information about the other members
				}
			}
			else
			{
				if(acceptedProposal == null)
				{
					System.out.println("(" + name + ") I did not want any of the proposals.");
				}
				else
				{
					System.out.println("(" + name + ") The other proposed agent did not accept.");
					// process information about the other members
				}
			}
		}
		else if(fields[1].equals("0"))		// the coalition was formed but didn't succeed
		{
			System.out.println("(" + name + ") The coalition was formed but it didn't succeed.");
			// process information about the other members
		}
		else if(fields[1].equals("1"))
		{
			if(!proposer)
			{
				System.out.println("(" + name + ") The coalition has succeeded.");
				
				int myPerc = 0;
				
				if(memberOfAccProp == 0)
					myPerc = acceptedProposal[1];
				else
					myPerc = acceptedProposal[3];
				
				int myVal = acceptedProposal[5];
				
				score += (double)myVal * (double)myPerc / 100.0;
				
				// process information about the other members

			}
			else
			{
				System.out.println("(" + name + ")The coalition that I proposed has succeeded.");
				
				int myPerc = 100 - proposedShares[0] - proposedShares[1];
				
				score += (double)taskValue * (double)myPerc / 100.0;
				
				// process information about the other members

			}	
		}
		
		System.out.println("(" + name + ") My current score is " + score);
	}
	
}
