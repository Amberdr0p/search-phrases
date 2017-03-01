
import edu.stanford.nlp.util.CoreMap;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import ru.stachek66.nlp.mystem.holding.Factory;
import ru.stachek66.nlp.mystem.holding.MyStem;
import ru.stachek66.nlp.mystem.holding.MyStemApplicationException;
import ru.stachek66.nlp.mystem.holding.Request;
import ru.stachek66.nlp.mystem.model.Info;
import scala.Option;
import scala.collection.JavaConversions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

  private static ExecutorService executor = new ThreadPoolExecutor(30, 30, 0L,
      TimeUnit.MILLISECONDS, new SynchronousQueue(), new ThreadPoolExecutor.CallerRunsPolicy());



  public static void main(String[] args) throws IOException, InterruptedException {
    RDFStore store = new RDFStore();
    long millis = System.currentTimeMillis();
    int window = 100;
    ProcessingFile.readFile("filename.txt");
    List<String> listLine = ProcessingFile.nextWindow(window);
    int cc = 0;
    while (!listLine.isEmpty()) { // BufferReader вынести надо будет
      long millisWindow = System.currentTimeMillis();
      ProcessingText.init();
      Map<Integer, String> resLines = new TreeMap<Integer, String>();
      for (int i = 0; i < listLine.size(); i++) {
        processingLine(listLine.get(i), i, store, resLines);
      }

      while (true) {
        if (resLines.size() == listLine.size()) {
          ProcessingFile.writeFile("filetest.txt", resLines.values());
          System.out.println("Count processing: "+ String.valueOf(cc += window));
          break;
        } else {
          Thread.sleep(300);
        }
      }
      System.out.println("Time window: " + String.valueOf(System.currentTimeMillis() - millisWindow));
      listLine = ProcessingFile.nextWindow(window);
    }

    /*
     * List<String> res = new ArrayList<String>(); for (List<HasWord> sentence : dp) { String
     * sentenceStr = SentenceUtils.listToOriginalTextString(sentence); //
     * System.out.println(sentenceStr); long millis1 = System.currentTimeMillis();
     * 
     * res.add(processingSentence(sentenceStr, store)); System.out.println("One sentence: " +
     * String.valueOf(System.currentTimeMillis() - millis1)); } writeFile("filetest.txt",res);
     * 
     * 
     */
    System.out.println("AllTime: " + String.valueOf(System.currentTimeMillis() - millis));
  } 

  private static void processingLine(String line, int numLine, RDFStore store,
      Map<Integer, String> resLines) {
    executor.execute(() -> {
      List<CoreMap> lcm = ProcessingText.process(line);
      String resLine = new String(line);
      for (CoreMap cm : lcm) {
        resLine = processingSentence(resLine, cm.toString(), store);
      }
      resLines.put(numLine, resLine);
    });
  }

  private static String processingSentence(String resLine, String sentence, RDFStore store) {
    List<Info> result;
    try {
      long millis = System.currentTimeMillis();
      result = JavaConversions
          .seqAsJavaList(mystemAnalyzer.analyze(Request.apply(sentence)).info().toSeq());
      // System.out.println("mystem:" + String.valueOf(System.currentTimeMillis() - millis));
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
          // System.out.println("Process to Map and Query: " + String.valueOf(System.currentTimeMillis() - millisKB)); //

          for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            List<String> list = entry.getValue();
            int count = list.size();

            if (count > max) {
              int maxForList = 2;
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
            i += max - 1;
            resLine = replace(resLine, list);
          }
        } else { // удалить
          /* System.out.print("Bad: ");
          if (lex1 != null && lex1 != nullOption)
            System.out.println(lex1.get());
          else if (lex2 != null && lex2 != nullOption)
            System.out.println(lex2.get()); */
        }
      }
    } catch (MyStemApplicationException e) {
      e.printStackTrace();
    }
    return resLine;
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
      for (int i = 1; i < list.size(); i++) {
        end = str.indexOf(list.get(i), startE);

        int sh = end - startE - list.get(i - 1).length();
        if (sh < 0 && sh > 6) {
          break;
        } else if (i + 1 == list.size()) {
          String l = str.substring(start, end + list.get(i).length());

          return str.replace(l, l.replace(" ", "_"));
        }
      }
    } while (start != -1);
    return "";
  }

  private static Map<String, List<String>> getLemmaFromKB(String lemma1, String lemma2,
      RDFStore store) {

    long millis = System.currentTimeMillis();
    ResultSet res =
        store.select(QUERY_SELECT_LEMMA.replace(VAR_LEMMA1, lemma1).replace(VAR_LEMMA2, lemma2));
    //System.out.println("query: " + String.valueOf(System.currentTimeMillis() - millis));

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
