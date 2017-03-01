

import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.ConfigFactory;
import org.aeonbits.owner.Mutable;

@LoadPolicy(LoadType.FIRST)
@Sources({"file:C:/Users/Ivan/git/extractor_dbpedia/config.properties"})
public interface ServiceConfig extends Mutable {

  public static final ServiceConfig CONFIG = ConfigFactory.create(ServiceConfig.class);

  @DefaultValue("admin")
  @Key("services.triplestore.username")
  String storeUsername();

  @DefaultValue("pw")
  @Key("services.triplestore.password")
  String storePassword();

  @DefaultValue("http://winghouse.semiot.ru:3030/blazegraph/sparql")
  @Key("services.triplestore.url")
  String storeUrl();
}
