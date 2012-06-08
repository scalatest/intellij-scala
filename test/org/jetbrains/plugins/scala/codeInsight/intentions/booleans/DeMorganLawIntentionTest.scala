package org.jetbrains.plugins.scala
package codeInsight.intentions.booleans

import codeInsight.intentions.ScalaIntentionTestBase
import codeInsight.intention.booleans.DeMorganLawIntention

/**
 * @author Ksenia.Sautina
 * @since 5/12/12
 */

class DeMorganLawIntentionTest extends ScalaIntentionTestBase {
  def familyName = DeMorganLawIntention.familyName

  def test() {
    val text = "if (a |<caret>| b) {}"
    val resultText = "if (!(!a &<caret>& !b)) {}"

    doTest(text, resultText)
  }

  def test2() {
    val text = "if (a &<caret>& b) {}"
    val resultText = "if (!(!a |<caret>| !b)) {}"

    doTest(text, resultText)
  }

  def test3() {
    val text = "if (!a |<caret>| b) {}"
    val resultText = "if (!(a &<caret>& !b)) {}"

    doTest(text, resultText)
  }

  def test4() {
    val text = "if (a |<caret>| !b) {}"
    val resultText = "if (!(!a &<caret>& b)) {}"

    doTest(text, resultText)
  }

  def test5() {
    val text = "if (!a |<caret>| !b) {}"
    val resultText = "if (!(a &<caret>& b)) {}"

    doTest(text, resultText)
  }

  def test6() {
    val text = "if (!a &<caret>& b) {}"
    val resultText = "if (!(a |<caret>| !b)) {}"

    doTest(text, resultText)
  }

  def test7() {
    val text = "if (a <caret>&& !b) {}"
    val resultText = "if (!(!a <caret>|| b)) {}"

    doTest(text, resultText)
  }

  def test8() {
    val text = "if (!a &&<caret> !b) {}"
    val resultText = "if (!(a ||<caret> b)) {}"

    doTest(text, resultText)
  }

  def test9() {
    val text = "if (true |<caret>| false) {}"
    val resultText = "if (!(false &<caret>& true)) {}"

    doTest(text, resultText)
  }

  def test10() {
    val text = "!(!left &<caret>& !right)"
    val resultText = "left |<caret>| right"

    doTest(text, resultText)
  }

  def test11() {
    val text =
      """
        |val % = true
        |!(!(%) &<caret>& !(%))
      """.stripMargin.replace("\r", "").trim
    val resultText =
      """
        |val % = true
        |% |<caret>| %
      """.stripMargin.replace("\r", "").trim

    doTest(text, resultText)
  }

  def test12() {
    val text =
      """
        |val % = true
        |% |<caret>| %
      """.stripMargin.replace("\r", "").trim
    val resultText =
      """
        |val % = true
        |!(!(%) &<caret>& !(%))
      """.stripMargin.replace("\r", "").trim

    doTest(text, resultText)
  }

  def test13() {
    val text =
      """
        |val b = true
        |(true equals b) |<caret>| true
      """.stripMargin.replace("\r", "").trim
    val resultText =
      """
        |val b = true
        |!(!(true equals b) &<caret>& false)
      """.stripMargin.replace("\r", "").trim

    doTest(text, resultText)
  }

  def test14() {
    val text =
      """
        |val b = true
        |!(!(true equals b) &<caret>& false)
      """.stripMargin.replace("\r", "").trim
    val resultText =
      """
        |val b = true
        |(true equals b) |<caret>| true
      """.stripMargin.replace("\r", "").trim

    doTest(text, resultText)
  }

  def test15() {
    val text =
      """
        |val % = true
        |(%) |<caret>| (%)
      """.stripMargin.replace("\r", "").trim
    val resultText =
      """
        |val % = true
        |!(!(%) &<caret>& !(%))
      """.stripMargin.replace("\r", "").trim

    doTest(text, resultText)
  }



}