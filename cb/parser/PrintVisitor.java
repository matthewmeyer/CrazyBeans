package cb.parser;
import java.util.Iterator;
import java.io.*;
import java.util.Collection;
import java.util.Stack;
import cb.util.*;
import cb.petal.*;

/**
 * Print petal file exactly like Rose would with some limitations concerning
 * indendattion, i.e., if you don't mind white space, input and output files are
 * identical.
 *
 * @version $Id: PrintVisitor.java,v 1.18 2001/11/01 15:56:49 dahm Exp $
 * @author  <A HREF="mailto:markus.dahm@berlin.de">M. Dahm</A>
 */
public class PrintVisitor extends DescendingVisitor {
  private PrintStream out;
  private int level = 0;
  private int column = 0, row = 1;
  private Stack align_stack = new Stack();

  public PrintVisitor(PrintStream out) {
    this.out = out;
  }

  public void visit(PetalFile obj) {
    println();
    obj.getPetal().accept(this);
    println();
    println();
    obj.getDesign().accept(this);
    println();
  }

  public void visitObject(PetalObject obj) {
    StringBuffer buf = new StringBuffer("(object " + obj.getName());

    for(Iterator i = obj.getParameterList().iterator(); i.hasNext(); )
      buf.append(" \"" + i.next() + "\"");

    if(obj instanceof Tagged) {
      int label = ((Tagged)obj).getTag();
      
      if(label > 0)
	buf.append(" @" + label);
    }

    print(buf);

    if(obj.getNames().size() > 0)
      println();

    level++;
    setAlignment(obj.getLongestName().length());

    for(Iterator i = obj.getNames().iterator(), j = obj.getPropertyList().iterator();
	i.hasNext(); )
    {
      indent();
      print(i.next());
      align();

      ((PetalNode)j.next()).accept(this);

      if(i.hasNext())
	println();
    }

    print(")");

    level--;
    restoreAlignment();
  }

  /* Property names are aligned to a 4 column boundary
   * Property values are aligned to a 8 column boundary
   * 8 spaces are replaced by one tab.
   */

  private int align_at=-1;

  private void setAlignment(int max) {
    align_stack.push(new Integer(align_at));

    int indent = level * 4;
    int min_dist; // min distance between first char of name and first char of property

    if(indent % 8 == 0) // name aligned on 8
      min_dist = 16;
    else                // name aligned on 4
      min_dist = 12;

    align_at = indent + min_dist;
  }

  private void restoreAlignment() {
    Integer i = (Integer) align_stack.pop();
    align_at = i.intValue();
  }

  /** Insert spaces up to next 8 column boundary
   */
  private void align() {
    int fill;

    if(column < align_at) {
      fill = align_at - column;
      //System.err.println("< : " + align_at + ":" + column + ":" + fill);
    }  else {
      fill = 8 - (column % 8);
      //System.err.println(">= : " + align_at + ":" + column + ":" + fill);
    }

    int spaces = 4 - (fill % 4);
    for(int i=0; i < spaces; i++)
      out.print(' ');
    
    column += spaces;
    fill   -= spaces;

    if(fill > 0) {
      int tabs   = fill / 8;

      if(fill % 8 > 0)
	tabs++;
    
      for(int i=0; i < tabs; i++)
      out.print('\t');
    }

    //for(int i=0; i < fill; i++)
    //out.print(' ');

    column += fill;
  }

  /** Initial indentation of line depend on current nesting level to 4 column boundary.
   */
  private void indent() {
    int tabs   = level / 2; // 4 spaces (INDENT) == 1 tab
    int spaces = level % 2;

    for(int i=0; i < tabs; i++)
      out.print('\t');

    for(int i=0; i < spaces; i++)
      out.print("    ");

    column += level * 4;
  }

  private void println() {
    out.print("\r\n");
    row++;
    column = 0;
  }

  private void print(java.lang.Object o) {
    String s = o.toString();
    column += s.length();

    out.print(s);
  }

  public void visit(StringLiteral obj) {
    if(obj.isMultiLine()) {
      println();

      Collection c = obj.getLines();

      for(Iterator i = c.iterator(); i.hasNext(); ) {
	print("|" + i.next());
	println();
      }

      indent();
    } else
      print(obj);
  }

  public void visit(BooleanLiteral obj) {
    print(obj);
  }

  public void visit(FloatLiteral obj) {
    print(obj);
  }

  public void visit(IntegerLiteral obj) {
    print(obj);
  }

  public void visit(Tag ref) {
    print(ref);
  }

  public void visit(Location loc) {
    print(loc);
  }

  public void visit(List list) {
    print("(list ");

    if(list.getName() != null)
      print(list.getName());

    java.util.List c = list.getElements();

    if(c.size() > 0) {
      level++;

      for(Iterator i = c.iterator(); i.hasNext(); ) {
	println();
	indent();
	((PetalNode)i.next()).accept(this);
      }

      level--;
    }

    print(")");
  }

  public void visit(Value value) {
    print("(value " + value.getValueName());

    StringLiteral val = value.getValue();

    if(val.isMultiLine()) {
      val.accept(this);
      
      print(")");
    } else
      print(" " + val + ")");
  }

  public void visit(Tuple tuple) {
    print(tuple);
  }

  public static void main(String[] args) {
    PetalFile tree = PetalParser.parse(args);
    tree.accept(new PrintVisitor(System.out));
  }
}
