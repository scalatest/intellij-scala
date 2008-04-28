package org.jetbrains.plugins.scala.lang.surroundWith.surrounders.expression

/**
 * @author: Dmitry Krasilschikov
 */

import com.intellij.psi.PsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange


import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElementImpl
import lang.psi.api.expr._

/*
 * Surrounds expression with do - while: do { Expression } while { <Cursor> }
 */

class ScalaWithDoWhileSurrounder extends ScalaExpressionSurrounder {
  override def getExpressionTemplateAsString (expr : ASTNode) = {
    val exprAsString = "do { \n " + expr.getText + "\n" + "} while (true)"

    if (!isNeedBraces(expr)) exprAsString
    else "(" + exprAsString + ")"
  }

  override def getTemplateAsString(elements: Array[PsiElement]): String = {
    return "do {" + super.getTemplateAsString(elements) + "} while (true)"
  }

  override def getTemplateDescription = "do / while"

  override def getSurroundSelectionRange (withDoWhileNode : ASTNode ) : TextRange = {
    def isDoWhileStmt = (e : PsiElement) => e.isInstanceOf[ScDoStmt]

    val doWhileStmt = withDoWhileNode.getPsi.asInstanceOf[ScDoStmt]

    val conditionNode : ASTNode = doWhileStmt.getNode.getLastChildNode.getTreePrev

    val startOffset = conditionNode.getTextRange.getStartOffset
    val endOffset = conditionNode.getTextRange.getEndOffset

    return new TextRange(startOffset, endOffset);
  }
}