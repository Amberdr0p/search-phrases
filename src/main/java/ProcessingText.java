import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.util.List;
import java.util.Properties;

public class ProcessingText {

  private static StanfordCoreNLP pipeline;
  
  public static void init() {
    Properties props = new Properties();
    props.setProperty("annotators", "tokenize, ssplit");
    pipeline = new StanfordCoreNLP(props);
  }
  
  public static List<CoreMap> process(String text) {
    Annotation annotation = pipeline.process(text);
    return annotation.get(CoreAnnotations.SentencesAnnotation.class);
  }
  
}
