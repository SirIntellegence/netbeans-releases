/* Generated By:JJTree: Do not edit this line. ASTString.java */

package org.netbeans.performance.bde.generated;

public class ASTString extends SimpleNode {
  public ASTString(int id) {
    super(id);
  }

  public ASTString(BDEParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(BDEParserVisitor visitor, Object data) throws Exception {
    return visitor.visit(this, data);
  }
}
