package com.github.jongwook

import java.io.{ByteArrayOutputStream, FileInputStream}
import java.nio.file.{Files, Paths}
import java.util.UUID
import java.util.jar.{Attributes, JarOutputStream, Manifest}
import java.util.zip.ZipEntry

import com.twitter.io.StreamIO
import com.twitter.util.{Eval, NonFatal}

import scala.language.reflectiveCalls
import scala.reflect.runtime.universe._

/** Compiles a scala code, using [[Eval]], and returns a byte array that contains the result */
object EvalArchiver {

  private val target = Files.createTempDirectory("EvalArchiver").toFile
  private val eval = new Eval(Some(target))

  /**
    * Compiles given Scala code and returns a byte array that contains the compiled result.
    *
    * @param code       the scala code
    * @param resetState whether the compiler will start from the scratch
    * @tparam T         the type that the last expression in the Scala code will return
    * @return           the byte array representation of the code
    */
  def apply[T: TypeTag](code: String, resetState: Boolean = true): Array[Byte] = {
    // convert primitive class to the corresponding boxed type
    val returnType = typeTag[T].tpe
    if (returnType == typeOf[Nothing]) {
      throw new IllegalArgumentException("A type parameter should be supplied")
    }

    // compile the code
    val processed = eval.sourceForString(code)
    val className = "Evaluator_" + UUID.randomUUID().toString.replace("-", "_")
    val compiled = compiler(wrapCodeInClass(className, returnType, code), className, resetState)

    archive(compiled)
  }

  /**
    * Compiles given Scala code and saves the result to an executable Jar file.
    * @see [[apply]]
    */
  def save[T: TypeTag](path: String, code: String, resetState: Boolean = true): Unit = {
    val bytes = apply(code, resetState)
    Files.write(Paths.get(path), bytes)
  }

  private def wrapCodeInClass(className: String, returnType: Type, code: String) = {
    s"""class $className extends (() => ${returnType.typeSymbol.fullName}) with java.io.Serializable {
       |  def apply() = {
       |$code
       |  }
       |}
       |
       |object $className extends App {
       |  new $className().apply()
       |}
     """.stripMargin
  }

  /** the structural type whose interface is identical to StringCompiler */
  private type Compiler = {
    def apply(code: String, className: String, resetState: Boolean): Class[_]
    def reset(): Unit
  }

  /** holds the reference to the StringCompiler */
  private lazy val compiler: Compiler = {
    // enable the reflective access to the compiler field
    val field = eval.getClass.getDeclaredField("compiler")
    field.setAccessible(true)

    // instantiate the lazy values in eval
    eval("")

    // retrieve the compiler instance
    val compiler = field.get(eval).asInstanceOf[Compiler]
    compiler.reset()
    compiler
  }

  private def archive(compiled: Class[_]): Array[Byte] = {
    val manifest = new Manifest()
    manifest.getMainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")
    manifest.getMainAttributes.put(Attributes.Name.MAIN_CLASS, compiled.getName)

    val bytes = new ByteArrayOutputStream(1024 * 1024)
    val jar = new JarOutputStream(bytes, manifest)

    target.list().foreach { file =>
      jar.putNextEntry(new ZipEntry(file))
      val input = new FileInputStream(s"$target/$file")
      StreamIO.copy(input, jar)
      input.close()
      jar.closeEntry()
    }

    jar.close()

    bytes.toByteArray
  }
}
