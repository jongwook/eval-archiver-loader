package com.github.jongwook

import org.scalatest._

trait Calculator {
  def run(): Int
}

class EvalSpec extends FlatSpec with Matchers {
  "Eval Loader" should "correctly load what EvalArchiver has saved" in {
    val code = EvalArchiver[Calculator](
      """import com.github.jongwook._
        |
        |new Calculator {
        |  def run(): Int = 42
        |}
      """.stripMargin)

    val factory = EvalLoader[Calculator](code)
    val answerer = factory()
    answerer.run() should be (42)
  }
}