package ssExtraction;

import java.util.Locale;

public class NearestVertex 
{
	public int id;
	public double reachabilityDistance;  // Reachability distance value for which the label was reached
	public int idLabeledObject; // Labeled object which reached the unlabeled object.
	
	
	
	public NearestVertex(int id, double rd, int idLabeled)
	{
		this.id = id;
		this.reachabilityDistance = rd;
		this.idLabeledObject = idLabeled;
	}
	
	public String toString() 
	{
		String str = "<" + this.id + "; " + String.format(Locale.CANADA, "%.2f", this.reachabilityDistance) + "; " + this.idLabeledObject + ">";
		return str;
	}
}