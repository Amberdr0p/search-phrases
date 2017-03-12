import com.bigdata.rdf.sail.webapp.client.IPreparedTupleQuery;
import com.bigdata.rdf.sail.webapp.client.RemoteRepository;
import com.bigdata.rdf.sail.webapp.client.RemoteRepositoryManager;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlazegraphS {
  // private static final String sparqlEndPoint = "http://winghouse.semiot.ru:3030/blazegraph";

  private static final String QUERY_SELECT_LEMMA =
      "select ?uri ?x { ?uri <http://www.custom-ontology.org/ner#lemma> ?x. "
          + "?uri <http://www.custom-ontology.org/ner#lemma> \"${LEMMA1}\". "
          + "?uri <http://www.custom-ontology.org/ner#lemma> \"${LEMMA2}\"}";
  private static final String VAR_LEMMA1 = "${LEMMA1}";
  private static final String VAR_LEMMA2 = "${LEMMA2}";
  
  private static final long maxQueryMs = 10000;

  private static final RemoteRepositoryManager repo = new RemoteRepositoryManager(ServiceConfig.CONFIG.storeUrl());
  private static final RemoteRepository rr = repo.getRepositoryForDefaultNamespace();

  public static Map<String, List<String>> selectMap(String query) {
    Map<String, List<String>> map = new HashMap<String, List<String>>();
    // long millis = System.currentTimeMillis();

    TupleQueryResult result = null;
    // result processing
    try {
      IPreparedTupleQuery iprepQuery = rr.prepareTupleQuery(query);
      iprepQuery.setMaxQueryMillis(maxQueryMs);
      result = iprepQuery.evaluate();
      while (result != null && result.hasNext()) {
        BindingSet bs = result.next();
        String label = bs.getValue("x").stringValue();
        String uri = bs.getValue("uri").stringValue();
        if (map.containsKey(uri)) {
          map.get(uri).add(label);
        } else {
          List<String> list = new ArrayList<String>();
          list.add(label);
          map.put(uri, list);
        }
        // System.out.println(bs);
      }
       //System.out.println("AllTime: " + String.valueOf(System.currentTimeMillis() - millis));
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (result != null) {
        try {
          result.close();
        } catch (QueryEvaluationException e) {
          e.printStackTrace();
        }
      }
    }

    return map;
  }



  public static void main(String[] args) throws Exception {

    final RemoteRepositoryManager repo = new RemoteRepositoryManager(ServiceConfig.CONFIG.storeUrl());
    final RemoteRepository rr = repo.getRepositoryForDefaultNamespace();

    try {
      long millis = System.currentTimeMillis();
      TupleQueryResult result = rr
          .prepareTupleQuery(
              QUERY_SELECT_LEMMA.replace(VAR_LEMMA1, "город").replace(VAR_LEMMA2, "массовый"))
          .evaluate();

      TupleQueryResult result1 = rr
          .prepareTupleQuery(
              QUERY_SELECT_LEMMA.replace(VAR_LEMMA1, "кубок").replace(VAR_LEMMA2, "конфедераций"))
          .evaluate();
      TupleQueryResult result3 = rr
          .prepareTupleQuery(
              QUERY_SELECT_LEMMA.replace(VAR_LEMMA1, "привет").replace(VAR_LEMMA2, "пока"))
          .evaluate();
      TupleQueryResult result2 = rr
          .prepareTupleQuery(
              QUERY_SELECT_LEMMA.replace(VAR_LEMMA1, "аль").replace(VAR_LEMMA2, "черро"))
          .evaluate();

      // result processing
      try {
        while (result.hasNext()) {
          BindingSet bs = result.next();
          // bs.getValue("x").
          System.out.println(bs);
        }
        while (result2.hasNext()) {
          BindingSet bs = result2.next();
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
