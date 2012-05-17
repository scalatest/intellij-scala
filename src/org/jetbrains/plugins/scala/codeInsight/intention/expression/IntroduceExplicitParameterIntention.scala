package org.jetbrains.plugins.scala
package codeInsight.intention.expression

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Editor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.util.TextRange
import lang.psi.api.expr._
import com.intellij.psi.{PsiDocumentManager, PsiElement}
import lang.psi.impl.ScalaPsiElementFactory
import extensions._
import lang.refactoring.namesSuggester.NameSuggester
import com.intellij.codeInsight.CodeInsightUtilBase
import com.intellij.refactoring.rename.inplace.MyLookupExpression
import lang.psi.api.statements.params.ScParameter
import lang.refactoring.util.ScalaVariableValidator
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.openapi.editor.markup.{TextAttributes, RangeHighlighter}
import com.intellij.openapi.editor.colors.{EditorColors, EditorColorsManager}
import com.intellij.codeInsight.template._
import impl.{TemplateManagerImpl, TemplateState}
import scala.{None, Option}
import collection.mutable.{ArrayBuffer, HashSet, HashMap}
import org.jetbrains.plugins.scala.lang.psi.types.result.TypingContext
import com.intellij.openapi.extensions.Extensions

/**
 * @author Ksenia.Sautina
 * @since 4/13/12
 */

object IntroduceExplicitParameterIntention {
  def familyName = "Introduce explicit parameter"
}

class IntroduceExplicitParameterIntention extends PsiElementBaseIntentionAction {
  def getFamilyName = IntroduceExplicitParameterIntention.familyName

  override def getText: String = getFamilyName

  def isAvailable(project: Project, editor: Editor, _element: PsiElement): Boolean = {
    findExpression(_element, editor) match {
      case Some(x) => true
      case None => false
    }
  }

  override def invoke(project: Project, editor: Editor, element: PsiElement) {
    val expr = findExpression(element, editor).get
    if (expr == null || !expr.isValid) return

    val buf = new StringBuilder
    val parentStartOffset = expr.getTextRange.getStartOffset
    val parentEndOffset = expr.getTextRange.getEndOffset

    val underscores = ScUnderScoreSectionUtil.underscores(expr)
    val underscoreToParam: HashMap[ScUnderscoreSection, ScParameter] = new HashMap[ScUnderscoreSection, ScParameter]
    val offsets: HashMap[String, Int] = new HashMap[String, Int]
    val usedNames: HashSet[String] = new HashSet[String]
    val macros: HashSet[String] = new HashSet[String]
    var needComma = false
    var needBraces = false

    for (m <- Extensions.getExtensions(Macro.EP_NAME)) {
      macros.add(m.getName)
    }

    for (u <- underscores) {
      if (needComma) buf.append(",")
      if (underscores.size > 1) needComma = true

      val names = NameSuggester.suggestNames(u,
        new ScalaVariableValidator(null, project, u, false, expr.getContext, expr.getContext) {
          override def validateName(name: String, increaseNumber: Boolean): String = {
            var res = super.validateName(name, increaseNumber)
            var index = 1

            if (usedNames.contains(res)) {
              val indexStr = res.replaceAll(name, "")
              if (indexStr != "") index = Integer.valueOf(indexStr)

              while (usedNames.contains(name + index)) {
                index = index + 1
              }
            } else {
              return res
            }
            res = name + index
            res
          }
        })

      var un = names(0)
      if (macros.contains(un)) {
        if (names.size > 1) {
          un = names(1)
        } else {
          un = "value"
        }
      }

      usedNames.add(un)
      buf.append(un)

      if (u.getParent.isInstanceOf[ScTypedStmt]) {
        needBraces = true
        val typedStmt = u.getParent.asInstanceOf[ScTypedStmt]
        buf.append(": ").append(typedStmt.getType(TypingContext.empty).get.canonicalText)
      }

      val newParam = ScalaPsiElementFactory.createParameterFromText(un, element.getManager)
      underscoreToParam.put(u, newParam)
    }

    inWriteAction {
      for (u <- underscores) {
        val param = underscoreToParam.get(u)
        u.replace(param.get)
        offsets.put(param.get.name, param.get.getTextRange.getStartOffset)
      }
    }

    if (underscores.size > 1 || needBraces) buf.insert(0, "(").append(")")
    buf.append(" => ")
    val diff = buf.length
    buf.append(expr.getText)

    val newExpr = ScalaPsiElementFactory.createExpressionFromText(buf.toString(), element.getManager)

    inWriteAction {
      val document = editor.getDocument

      expr.replace(newExpr)
      PsiDocumentManager.getInstance(project).commitDocument(document)

      val file = PsiDocumentManager.getInstance(project).getPsiFile(document)
      val parent = PsiTreeUtil.findCommonParent(file.findElementAt(parentStartOffset), file.findElementAt(parentEndOffset - 1))

      val builder: TemplateBuilderImpl = TemplateBuilderFactory.getInstance().
              createTemplateBuilder(parent).asInstanceOf[TemplateBuilderImpl]
      val params = new HashMap[Int, String]()
      val depends = new HashMap[Int, String]()

      var index: Int = 1
      parent match {
        case f: ScFunctionExpr =>
          for (parameter <- f.parameters) {
            val lookupExpr = new MyLookupExpression(parameter.name, null, parameter, false, null)
            builder.replaceElement(parameter.nameId, parameter.name, lookupExpr, true)

            val dependantParam = file.findElementAt(offsets(parameter.name) + diff)
            builder.replaceElement(dependantParam, parameter.name + "_1", parameter.name, false)

            params.put(index, parameter.name)
            depends.put(index, parameter.name + "_1")
            index = index + 1
          }

        case _ =>
      }

      CodeInsightUtilBase.forcePsiPostprocessAndRestoreElement(parent)

      editor.getCaretModel.moveToOffset(parent.getTextRange.getStartOffset)
      val template = builder.buildInlineTemplate()
      val myHighlighters = new ArrayBuffer[RangeHighlighter]
      val rangesToHighlight: HashMap[TextRange, TextAttributes] = new HashMap[TextRange, TextAttributes]

      TemplateManager.getInstance(project).startTemplate(editor, template, new TemplateEditingAdapter {
        override def waitingForInput(template: Template) {
          markCurrentVariables(1)
        }

        override def currentVariableChanged(templateState: TemplateState, template: Template,
                                            oldIndex: Int, newIndex: Int) {
          if (oldIndex >= 0) clearHighlighters()
          if (newIndex > 0) markCurrentVariables(newIndex + 1)
        }

        override def templateCancelled(template: Template) {
          clearHighlighters()
        }

        override def templateFinished(template: Template, brokenOff: Boolean) {
          clearHighlighters()
        }

        private def addHighlights(ranges: HashMap[TextRange, TextAttributes], editor: Editor,
                                  highlighters: ArrayBuffer[RangeHighlighter], highlightManager: HighlightManager) {
          for ((range, attributes) <- ranges) {
            import scala.collection.JavaConversions._
            highlightManager.addOccurrenceHighlight(editor, range.getStartOffset, range.getEndOffset,
              attributes, 0, highlighters, null)
          }
          for (highlighter <- highlighters) {
            highlighter.setGreedyToLeft(true)
            highlighter.setGreedyToRight(true)
          }
        }

        private def markCurrentVariables(index: Int) {
          val colorsManager: EditorColorsManager = EditorColorsManager.getInstance
          val templateState: TemplateState = TemplateManagerImpl.getTemplateState(editor)
          var i: Int = 0

          while (i < templateState.getSegmentsCount) {
            val segmentOffset: TextRange = templateState.getSegmentRange(i)
            val name: String = template.getSegmentName(i)
            var attributes: TextAttributes = null
            if (name == params.get(index).get) {
              attributes = colorsManager.getGlobalScheme.getAttributes(EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES)
            }
            else if (name == depends.get(index).get) {
              attributes = colorsManager.getGlobalScheme.getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES)
            }
            if (attributes != null) rangesToHighlight.put(segmentOffset, attributes)
            i += 1
          }
          addHighlights(rangesToHighlight, editor, myHighlighters, HighlightManager.getInstance(project))
        }

        private def clearHighlighters() {
          val highlightManager = HighlightManager.getInstance(project)
          myHighlighters.foreach {a => highlightManager.removeSegmentHighlighter(editor, a)}
          rangesToHighlight.clear()
          myHighlighters.clear()
        }
      })

      PsiDocumentManager.getInstance(project).commitDocument(document)
    }
  }

  private def findExpression(_element: PsiElement, editor: Editor): Option[ScExpression] = {
    var element: PsiElement = _element
    if (!element.getParent.isInstanceOf[ScUnderscoreSection]) {
      if (element.getTextRange.getStartOffset == editor.getCaretModel.getOffset) {
        val offset = element.getTextRange.getStartOffset - 1
        if (offset < 0) return None
        element = element.getContainingFile.findElementAt(offset)
      }
    }

    while (element != null) {
      element match {
        case expression: ScExpression =>
          if (ScUnderScoreSectionUtil.isUnderscoreFunction(element)) {
            val underscores = ScUnderScoreSectionUtil.underscores(element)
            val offset = editor.getCaretModel.getOffset
            for (u <- underscores) {
              val range: TextRange = u.getTextRange
              if (range.getStartOffset <= offset && offset <= range.getEndOffset) return Some(expression)
            }
            return None
          }
        case _ =>
      }
      element = element.getParent
    }

    None
  }
}