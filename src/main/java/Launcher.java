
import edu.stanford.nlp.util.CoreMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Collections;
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

  private static ExecutorService executor = new ThreadPoolExecutor(100, 100, 0L,
      TimeUnit.MILLISECONDS, new SynchronousQueue(), new ThreadPoolExecutor.CallerRunsPolicy());

  private final static Logger logger = LoggerFactory.getLogger(Launcher.class);

  public static void main(String[] args) {

    long millis = System.currentTimeMillis();
    // int window = 3000;
    try {
      ProcessingFile.readFile(ServiceConfig.CONFIG.readFileName(),
          ServiceConfig.CONFIG.readFileRow());

      List<String> listLine = ProcessingFile.nextWindow(ServiceConfig.CONFIG.windowSize());
      int cc = 0;
      while (!listLine.isEmpty()) { // BufferReader вынести надо будет
        logger.info("New window start"); // error специально для уровня логирования
        System.out.println("New window start");
        long millisWindow = System.currentTimeMillis();
        ProcessingText.init();
        Map<Integer, String> resLines = Collections.synchronizedMap(new TreeMap<Integer, String>());
        for (int i = 0; i < listLine.size(); i++) {
          processingLine(listLine.get(i), i, resLines);
        }
        executor.shutdown();
        try {
          executor.awaitTermination(45, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }


        ProcessingFile.WriteToEndFile(ServiceConfig.CONFIG.writeFileName(), resLines.values());
        logger.info("Count processing: " + String.valueOf(cc += resLines.size()));
        // System.out.println("Count processing: " + String.valueOf(cc += resLines.size()));

        executor = new ThreadPoolExecutor(100, 100, 0L, TimeUnit.MILLISECONDS,
            new SynchronousQueue(), new ThreadPoolExecutor.CallerRunsPolicy());

         System.out
         .println("Time window: " + String.valueOf(System.currentTimeMillis() - millisWindow));
        logger.info("Time window: " + String.valueOf(System.currentTimeMillis() - millisWindow));
        listLine = ProcessingFile.nextWindow(ServiceConfig.CONFIG.windowSize());
      }
      ProcessingFile.close();
      logger.info("AllTime: " + String.valueOf(System.currentTimeMillis() - millis));
    } catch (IOException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void processingLine(String line, int numLine, Map<Integer, String> resLines) {
    executor.execute(() -> {
      List<CoreMap> lcm = ProcessingText.process(line);
      String resLine = new String(line);
      for (CoreMap cm : lcm) {
        resLine = processingSentence(resLine, cm.toString());
      }
      resLines.put(numLine, resLine);
    });
  }

  private static String processingSentence(String resLine, String sentence) {
    List<Info> result;
    try {
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

          // long millisKB = System.currentTimeMillis();
          Map<String, List<String>> map = getLemmaFromKB(lex1.get(), lex2.get());

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
                  || (maxForList > 3 && maxForList + 1 == count && max < maxForList)) { // или +1?
                max = maxForList;
                maxList = list;
              }
            }
          }

          if (maxList != null) {
            List<String> list = new ArrayList<String>();
            boolean haveUpperCaseWord = false;
            for (int k = i; k < i + max; k++) {
              String initialStr = result.get(k).initial();
              if (Character.isUpperCase(initialStr.charAt(0))) { // проверка на null не нужна т.к.
                                                                 // mystem все отметается
                haveUpperCaseWord = true;
              }
              list.add(initialStr);
            }
            if (haveUpperCaseWord) {
              i += max - 1;
              resLine = replace(resLine, list);
            }
          }
        }
      }
    } catch (MyStemApplicationException e) {
      e.printStackTrace();
    }
    return resLine;
  }

  private static String replace(String str, List<String> list) {
    int start = str.indexOf(list.get(0));
    while (start != -1) {
      int startE = start;
      int end = 0;
      for (int i = 1; i < list.size(); i++) {
        end = str.indexOf(list.get(i), startE + 1);

        int sh = end - startE - list.get(i - 1).length();
        if (sh < 0 || sh > 5) {
          break;
        } else if (i + 1 == list.size()) {
          String l = str.substring(start, end + list.get(i).length());
          return str.replace(l, l.replace(" ", "_"));
        } else {
          startE = end;
        }
      }
      start = str.indexOf(list.get(0), start + 1); // первый элемент подстроки
    }
    return str;
  }

  private static Map<String, List<String>> getLemmaFromKB(String lemma1, String lemma2) {
    return BlazegraphS
        .selectMap(QUERY_SELECT_LEMMA.replace(VAR_LEMMA1, lemma1).replace(VAR_LEMMA2, lemma2));
  }

}
