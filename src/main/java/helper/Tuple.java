package helper;

public class Tuple<A, B>
{
	private A objA= null;
	private B objB = null;
	
	public Tuple(A objA, B objB)
	{
		this.setObjA(objA);
		this.setObjB(objB);
	}
	
	public A getObjA()
	{
		return objA;
	}
	
	public B getObjB()
	{
		return objB;
	}
	
	public void setObjA(A objA)
	{
		this.objA = objA;
	}
	
	public void setObjB(B objB)
	{
		this.objB = objB;
	}
	
}