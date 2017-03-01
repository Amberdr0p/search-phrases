import com.bigdata.rdf.sail.webapp.client.RemoteRepositoryManager;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;

public class BlazegraphS {
  private static final String sparqlEndPoint = "http://winghouse.semiot.ru:3030/blazegraph";

  public static void main(String[] args) throws Exception {

    final String QUERY_SELECT_LEMMA =
        "select ?uri ?x { ?uri <http://www.custom-ontology.org/ner#lemma> ?x. "
            + "?uri <http://www.custom-ontology.org/ner#lemma> \"город\". "
            + "?uri <http://www.custom-ontology.org/ner#lemma> \"массовый\"}";
    
    final RemoteRepositoryManager repo =
        new RemoteRepositoryManager(sparqlEndPoint /* useLBS */);
    try {
      long millis = System.currentTimeMillis();
      TupleQueryResult result = repo.getRepositoryForDefaultNamespace()
          .prepareTupleQuery(QUERY_SELECT_LEMMA).evaluate();
      // result processing
      try {
        while (result.hasNext()) {
          BindingSet bs = result.next();
          // bs.getValue("x").
          System.out.println(bs);
        }
        System.out.println("AllTime: " + String.valueOf(System.currentTimeMillis() - millis));
      } finally {
        result.close();
      }

    } finally {
      repo.close();
    }
  }

}
