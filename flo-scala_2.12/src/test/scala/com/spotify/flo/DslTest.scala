package com.spotify.flo

import org.scalatest._

class DslTest extends FlatSpec with Matchers {

  "A defTask `$` builder" can "be accessed in dsl scope to create a task" in {
    defTask[String]() dsl ($ process "hello") shouldBe a [Task[_]]
  }

  it should "create a task using the name of the enclosing class method" in {
    classMethod.id.name shouldBe "classMethod"
  }

  it should "create a task using the name of the enclosing inner method" in {
    def innerMethod = defTask().process("hello")

    innerMethod.id.name shouldBe "innerMethod"
  }

  it should "create a task with a specific name" in {
    def task = defTaskNamed("specific-name").process("hello")

    task.id.name shouldBe "specific-name"
  }

  it should "create a task with arguments" in {
    def task = defTask(1, 2, 3).process("hello")

    task.id.toString shouldBe "task(1,2,3)#2ac733ae"
  }

  it must "throw a RuntimeException if accessed outside of defTask" in {
    val exception = the [RuntimeException] thrownBy $

    exception should have message "Builder accessor used outside a defTask scope"
  }

  def classMethod: Task[String] = defTask().process("hello")
}
