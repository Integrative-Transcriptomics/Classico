package datastructures;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Datenstruktur eines Newick-Trees
 * @author Katrin Fischer
 *
 */
public class NewickTree {

	private String inputFile;

	private Node root;
	private List<Node> nodeList = new ArrayList<Node>();

	/**
	 * Konstruktor eines NewickTrees anhand einer Newick-Datei
	 * @param inputFile Dateipfad der Newick-Datei
	 */
	public NewickTree(String inputFile) {
		this.inputFile = inputFile;
		readNewickTree();
	}

	private void readNewickTree() {
		try {
			@SuppressWarnings("resource")
			BufferedReader br = new BufferedReader(new FileReader(this.inputFile));
			String line = "";
			String newickString = "";
			while ((line = br.readLine()) != null) {
				newickString += line;
			}
			System.out.println(newickString);

			this.root = readSubtree(newickString.substring(0, newickString.length() - 1));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Node readSubtree(String s) {

		int leftParen = s.indexOf('(');
		int rightParen = s.lastIndexOf(')');
		int colon = s.lastIndexOf(':');

		if (leftParen != -1 && rightParen != -1) {
			String name;
			double length = 0;
			if(colon!= -1 && colon > rightParen){
				name = s.substring(rightParen + 1, colon);
				length = Double.parseDouble(s.substring(colon+1));
			}else{
				name = s.substring(rightParen + 1);
			}
			String[] childrenString = split(s.substring(leftParen + 1, rightParen));
			
			Node node = new Node(formatName(name) , length);
			for (String sub : childrenString) {
				Node child = readSubtree(sub);
				node.addChild(child);
				child.setParent(node);
			}

			node.setId(nodeList.size()+1);
			nodeList.add(node);
			return node;
		} else if (leftParen == rightParen) {
			
			String name = s.substring(0, colon);
			double length = Double.parseDouble(s.substring(colon+1));
			Node node = new Node(formatName(name) , length);
			node.setId(nodeList.size()+1);
			nodeList.add(node);
			return node;

		} else
			throw new RuntimeException("unbalanced brackets");
	}

	private static String[] split(String s) {

		ArrayList<Integer> splitIndices = new ArrayList<>();

		int rightParenCount = 0;
		int leftParenCount = 0;
		for (int i = 0; i < s.length(); i++) {
			switch (s.charAt(i)) {
			case '(':
				leftParenCount++;
				break;
			case ')':
				rightParenCount++;
				break;
			case ',':
				if (leftParenCount == rightParenCount)
					splitIndices.add(i);
				break;
			}
		}

		int numSplits = splitIndices.size() + 1;
		String[] splits = new String[numSplits];

		if (numSplits == 1) {
			splits[0] = s;
		} else {

			splits[0] = s.substring(0, splitIndices.get(0));

			for (int i = 1; i < splitIndices.size(); i++) {
				splits[i] = s.substring(splitIndices.get(i - 1) + 1, splitIndices.get(i));
			}

			splits[numSplits - 1] = s.substring(splitIndices.get(splitIndices.size() - 1) + 1);
		}

		return splits;
	}

	// Ausgabe des Baumes in der Konsole
	public String toString() {
		if (root != null) {
			String result = root.toNewickString();
			return result.substring(0, result.lastIndexOf(':')) + ";";
		} else {
			// Ausgabe bei leerem Baum
			return "Leerer Baum kann nicht ausgegeben werden";
		}
	}

	public List<Node> getNodeList() {
		return nodeList;
	}
	
	public void label(SNPTable snp) {
		for (Integer pos : snp.getSNPs()) {
			for (String sample : snp.getSampleNames()) {
				String nuc = snp.getSnp(pos, sample);
				String ref = snp.getReferenceSnp(pos);
				if(nuc.equals(".")) {
					nuc = ref;
				}
				for(Node current : nodeList) {
					if(current.getName().equals(sample)) {
						current.setLabel(pos, nuc);
						break;
					}
				}
			}
		}
	}
	
	public Node getRoot() {
		return root;
	}

	public String formatName(String name) {
		StringBuilder sb = new StringBuilder(name);
		int index = sb.indexOf("_");
		while(index != -1) {
			sb.replace(index, index+1, " ");
			index = sb.indexOf("_");
		}
		return sb.toString();
	}
	
	

}
