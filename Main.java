package datastructures;

/**
 * @author Katrin Fischer
 *
 */
public class Main {
	public static void main(String[] args0){
		SNPTable lepraSNPs = new SNPTable(args0[0]);
		NewickTree lepraTree = new NewickTree(args0[1]);
		System.out.println(lepraTree);
		for(Node i : lepraTree.getNodeList()){
			System.out.println(i.toString());
		}
		System.out.println(lepraSNPs);
	}
}
