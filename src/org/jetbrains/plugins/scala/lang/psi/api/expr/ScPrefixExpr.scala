package org.jetbrains.plugins.scala
package lang
package psi
package api
package expr

import java.lang.UnsupportedOperationException
import com.intellij.psi.PsiElement

/** 
* @author Alexander Podkhalyuzin
* Date: 06.03.2008
*/

trait ScPrefixExpr extends ScExpression with MethodInvocation with ScSugarCallExpr {
  def operand = findChildrenByClassScala(classOf[ScExpression]).apply(1)

  def operation : ScReferenceExpression = findChildrenByClassScala(classOf[ScExpression]).apply(0) match {
    case re : ScReferenceExpression => re
    case _ =>
      throw new UnsupportedOperationException("Prefix Expr Operation is not reference expression: " + this.getText)
  }

  def argsElement: PsiElement = operation

  def getBaseExpr: ScExpression = operand
}