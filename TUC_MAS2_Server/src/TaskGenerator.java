import java.util.Random;


/**
 * @author mamakos
 *
 */
public class TaskGenerator
{
	private int N = 0;			// the number of the agents
	private int nTypes = 0;		// the number of main types
	private int nTasks = 0;		// the number of the tasks produced per iteration
	private int nTaskTypes = 0;	// the number of task types
	private int[][] taskTypes = null;
	private int[] values = null;	// the value of each of the possible tasks
	private Random rng = null;
	private int[] ids = null;
	private int[][] typeMatrix = null;
	
	public TaskGenerator(int N, int nTypes, int[] ids, int[][] typeMatrix)
	{
		this.N = N;
		this.nTypes = nTypes;
		this.ids = ids;
		this.typeMatrix = typeMatrix;
		
		// define number of tasks per iteration
		nTasks = 4;
		
		// define the number of task types
		nTaskTypes = 3;
				
		// create the task types
		// the first requiring 2 "carpenters" and 1 "electrician"
		// the second requiring 2 "electricians" and 1 "plumber"
		// the third requiring 1 "carpenter", 1 "electrician" and 1 of any type
		
		taskTypes = new int[nTaskTypes][nTypes];
		
		for(int i=0; i<nTaskTypes; i++)
			for(int j=0; j<nTypes; j++)
				taskTypes[i][j] = 1;
		
		for(int i=0; i<nTypes; i++)
			taskTypes[i][i]++;
		
		// For N = 12 and nTypes = 2 :
		//			|2|1|
		//	tasks=	|1|2|
		//			|1|1|
		//
		// in the last case the third member can be of any type,
		// therefore anyone can be the proposer
		
		// define values of tasks
		values = new int[nTasks];
		
		values[0] = 10;	// value of the task that 2 agents of type 0 are needed
		values[1] = 10;	// value of the task that 2 agents of type 1 are needed
		values[2] = 7;	// value of the task that the third agent can be of any type
		
		rng = new Random();
	}
	
	public int getNTasks()
	{
		return nTasks;
	}
	
	public Task[] generateTasks()
	{		
		Task[] tasks = new Task[nTasks];
		
		boolean[] occupied = new boolean[N];
		
		for(int i=0; i<N; i++)
			occupied[i] = false;
		
		for(int i=0; i<nTasks; i++)
		{
			int proposer = 0;
			int value = 0;
			int[] demand = new int[2];
			int type = 0;
			
			double val = rng.nextDouble();
			double sum = 0.0;
			
			// generate the task type	
			for(int j=0; j<nTaskTypes; j++)
			{
				sum++;
				
				if(val < sum/nTaskTypes)
				{
					value = values[j];
					demand = taskTypes[j];
					type = j;
					break;
				}
			}
			
			// generate the proposer
			if(type != nTypes)			// task of type 0 or 1
			{
				double total = 0.0;
				
				for(int j=0; j<N; j++)
					if(typeMatrix[j][0] == type && !occupied[j])
						total++;
				
				val = rng.nextDouble();
				double sum1 = 0.0;
				
				for(int j=0; j<N; j++)
				{
					if(typeMatrix[j][0] == type && !occupied[j])
					{
						sum1++;
						if(val < sum1/total)
						{
							proposer = ids[j];
							occupied[j] = true;
							break;
						}
					}
				}
			}
			else	// any type can be the proposer
			{
				double total = 0.0;
				
				for(int j=0; j<N; j++)
					if(!occupied[j])
						total++;
				
				val = rng.nextDouble();
				double sum1 = 0.0;
				
				for(int j=0; j<N; j++)
				{
					if(!occupied[j])
					{
						sum1++;
						if(val < sum1/total)
						{
							proposer = ids[j];
							occupied[j] = true;
							break;
						}
					}
				}
			}
			
			tasks[i] = new Task(proposer, value, demand, type);
		}
		
		return tasks;
	}
	
	public void setIds(int[] ids)
	{
		this.ids = ids;
	}
	
	public void setTypeMatrix(int[][] typeMatrix)
	{
		this.typeMatrix = typeMatrix;
	}

}
