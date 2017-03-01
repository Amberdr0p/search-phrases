import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ProcessingFile {
  private static BufferedReader br;

  public static void readFile(String path) {
    try {
      br = new BufferedReader(new FileReader(path));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public static List<String> nextWindow(int count) throws IOException {
    int i = 0;
    List<String> res = new ArrayList<String>();
    for (String line; (line = br.readLine()) != null && i < count; i++) {
      res.add(line);
    }
    return res;
  }

  public static void writeFile(String path, Collection<String> list)
      throws UnsupportedEncodingException, FileNotFoundException, IOException {
    try (Writer writer =
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "utf-8"))) {
      for (String line : list) {
        writer.write(line + "\r\n");
      }
    }
  }

  private static void writeFileLarge()
      throws UnsupportedEncodingException, FileNotFoundException, IOException {
    try (Writer writer =
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream("filename.txt"), "utf-8"))) {
      List<String> list = readFileLarge("ALL.txt", 600000); // random count
      for (String line : list) {
        writer.write(line + "\r\n");
      }
    }
  }
  
  public static List<String> readFileLarge(String path, int count) throws IOException {
    int i = 0;
    List<String> res = new ArrayList<String>();
    try (BufferedReader br = new BufferedReader(new FileReader(path))) {
      for (String line; (line = br.readLine()) != null && i < count; i++) {
        // process the line.
        // System.out.println(line);
        res.add(line);
      }
    }
    return res;
  }

  
  public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException, IOException {
    writeFileLarge();
  }
}
