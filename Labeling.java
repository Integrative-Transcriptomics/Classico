package Project;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import datastructures.NewickTree;
import datastructures.Node;
import datastructures.SNPTable;

public class Project {
	private SNPTable snp;
	private NewickTree tree;
	private Map<Integer,List<Node>> claden = new HashMap<Integer, List<Node>>();
	
	public Project(String snpFile, String newickTreeFile) {
		snp = new SNPTable(snpFile);
		tree = new NewickTree(newickTreeFile);
		this.tree.label(snp);
	}
	
	public Project(SNPTable snp, NewickTree tree) {
		this.snp = snp;
		this.tree = tree;
		this.tree.label(snp);
	}
	
	public void computeCladen() {
		
		tree.getRoot().getLabel().keySet();
	}
	
	

	

}
