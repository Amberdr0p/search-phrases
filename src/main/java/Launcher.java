
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.SentenceUtils;
import edu.stanford.nlp.process.DocumentPreprocessor;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import ru.stachek66.nlp.mystem.holding.Factory;
import ru.stachek66.nlp.mystem.holding.MyStem;
import ru.stachek66.nlp.mystem.holding.MyStemApplicationException;
import ru.stachek66.nlp.mystem.holding.Request;
import ru.stachek66.nlp.mystem.model.Info;
import scala.Option;
import scala.collection.JavaConversions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Launcher {

  private static final String QUERY_SELECT_LEMMA =
      "select ?uri ?x { ?uri <http://www.custom-ontology.org/ner#lemma> ?x. "
          + "?uri <http://www.custom-ontology.org/ner#lemma> \"${LEMMA1}\". "
          + "?uri <http://www.custom-ontology.org/ner#lemma> \"${LEMMA2}\"}";
  private static final String VAR_LEMMA1 = "${LEMMA1}";
  private static final String VAR_LEMMA2 = "${LEMMA2}";

  private static final MyStem mystemAnalyzer =
      new Factory("-ld --format json").newMyStem("3.0", Option.<File>empty()).get();
  private static final Option<String> nullOption = scala.Option.apply(null);

  private ExecutorService executor = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS,
      new SynchronousQueue(), new ThreadPoolExecutor.CallerRunsPolicy());

  private static List<String> readFile(String path) throws IOException {
    int count = 1000; // for test
    int i = 0;
    List<String> res = new ArrayList<String>();
    try (BufferedReader br = new BufferedReader(new FileReader(path))) {
      for (String line; (line = br.readLine()) != null && i < count; i++) {
        // process the line.
        System.out.println(line);
        res.add(line);
      }
      // line is not visible here.
    }
    return res;
  }

  private static void writeFileLarge()
      throws UnsupportedEncodingException, FileNotFoundException, IOException {
    try (Writer writer =
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream("filename.txt"), "utf-8"))) {
      List<String> list = readFile("C://Users//Ivan//Desktop//ALL.txt");
      for (String line : list) {
        writer.write(line + "\r\n");
      }
    }
  }
  
  private static void writeFile(String path, List<String> list)
      throws UnsupportedEncodingException, FileNotFoundException, IOException {
    try (Writer writer =
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "utf-8"))) {
      for (String line : list) {
        writer.write(line + "\r\n");
      }
    }
  }

  public static void main(String[] args) throws IOException {
    // writeFile();
    // readFile("C://Users//Ivan//Desktop//ALL.txt");

    RDFStore store = new RDFStore();

    long millis = System.currentTimeMillis();
    DocumentPreprocessor dp = new DocumentPreprocessor("filename.txt");
      // "C://Users//Ivan//workspace//search-phrases//test.txt", DocType.Plain, "Cp1251");

    List<String> res = new ArrayList<String>();
    for (List<HasWord> sentence : dp) {
      String sentenceStr = SentenceUtils.listToOriginalTextString(sentence);
      // System.out.println(sentenceStr);
      long millis1 = System.currentTimeMillis();

      res.add(processingSentence(sentenceStr, store));
      System.out.println("One sentence: " + String.valueOf(System.currentTimeMillis() - millis1));
    }
    writeFile("filetest.txt",res);

    System.out.println("AllTime: " + String.valueOf(System.currentTimeMillis() - millis));
  }

  private static String processingSentence(String sentence, RDFStore store) { // возвращаем 1
                                                                              // вариант,
                                                                              // соединенные
                                                                              // полностью через
                                                                              // подчеркивания
    List<Info> result;
    String str = new String(sentence);
    try {
      long millis = System.currentTimeMillis();
      result = JavaConversions
          .seqAsJavaList(mystemAnalyzer.analyze(Request.apply(sentence)).info().toSeq());
      System.out.println("mystem:" + String.valueOf(System.currentTimeMillis() - millis));
      int countRes = result.size();
      for (int i = 0; i < countRes - 1; i++) {
        Option<String> lex1 = result.get(i).lex();
        Option<String> lex2 = result.get(i + 1).lex();
        if (lex1 != null && lex1 != nullOption && lex2 != null && lex2 != nullOption) {
          // System.out.println(lex1.get() + " " + lex2.get());
          int max = 0;
          List<String> maxList = null;

          long millisKB = System.currentTimeMillis();
          Map<String, List<String>> map = getLemmaFromKB(lex1.get(), lex2.get(), store);
          System.out.println(
              "Process to Map and Query: " + String.valueOf(System.currentTimeMillis() - millisKB)); //

          for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            List<String> list = entry.getValue();
            int count = list.size();

            if (count > max) {
              int maxForList = 2;
              // System.out.println(entry.getKey() + " " + count + " " + list);

              List<String> eqList = new ArrayList<String>(list);
              eqList.remove(lex1.get());
              eqList.remove(lex2.get());

              for (int j = i + 2; j < countRes && !eqList.isEmpty(); j++) {
                Option<String> lexN = result.get(j).lex();
                if (lexN != null && lexN != nullOption && eqList.contains(lexN.get())) {
                  eqList.remove(lexN.get());
                  maxForList++;
                } else {
                  break;
                }
              }

              if ((maxForList == count && max <= maxForList)
                  || (maxForList > 2 && maxForList + 1 == count && max < maxForList)) { // или +1?
                max = maxForList;
                maxList = list;
              }
            }
          }

          if (maxList != null) {
            // System.out.println(maxList);
            List<String> list = new ArrayList<String>();
            // String s = "Result: ";
            for (int k = i; k < i + max; k++) {
              list.add(result.get(k).initial());
              /*
               * Option<String> lexN = result.get(k).lex(); if (lexN != null && lexN != nullOption)
               * { s = s + lexN.get() + " "; }
               */
            }
            // System.out.println(s);
            i += max;
            str = replace(str, list);
          }
        } else { // удалить
          System.out.print("Bad: ");
          if (lex1 != null && lex1 != nullOption)
            System.out.println(lex1.get());
          else if (lex2 != null && lex2 != nullOption)
            System.out.println(lex2.get());
        }
      }
    } catch (MyStemApplicationException e) {
      e.printStackTrace();
    }
    
    System.out.println(str);
    return str;
  }

  private static String replace(String str, List<String> list) { // ???????
    // int start = str.indexOf(list.get(0)); // первый элемент подстроки
    // int end = str.indexOf(list.get(1), start); 
    int shift = 0;
    int start;
    do {
      start = str.indexOf(list.get(0), shift); // первый элемент подстроки
      int startE = start;
      int end = 0;
      for(int i=1; i < list.size(); i++) {
        end = str.indexOf(list.get(i), startE);
        
        int sh = end - startE - list.get(i-1).length();
        if(sh < 0 && sh > 6) {
          break;
        } else if(i+1 == list.size()){
          String l = str.substring(start, end + list.get(i).length());
          
          return str.replace(l, l.replace(" ", "_"));
        }
      }
    } while(start != -1);
    return "";
  }

  private static Map<String, List<String>> getLemmaFromKB(String lemma1, String lemma2,
      RDFStore store) {

    long millis = System.currentTimeMillis();
    ResultSet res =
        store.select(QUERY_SELECT_LEMMA.replace(VAR_LEMMA1, lemma1).replace(VAR_LEMMA2, lemma2));
    System.out.println("query: " + String.valueOf(System.currentTimeMillis() - millis));

    Map<String, List<String>> map = new HashMap<String, List<String>>();
    while (res != null && res.hasNext()) {
      QuerySolution qs = res.next();
      String label = qs.getLiteral("x").getString();
      String uri = qs.getResource("uri").getURI();
      if (map.containsKey(uri)) {
        map.get(uri).add(label);
      } else {
        List<String> list = new ArrayList<String>();
        list.add(label);
        map.put(uri, list);
      }
    }

    return map;
  }

}
