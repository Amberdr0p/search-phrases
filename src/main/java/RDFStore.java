

import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import rx.subjects.PublishSubject;


public class RDFStore {

  /*
   * private static final String TRIPLESTORE_STORE_URL =
   * "http://winghouse.semiot.ru:3030/blazegraph/sparql"; private static final String
   * TRIPLESTORE_USERNAME = "admin"; private static final String TRIPLESTORE_PASSWORD = "pw";
   */

  private final HttpAuthenticator httpAuthenticator;
  private final PublishSubject<Model> ps = PublishSubject.create();

  public RDFStore() {
    httpAuthenticator = new SimpleAuthenticator(ServiceConfig.CONFIG.storeUsername(),
        ServiceConfig.CONFIG.storePassword().toCharArray());
  }

  public void save(Model model) {
    ps.onNext(model);
  }

  public void save(String graphUri, Model model) {
    DatasetAccessorFactory.createHTTP(ServiceConfig.CONFIG.storeUrl(), httpAuthenticator)
        .add(graphUri, model);
  }

  public ResultSet select(String query) {
    return select(QueryFactory.create(query));
  }

  public ResultSet select(Query query) {
    Query select = QueryFactory.create(query);
    ResultSet rs = QueryExecutionFactory
        .createServiceRequest(ServiceConfig.CONFIG.storeUrl(), select, httpAuthenticator)
        .execSelect();
    return rs;
  }

  public void update(String update) {
    UpdateRequest updateRequest = UpdateFactory.create(update);

    UpdateExecutionFactory
        .createRemote(updateRequest, ServiceConfig.CONFIG.storeUrl(), httpAuthenticator).execute();
  }

}
