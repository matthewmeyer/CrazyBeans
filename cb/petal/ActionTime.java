package cb.petal;
import java.util.Collection;

/**
 * Represents ActionTime object
 *
 * @version $Id: ActionTime.java,v 1.7 2001/06/22 09:10:36 dahm Exp $
 * @author  <A HREF="mailto:markus.dahm@berlin.de">M. Dahm</A>
 */
public class ActionTime extends PetalObject {
  public ActionTime(PetalNode parent, Collection params) {
    super(parent, "ActionTime", params);
  }

  public ActionTime() {
    super("ActionTime");
  }

  public String getWhen() {
    return getPropertyAsString("when");
  }

  public void setWhen(String o) {
    defineProperty("when", o);
  }

  public void accept(Visitor v) {
    v.visit(this);
  }
}
