
/**
 * @author mamakos
 *
 */
public class Task
{
	private int proposer = 0;
	private int value = 0;
	private int[] demand = null;
	private int type = 0;
	
	public Task(int proposer, int value, int[] demand, int type)
	{
		this.proposer = proposer;
		this.value = value;
		this.demand = demand;
		this.type = type;
	}
	
	public int getProposer()
	{
		return proposer;
	}
	
	public int getValue()
	{
		return value;
	}
	
	public int[] getDemand()
	{
		return demand;
	}
	
	public int getType()
	{
		return type;
	}

}
