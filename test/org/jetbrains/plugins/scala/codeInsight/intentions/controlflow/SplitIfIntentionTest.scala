package org.jetbrains.plugins.scala.codeInsight.intentions.controlflow

import org.jetbrains.plugins.scala.codeInsight.intentions.ScalaIntentionTestBase
import org.jetbrains.plugins.scala.codeInsight.intention.controlflow.SplitIfIntention

/**
 * @author Ksenia.Sautina
 * @since 6/8/12
 */

class SplitIfIntentionTest extends ScalaIntentionTestBase {
  val familyName = SplitIfIntention.familyName

  def testSplitIf() {
    val text  = """
                  |class X {
                  |  def f(a: Boolean, b: Boolean) {
                  |    if (a &<caret>& b) return
                  |  }
                  |}
                """.stripMargin.replace("\r", "").trim
    val resultText = """
                       |class X {
                       |  def f(a: Boolean, b: Boolean) {
                       |    if (<caret>a)
                       |      if (b) return
                       |  }
                       |}
                     """.stripMargin.replace("\r", "").trim

    doTest(text, resultText)
  }

  def testSplitIf2() {
    val text  = """
                  |class X {
                  |  def f(a: Boolean, b: Boolean) {
                  |    if (a &<caret>& b) {
                  |      return
                  |    } else {
                  |      System.out.println()
                  |    }
                  |  }
                  |}
                """.stripMargin.replace("\r", "").trim
    val resultText = """
                       |class X {
                       |  def f(a: Boolean, b: Boolean) {
                       |    if (<caret>a)
                       |      if (b) {
                       |        return
                       |      } else {
                       |        System.out.println()
                       |      }
                       |    else {
                       |      System.out.println()
                       |    }
                       |  }
                       |}
                     """.stripMargin.replace("\r", "").trim

    doTest(text, resultText)
  }

  def testSplitIf3() {
    val text  = """
                  |class X {
                  |  def f(a: Boolean, b: Boolean) {
                  |    if (a &<caret>& b) {
                  |      return
                  |    }
                  |  }
                  |}
                """.stripMargin.replace("\r", "").trim
    val resultText = """
                       |class X {
                       |  def f(a: Boolean, b: Boolean) {
                       |    if (<caret>a)
                       |      if (b) {
                       |        return
                       |      }
                       |  }
                       |}
                     """.stripMargin.replace("\r", "").trim

    doTest(text, resultText)
  }

  def testSplitIf4() {
    val text  = """
                  |class X {
                  |  def f(a: Boolean, b: Boolean) {
                  |    if (a &<caret>& b)
                  |      System.out.println("if")
                  |    else
                  |      System.out.println("else")
                  |  }
                  |}
                """.stripMargin.replace("\r", "").trim
    val resultText = """
                       |class X {
                       |  def f(a: Boolean, b: Boolean) {
                       |    if (<caret>a)
                       |      if (b) System.out.println("if")
                       |      else System.out.println("else")
                       |    else System.out.println("else")
                       |  }
                       |}
                     """.stripMargin.replace("\r", "").trim

    doTest(text, resultText)
  }

  def testSplitIf5() {
    val text  = """
                  |class X {
                  |  def f(a: Boolean, b: Boolean) {
                  |    if (a &<caret>& b) {
                  |      System.out.println("if")
                  |    } else
                  |      System.out.println("else")
                  |  }
                  |}
                """.stripMargin.replace("\r", "").trim
    val resultText = """
                       |class X {
                       |  def f(a: Boolean, b: Boolean) {
                       |    if (<caret>a)
                       |      if (b) {
                       |        System.out.println("if")
                       |      } else System.out.println("else")
                       |    else System.out.println("else")
                       |  }
                       |}
                     """.stripMargin.replace("\r", "").trim

    doTest(text, resultText)
  }

  def testSplitIf6() {
    val text  = """
                  |class X {
                  |  def f(a: Boolean, b: Boolean) {
                  |    if (a &<caret>& b) System.out.println("if")
                  |    else {
                  |      System.out.println("else")
                  |    }
                  |  }
                  |}
                """.stripMargin.replace("\r", "").trim
    val resultText = """
                       |class X {
                       |  def f(a: Boolean, b: Boolean) {
                       |    if (<caret>a)
                       |      if (b) System.out.println("if")
                       |      else {
                       |        System.out.println("else")
                       |      }
                       |    else {
                       |      System.out.println("else")
                       |    }
                       |  }
                       |}
                     """.stripMargin.replace("\r", "").trim

    doTest(text, resultText)
  }

  def testSplitIf7() {
    val text  = """
                  |class X {
                  |  def f(a: Boolean, b: Boolean) {
                  |    if ((a || b) &<caret>& b) System.out.println("if")
                  |    else {
                  |      System.out.println("else")
                  |    }
                  |  }
                  |}
                """.stripMargin.replace("\r", "").trim
    val resultText = """
                       |class X {
                       |  def f(a: Boolean, b: Boolean) {
                       |    if (<caret>a || b)
                       |      if (b) System.out.println("if")
                       |      else {
                       |        System.out.println("else")
                       |      }
                       |    else {
                       |      System.out.println("else")
                       |    }
                       |  }
                       |}
                     """.stripMargin.replace("\r", "").trim

    doTest(text, resultText)
  }

  def testSplitIf8() {
    val text  = """
                  |class X {
                  |  def f(a: Boolean, b: Boolean) {
                  |    if ((a || b) &<caret>& (b && a)) System.out.println("if")
                  |    else {
                  |      System.out.println("else")
                  |    }
                  |  }
                  |}
                """.stripMargin.replace("\r", "").trim
    val resultText = """
                       |class X {
                       |  def f(a: Boolean, b: Boolean) {
                       |    if (<caret>a || b)
                       |      if (b && a) System.out.println("if")
                       |      else {
                       |        System.out.println("else")
                       |      }
                       |    else {
                       |      System.out.println("else")
                       |    }
                       |  }
                       |}
                     """.stripMargin.replace("\r", "").trim

    doTest(text, resultText)
  }
}