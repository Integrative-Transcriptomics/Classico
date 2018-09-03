package utilities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import datastructures.NewickTree;
import datastructures.Node;
import datastructures.SNPTable;

public class SetTest {
	public static void main(String[] args0){
		Map<Integer,Set<String>> label = new HashMap<Integer, Set<String>>();
		Set<String> s = new HashSet<String>();
		s.add("A");
		label.put(73, s);
		label.get(73).add("T");
		System.out.println(label.size());
		Iterator i = label.get(73).iterator();
		while(i.hasNext()) {
			System.out.print((String) i.next());
		}
		System.out.println();
		
		
		
	}
	
}
