package prob.util;

public class Counter {
	private int n;
	
	public Counter(){
		this(1);
	}
	public Counter(int n){
		this.n = n;
	}
	
	public int next(){
		int rtn = n;
		n++;
		return rtn;
	}
}
