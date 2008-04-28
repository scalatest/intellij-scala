package org.jetbrains.plugins.scala.lang.surroundWith.descriptors;

/**
 * User: Dmitry.Krasilschikov
 * Date: 09.01.2007
 *
 */
import com.intellij.lang.surroundWith.SurroundDescriptor;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.codeInsight.CodeInsightUtil;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes

import org.jetbrains.plugins.scala.util.DebugPrint

import org.jetbrains.plugins.scala.lang.surroundWith.surrounders.expression._
import org.jetbrains.plugins.scala.lang.psi.api.expr._

           
class ScalaExpressionSurroundDescriptor extends SurroundDescriptor {
  val PARENTHESES_SURROUDNER = 9
  val IF_SURROUNDER = 0
  val BRACES_SURROUNDER = 8
  val IF_ELSE_SURROUNDER = 1
  val FOR_SURROUNDER = 4
  val FOR_YIELD_SURROUNDER = 5
  val MATCH_SURROUNDER = 6
  val WHILE_SURROUNDER = 2
  val DO_WHILE_SURROUNDER = 3
  val TRY_SURROUNDER = 7
  val TRY_CATCH_SURROUNDER = 7
  val TRY_FINALLY_SURROUNDER = 8

  private val SURROUNDERS : Array[Surrounder] = {
    val surrounders = new Array[Surrounder](9)
    surrounders(BRACES_SURROUNDER) = new ScalaWithBracesSurrounder
    //surrounders(PARENTHESES_SURROUDNER) = new ScalaWithParnthesisesSurrounder
    surrounders(IF_SURROUNDER) = new ScalaWithIfSurrounder()
    surrounders(IF_ELSE_SURROUNDER) = new ScalaWithIfElseSurrounder()
    surrounders(FOR_SURROUNDER) = new ScalaWithForSurrounder()
    surrounders(FOR_YIELD_SURROUNDER) = new ScalaWithForYieldSurrounder()
    surrounders(WHILE_SURROUNDER) = new ScalaWithWhileSurrounder()
    surrounders(DO_WHILE_SURROUNDER) = new ScalaWithDoWhileSurrounder()
    surrounders(MATCH_SURROUNDER) = new ScalaWithMatchSurrounder()
    //surrounders(TRY_SURROUNDER) = new ScalaWithTrySurrounder()
    surrounders(TRY_CATCH_SURROUNDER) = new ScalaWithTryCatchSurrounder()
    surrounders(TRY_FINALLY_SURROUNDER) = new ScalaWithTryFinallySurrounder()
    surrounders
  }

  override def getSurrounders() : Array[Surrounder] = SURROUNDERS

  override def getElementsToSurround(file : PsiFile, startOffset : Int, endOffset : Int) : Array[PsiElement] = {
    val expr : Array[PsiElement] = findExpressionInRange(file, startOffset, endOffset)
    if (expr == null) return PsiElement.EMPTY_ARRAY
    return expr
  }

  def findExpressionInRange(file : PsiFile, startOffset : Int, endOffset : Int) : Array[PsiElement] = {

    var element1 : PsiElement = file.findElementAt(startOffset);
    var element2 : PsiElement = file.findElementAt(endOffset - 1);
    (element1,element2) match {
      case (_: PsiWhiteSpace, _)  => {
        return findExpressionInRange(file,element1.getTextRange().getEndOffset(),endOffset)
      }
      case (_, _: PsiWhiteSpace) => {
        return findExpressionInRange(file,startOffset,element2.getTextRange().getStartOffset())
      }
      case (null,_) | (_, null) => return null
      case _ => {
        if (";".equals(element2.getText()))
          return findExpressionInRange(file,startOffset,endOffset-1)
        if (element1.getNode.getElementType == ScalaTokenTypes.tLINE_TERMINATOR)
          return findExpressionInRange(file,element1.getTextRange().getEndOffset(),endOffset)
        if (element2.getNode.getElementType == ScalaTokenTypes.tLINE_TERMINATOR)
          return findExpressionInRange(file,startOffset,element2.getTextRange().getStartOffset())
      }
    }

    def findAllInRange(file : PsiFile, startOffset : Int, endOffset : Int): Array[PsiElement] = {
      var element = file.findElementAt(startOffset)
      while (element != null && !element.isInstanceOf[ScExpression] && !element.isInstanceOf[PsiWhiteSpace] &&
        element.getNode.getElementType != ScalaTokenTypes.tLINE_TERMINATOR) {
        element = element.getParent()
        if (element.getTextRange().getStartOffset() != startOffset) return null
      }
      if (element == null) return null
      val result: Array[PsiElement] = Array.apply(element)
      if (element.getTextRange().getEndOffset() < endOffset){
        val res = findAllInRange(file, element.getTextRange().getEndOffset(), endOffset)
        if (res == null) return null
        return result ++ res
      }
      else if (element.getTextRange().getEndOffset() > endOffset) {
        return null
      }
      else {
        return result
      }
    }
    val result = findAllInRange(file, startOffset, endOffset)
    if (result == null) return null
    return result
  }
}