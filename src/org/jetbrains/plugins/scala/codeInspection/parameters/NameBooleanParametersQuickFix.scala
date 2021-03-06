package org.jetbrains.plugins.scala
package codeInspection.parameters

import com.intellij.openapi.project.Project
import com.intellij.codeInspection.{ProblemDescriptor, LocalQuickFix}
import lang.psi.api.base.ScLiteral
import lang.psi.api.expr.ScMethodCall
import org.jetbrains.plugins.scala.util.IntentionUtils

/**
 * @author Ksenia.Sautina
 * @since 5/10/12
 */

class NameBooleanParametersQuickFix(expr: ScMethodCall, element: ScLiteral) extends LocalQuickFix {
  def getName = "Name boolean parameters"

  def getFamilyName = "Name boolean parameters"

  def applyFix(project: Project, descriptor: ProblemDescriptor) {
    if (!element.isValid) return

    IntentionUtils.check(element) match {
      case Some(x) => x()
      case None =>
    }
  }
}
