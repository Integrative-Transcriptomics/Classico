package datastructures;

import java.io.File;
import java.io.IOException;

public class NewFile {
	
	private String path;
	
	public NewFile(String path){
		if(path.lastIndexOf('\\')==path.length()){
			this.path = path;
		}else{
			this.path = path + "\\";
		}
	}
	
	public void createDir(String name){
		File dir = new File(path + name);
		if(dir.mkdir()){
			System.out.println("Verzeichnis " + name + " an " + path + " erstellt");
		}
		this.path = path + name;
	}
	
	private boolean checkFile(File file) {
        if (file != null) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.err.println("Error creating " + file.toString());
            }
            if (file.isFile() && file.canWrite() && file.canRead())
                return true;
        }
        return false;
    }
	
	public String createFile(String name){
		if(this.checkFile(new File(name)))
            System.out.println(name + " erzeugt");
		return path + "\\" + name;
	} 
}
