package project;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import datastructures.NewickTree;
import datastructures.Node;
import datastructures.SNPTable;
import datastructures.NewFile;

public class Project {
	private SNPTable snp;
	private NewickTree tree;
	private NewFile file;
	private Map<Integer, HashMap<String, List<Node>>> claden = new HashMap<Integer, HashMap<String, List<Node>>>();
	private Map<Integer, HashMap<String, List<Node>>> supportTree = new HashMap<Integer, HashMap<String, List<Node>>>();
	private Map<Integer, HashMap<String, List<Node>>> notSupportTree = new HashMap<Integer, HashMap<String, List<Node>>>();
	private Map<Integer, List<Integer>> splitKeys = new HashMap<Integer, List<Integer>>();

	public Project(String snpFile, String newickTreeFile, String path) {
		snp = new SNPTable(snpFile);
		tree = new NewickTree(newickTreeFile);
		file = new NewFile(path);
		file.createDir("Ergebnis");
	}

	public Project(SNPTable snp, NewickTree tree, String path) {
		this.snp = snp;
		this.tree = tree;
		file = new NewFile(path);
		file.createDir("Ergebnis");
	}

	public static void main(String[] args0) throws IOException {
		if (args0.length == 3) {
			Project comcla = new Project(args0[0], args0[1], args0[2]);
			comcla.compute();
			/*int key = 0;
			for (Integer pos : comcla.snp.getSNPs()) {
				//key = pos;
				comcla.label(comcla.tree, pos);
				comcla.computeCladen(comcla.tree.getRoot(), pos, true);
				comcla.evaluateCladen(pos);
			}*/
			int[] showPositions = {155648};
			comcla.showPositions(showPositions);
			comcla.getResults();
			/*HashMap<String, List<Node>> hm = comcla.supportTree.get(key);
			for (String s : hm.keySet()) {
				for (Node nl : hm.get(s)) {
					for (Node nt : comcla.tree.getNodeList()) {
						if (nt.getId() == nl.getId()) {
							nt.setPosSNP("-" + key + "-" + s.substring(1, s.length() - 1));
						}
					}
				}
			}*/
			// System.out.println(comcla.tree.toString());

			System.out.println("ready");
		} else {
			System.err.println(
					"Geben Sie als ersten Dateipfad die SNP-Tabelle, als zweite eine Newick-Datei und als dritte einen Dateipfad für das Ergebnisverzeichnis an");
		}
	}

	public void compute(){
		for (Integer pos : snp.getSNPs()) {
			label(tree, pos);
			computeCladen(tree.getRoot(), pos, true);
			evaluateCladen(pos);
		}
		splitKeys(file.createFile("splitKeys.txt"));
	}
	
	public void computeCladen(Node node, int key, boolean withoutN) {

		if (node.getLabel().containsKey(key)) {
			Set<String> base = node.getLabel().get(key);
			switch (base.size()) {
			case 0:
				System.err.println("Project:86 - Key enthalten aber keine Strings");
				break;
			case 1:
				setClade(key, node, base.toString(), claden);
				break;
			case 2:
				if (withoutN && node.getLabel().get(key).contains("N")) {
					base.remove("N");
					setClade(key, node, base.toString(), claden);
					break;
				}
			default:
				if (!node.getChildren().isEmpty()) {
					for (Node i : node.getChildren()) {
						// Durchsuche Kinder nach Claden
						computeCladen(i, key, withoutN);
					}
				} else {
					// Knoten hat keine Kinder aber zwei Basen (Darf nicht
					// passieren)
					System.err.println("Project:110 - Blatt hat zwei Basen");
				}
				break;
			}
		} else {
			// TODO: Bedeutung??
			System.err.println(
					"Project:111 - Key im Knoten nicht gefunden " + key + "_" + node.getId() + "_" + node.getName());
		}
	}

	public void evaluateCladen(int pos) {
		HashMap<String, List<Node>> l = claden.get(pos);
		List<Integer> size = new ArrayList<Integer>();
		for (String s : l.keySet()) {
			size.add(l.get(s).size());
		}
		Collections.sort(size);
		// System.out.println(size.toString());
		int max = size.get(size.size() - 1);
		if (max == size.get(0)) {
			max++;
		}
		for (String s : l.keySet()) {
			List<Node> ln = l.get(s);
			if (ln.size() < max) {
				if (ln.size() == 1) {
					for (Node n : ln) {
						setClade(pos, n.getParent(), s, supportTree);
					}
				} else {
					for (Node n : ln) {
						setClade(pos, n.getParent(), s, notSupportTree);
					}
				}
			}
		}
	}

	public void undefNuc() {

	}

	public void splitKeys(String filename) {
		for (int key : supportTree.keySet()) {
			for (String s : supportTree.get(key).keySet()) {
				for (Node n : supportTree.get(key).get(s)) {
					setInt(n.getId(), key);
				}
			}
		}
		// System.out.println(splitKeys.toString());
		FileWriter fw;
		try {
			fw = new FileWriter(filename);
			BufferedWriter bw = new BufferedWriter(fw);
			for (int i : splitKeys.keySet()) {
				bw.write(i + "\t");
				Collections.sort(splitKeys.get(i));
				for (int j : splitKeys.get(i)) {
					bw.write(tree.getNode(i).getLabel().get(j).toString());
				}
				bw.write("\n");
			}
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void label(NewickTree tree, Integer key) {
		for (String sample : snp.getSampleNames()) {
			String nuc = snp.getSnp(key, sample);
			String ref = snp.getReferenceSnp(key);
			if (nuc.equals(".")) {
				nuc = ref;
			}
			for (Node current : tree.getNodeList()) {
				if (current.getName().equals(sample)) {
					current.setLabel(key, nuc);
					break;
				}
			}
		}
		List<Node> missingNodes = new ArrayList<Node>();
		for (Node n : tree.getNodeList()) {
			if (!n.getLabel().containsKey(key) && n.getChildren().isEmpty()) {
				missingNodes.add(n);
			}
		}
		if (!missingNodes.isEmpty()) {
			// TODO: Grammatik 1 oder mehr Knoten
			//System.err.println("Baum stimmt nicht mit SNPTable ueberein, Sample " + missingNodes.toString()
					//+ " sind nicht in SNP enthalten");
		}
	}

	public void setClade(int key, Node node, String label, Map<Integer, HashMap<String, List<Node>>> claden) {
		if (claden.containsKey(key)) {
			if (claden.get(key).containsKey(label)) {
				claden.get(key).get(label).add(node);
			} else {
				List<Node> split = new ArrayList<Node>();
				split.add(node);
				claden.get(key).put(label, split);
			}

		} else {
			HashMap<String, List<Node>> labeledNode = new HashMap<String, List<Node>>();
			List<Node> split = new ArrayList<Node>();
			split.add(node);
			labeledNode.put(label, split);
			claden.put(key, labeledNode);
		}
	}
	
	public void getResults(){
		toFile(file.createFile("claden.txt"), claden);
		toFile(file.createFile("supportTree.txt"), supportTree);
		toFile(file.createFile("notSupportTree.txt"), notSupportTree);
	}

	public void toFile(String filename, Map<Integer, HashMap<String, List<Node>>> claden) {
		FileWriter fw;
		try {
			fw = new FileWriter(filename);
			BufferedWriter bw = new BufferedWriter(fw);
			for (int i : claden.keySet()) {
				// System.out.println(i + ":");
				bw.write(i + ":\n");
				for (String l : claden.get(i).keySet()) {
					// System.out.println(l + ":");
					bw.write(l + ":\n");
					for (Node n : claden.get(i).get(l)) {
						/*
						 * System.out.println( n.getId() + "-" + n.getName() +
						 * "-" + n.getLabel().get(i) + ":" +
						 * n.toNewickString());
						 */
						bw.write(n.getId() + "-" + n.getName() + "-" + n.getLabel().get(i) + ":" + n.toNewickString()
								+ "\n");
					}
				}
				// System.out.println();
				bw.write("\n");
			}
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setInt(int id, int pos) {
		if (splitKeys.containsKey(id)) {
			splitKeys.get(id).add(pos);
		} else {
			List<Integer> set = new ArrayList<Integer>();
			set.add(pos);
			splitKeys.put(id, set);
		}
	}
	
	public void showPositions(int[] pos){
		for(int i: pos){
			if(snp.getSNPs().contains(i)){
				showCladeAtPosition(i);
			}else{
				System.out.println("Position " + i + " nicht in Tabelle enthalten");
			}
		}
	}

	public void showCladeAtPosition(int pos) {
		String filename = file.createFile("TreeFor" + pos+ ".nwk");
		FileWriter fw;
		try {
		fw = new FileWriter(filename);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(tree.toPositionString(pos));
		bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int getParentId(String name){
		int id=-1;
		for(Node n : tree.getNodeList()){
			if(n.getName().equals(name)){
				id= n.getParent().getId();
			}
		}
		return id;
	}
	
	public void getSplit(String name){
		int id = getParentId(name);
		if(splitKeys.containsKey(id)){
			System.out.println(name + ": " + splitKeys.get(id).toString());
		}else{
			System.out.println("Kein passender Split für Knoten " + name + " mit ID " + id + " gefunden");
		}
	}

}
