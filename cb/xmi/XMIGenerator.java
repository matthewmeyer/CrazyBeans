package cb.xmi;

import cb.parser.*;
import cb.util.*;
import java.io.*;
import cb.petal.*;
import java.util.*;

import ru.novosoft.uml .*;
import ru.novosoft.uml.foundation.core.*;
import ru.novosoft.uml.model_management.*;
import ru.novosoft.uml.xmi.XMIWriter;
import ru.novosoft.uml.behavior.use_cases.*;
import ru.novosoft.uml.foundation.extension_mechanisms.*;
import ru.novosoft.uml.foundation.data_types.*;
import ru.novosoft.uml.behavior.collaborations.*;

/**
 * Convert a Rose petal file into the <a href="http://xml.coverpages.org/xmi.html">XMI</a>
 * format using NovoSoft's <a href="http://nsuml.sourceforge.net/">
 * NSUML</a> package which implements the entities defined in the
 * <a href="http://www.omg.org/uml/">UML</a> specification 1.3.
 *
 * @version $Id: XMIGenerator.java,v 1.8 2005/05/09 20:34:55 moroff Exp $
 * @author <A HREF="mailto:markus.dahm@berlin.de">M. Dahm</A>
 */
public class XMIGenerator extends DescendingVisitor {
  /** Where to dump the XMI file
   */
  protected String     dump;

  /** Which factory to use
   */
  protected XMIFactory factory;

  /** The Rose Petal file to convert
   */
  protected PetalFile  tree;

  /** The XMI model being set up
   */
  protected MModel     model;

  /**  Stack<MPackage>
   */
  private Stack packages = new Stack();

  /** The current package level (may be nested)
   */
  private MPackage pack;

  /** Register created objects by the quid of the petal object.
   */
  protected HashMap quid_map = new HashMap(); // Map<quid, MClassifier>

  protected HashMap package_map = new HashMap(); // Map<ClassCategory, MPackage>

  protected final void addObject(String quid, MClassifier obj) {
    quid_map.put(quid, obj);
  }

  protected void removeObject(String quid) {
    quid_map.remove(quid);
  }

  protected final MClassifier getClassifier(String quid) {
    return (MClassifier)quid_map.get(quid);
  }

  protected final MPackage getPackage(String quid) {
      for (Iterator iter = package_map.values().iterator(); iter.hasNext();) {
         java.lang.Object element = (java.lang.Object) iter.next();

         if (element instanceof MPackage) {
            MPackage modelPackage = (MPackage) element;

            if (modelPackage.getUUID().equals(quid))
               return modelPackage;
         }
      }

      return null;
   }

  
  /**
    * @param tree
    *           the Rose petal file to convert
    * @param dump
    *           where to dump the generated XMI file
    */
  public XMIGenerator(PetalFile tree, String dump) {
    this.dump = dump;
    this.tree = tree;

    factory = getFactory();
    model   = factory.createModel();
    pack    = model;
  }

  /** Start generation of XMI code.
   */
  public void start() {
    /* Run a first pass visitor to add all packages, classes, use
     * cases, etc.  that may be referenced from different places, or
     * even be referenced in a forward declaration.
     */
    tree.accept(new DescendingVisitor() {
      private void addPackage(QuidObject obj, MPackage p) {
	package_map.put(obj, p);

	pack.addOwnedElement(p);
	packages.push(pack); // Save old value
	pack = p;

	super.visitObject(obj); // Default traversal

	pack = (MPackage)packages.pop();
      }

      public void visit(LogicalCategory obj) {
	addPackage(obj, factory.createPackage(obj));
      }

      public void visit(UseCaseCategory obj) {
	addPackage(obj, factory.createPackage(obj));
      }

      public void visit(SubSystem sub) {
	/** ArgoUML can't handle subsystems
	 */
	addPackage(sub, factory.createPackage(sub));
      }

      public void visit(cb.petal.Class clazz) {
	String quid = clazz.getQuid();

	MClassifier cl = factory.createClass(clazz);
	pack.addOwnedElement(cl);
	addObject(quid, cl);
      }

      public void visit(UseCase caze) {
	String quid = caze.getQuid();

	MClassifier cl = factory.createUseCase(caze);
	pack.addOwnedElement(cl);
	addObject(quid, cl);
      }

      public void visit(Module module) {
	String quid = module.getQuid();

	MClassifier cl = factory.createComponent(module);
	pack.addOwnedElement(cl);
	addObject(quid, cl);
      }
    });

    tree.accept(this);
  }

  /** Override this if you don't like the default factory
   */
  protected XMIFactory getFactory() {
    return new XMIFactory(tree, this);
  }

  private void setPackage(QuidObject obj) {
    MPackage p = (MPackage)package_map.get(obj);

    packages.push(pack); // Save old value
    pack = p;

    visitObject(obj); // Default traversal

    pack = (MPackage)packages.pop();
  }

  public void visit(LogicalCategory obj) {
    setPackage(obj);
  }

  public void visit(UseCaseCategory obj) {
    setPackage(obj);
  }

  public void visit(SubSystem obj) {
    /** ArgoUML can't handle subsystems
     */
    setPackage(obj);
  }

  public void visit(ClassAttribute attr) {
    MAttribute  a     = factory.createAttribute(attr);
    MClassifier clazz = getContainingClassifier(attr);

    clazz.addFeature(a);
  }

  public void visit(Operation op) {
    MOperation m = factory.createOperation(op);
    MClassifier clazz = getContainingClassifier(op);
    clazz.addFeature(m);
  }

  public void visit(InheritanceRelationship rel) {
    MGeneralization gen = factory.createGeneralization(rel);
    pack.addOwnedElement(gen);
  }

  public void visit(UsesRelationship rel) {
    MUsage usage = factory.createUsage(rel);
    pack.addOwnedElement(usage);
  }

  public void visit(DependencyRelationship rel) {
    MDependency dependency = factory.createDependency(rel);
    pack.addOwnedElement(dependency);
  }

  public void visit(RealizeRelationship rel) {
    MAbstraction real = factory.createRealization(rel);
    pack.addOwnedElement(real);
  }

  /** If this association contains an association class, use that object,
   * otherwise create new object.
   */
  public void visit(Association assoc) {
    MAssociation a;

    /* ArgoUML/Poseidon can't handle AssociationClass correctly
     */
    cb.petal.Class clazz = assoc.getAssociationClass();

    if(clazz != null) {
      a = (MAssociation)getClassifier(clazz.getQuid());
      factory.setupAssociation(assoc, a);
    } else{
      a = factory.createAssociation(assoc);
      pack.addOwnedElement(a);
    }
  }

  /*************************** Utility method *******************************/

  /** Search for element of given name in model (and all sub-packages)
   * @param name name to look for with getName()
   * @param clazz Class searched element is an instance of
   * @return found element or null
   */
  public MModelElement searchElement(String name, java.lang.Class clazz) {
    return searchElement(model, name, clazz);
  }

  private static MModelElement searchElement(MPackage pack, String name,
					     java.lang.Class clazz)
  {
    for(Iterator i = pack.getOwnedElements().iterator(); i.hasNext(); ) {
      MModelElement elem = (MModelElement)i.next();

      if ( name.indexOf("::") > 0 && getQualifiedName(elem).equals(name) && clazz.isInstance(elem))
         return elem;
      else if(name.equals(elem.getName()) && clazz.isInstance(elem))
        return elem;
      else if(elem instanceof MPackage) {
        MModelElement found = searchElement((MPackage)elem, name, clazz);

        if(found != null)
	       return found;
      }
    }

    return null;
  }

  /** @return XMI object for containing class of obj
   */
  protected final MClassifier getContainingClassifier(PetalObject obj) {
    return getClassifier(((QuidObject)obj.getParent()).getQuid());
  }

  /** @return XMI object for containing class of obj
   */
  protected final MPackage getContainingPackage(PetalObject obj) {
    return getPackage(((QuidObject)obj.getParent()).getQuid());
  }

  public void dump() throws IOException, ru.novosoft.uml.xmi.IncompleteXMIException{
    XMIWriter writer = new XMIWriter(model, dump);
    writer.gen();
  }

  /** @return generated model
   */
  public MModel getModel() {
    return model;
  }

  /** @return current package
   */
  public MPackage getPackage()           { return pack; }
  
   /**
    * @return
    */
   public PetalFile getTree() {
      return tree;
   }
   
   /**
    * @param file
    */
   public void setTree(PetalFile file) {
      tree = file;
   }

   private static String getQualifiedName(MModelElement modelElement) {
      StringBuffer buffer = new StringBuffer();
      
      buffer.append(modelElement.getName());
      while ( modelElement.getNamespace() != null && !(modelElement.getNamespace() instanceof MModel) ) {
         modelElement = modelElement.getNamespace();
         buffer.insert(0, "::");
         buffer.insert(0, modelElement.getName());
      }
      return buffer.toString();
   }
}
