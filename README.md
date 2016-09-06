Eval Archiver/Loader
====================

This simple Scala utility provides a functionality to compile a Scala code on-the-fly and reuse it somewhere else. It uses [Twitter's util](https://github.com/twitter/util) and thus requires JDK 8.

## Compiling a String and loading it

[`EvalArchiver`](src/main/scala/com/github/jongwook/EvalArchiver.scala) compiles Scala expressions and returns a byte array. It requires the result type to be passed as a type parameter.

```scala
// in a Scala REPL which has this utility in the classpath, e.g. sbt console 
scala> import com.github.jongwook._
scala> val code = EvalArchiver[Int]("3 * 4 + 4 * 5 + 10")
code: Array[Byte] = Array(80, 75, 3, 4, 20, 0, 8, 8, 8, 0, -51, 30, 38, 73, ...)
```

This byte array can then be loaded by [`EvalLoader`](src/main/scala/com/github/jongwook/EvalLoader.scala), which returns a factory method that returns the evaluated result.

```scala
// in the same Scala REPL
scala> val factory = EvalLoader[Int](code)
factory: () => Int = <function0>

scala> factory()
res1: Int = 42

// or execute the method right away
scala> EvalLoader[Int](code)()
res2: Int = 42
```

## Saving the code as an executable Jar

It is also possible to save the compiled code as an executable Jar file.

```scala
// in a Scala REPL
scala> import com.github.jongwook._
scala> EvalArchiver.save[Unit]("hello.jar", """println("Hello world!")""")

// in bash shell
$ scala hello.jar
Hello world!
```

## Using as a DSL alternative

Using a custom trait as the return type, one can make a DSL-style application, as follows.

```scala
// in AnimalFarm.scala, as in src/test/scala/com/github/jongwook
trait AnimalFarm {
  def quack() { println("quack") }
  def woof() { println("woof") }
  def meow() { println("meow") }
  def run(): Unit
}

// in a Scala application, e.g. sbt test:console
scala> import com.github.jongwook._
scala> val code: Array[Byte] = EvalArchiver[AnimalFarm](
      """import com.github.jongwook._
        |
        |new AnimalFarm {
        |  def run() {
        |    woof(); woof(); meow(); quack()
        |  }
        |}
      """.stripMargin)
      
// possibly running from somewhere else
scala> val farm = EvalLoader[AnimalFarm](code)()
scala> farm.run()
woof
woof
meow
quack
```

Note that the trait to be the return type cannot be defined in a Scala REPL, since the Scala compiler mangles the names of types defined within the REPL.
