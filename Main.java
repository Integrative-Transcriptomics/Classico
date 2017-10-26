package datastructures;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Katrin Fischer
 *
 */
public class Main {
	public static void main(String[] args0) throws IOException{
		if(args0.length == 3) {
		SNPTable lepraSNPs = new SNPTable(args0[0]);
		NewickTree lepraTree = new NewickTree(args0[1]);
		
		System.out.println(lepraTree);
		for(Node i : lepraTree.getNodeList()){
			System.out.println(i.toString());
		}
		//System.out.println(lepraSNPs);
		lepraTree.label(lepraSNPs);
		FileWriter fw = new FileWriter(args0[2]);
		BufferedWriter bw = new BufferedWriter(fw);
		for(Node i : lepraTree.getNodeList()){
		    bw.write(i.toString());
		    bw.write("\n");
		}
		bw.close();
		System.out.println("ready");
		}else {
			System.err.println("Geben Sie als ersten Dateipfad die SNP-Tabelle an und als zweite eine Newick-Datei");
		}
	}
}
