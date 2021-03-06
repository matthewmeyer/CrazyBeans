package cb.petal;
import java.util.Collection;

/**
 * Represents Partition object
 *
 * @version $Id: Partition.java,v 1.1 2001/11/20 11:24:29 dahm Exp $
 * @author  <A HREF="mailto:markus.dahm@berlin.de">M. Dahm</A>
 */
public class Partition extends QuidObject implements Named {
  public Partition(PetalNode parent, Collection params) {
    super(parent, "Partition", params);
  }

  public Partition() {
    super("Partition");
  }

  public void setNameParameter(String o) {
    params.set(0, o);
  }

  public String getNameParameter() {
    return (String)params.get(0);
  }

  public void accept(Visitor v) {
    v.visit(this);
  }
}
