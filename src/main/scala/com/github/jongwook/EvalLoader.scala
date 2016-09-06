package com.github.jongwook

import java.io.{ByteArrayInputStream, FileInputStream, InputStream}
import java.util.jar.{Attributes, JarInputStream}
import scala.reflect.runtime.universe._
import scala.util.{Try, Failure}

/** Loads an implementation serialized by [[EvalArchiver]] and returns a function that will an instance */
object EvalLoader {

  private val classLoader = new ClassLoader(getClass.getClassLoader) {
    def define(name: String, bytes: Array[Byte]): Class[_] = define(name, bytes, 0, bytes.length)
    def define(name: String, bytes: Array[Byte], offset: Int, length: Int): Class[_] = {
      defineClass(name, bytes, offset, length)
    }
  }

  def apply[T](path: String): () => T = {
    val input = new FileInputStream(path)
    try {
      apply(input)
    } finally {
      input.close()
    }
  }

  def apply[T](input: Array[Byte]): () => T = {
    apply(new ByteArrayInputStream(input))
  }

  def apply[T](input: InputStream): () => T = {
    val jar = new JarInputStream(input)
    try {
      val main = jar.getManifest.getMainAttributes.getValue(Attributes.Name.MAIN_CLASS)

      var entry = jar.getNextJarEntry
      while (entry != null) {
        val name = entry.getName
        if (name.endsWith(".class")) {
          val clazz = name.replace(".class", "").replace("/", ".").replace("\\", ".")
          if (Try(classLoader.loadClass(clazz)).isFailure) {
            // if not already defined, load the class
            val bytes = reflect.io.Streamable.bytes(jar)
            classLoader.define(clazz, bytes)
          }
        }
        entry = jar.getNextJarEntry
      }

      val constructor = classLoader.loadClass(main).getConstructor()
      constructor.newInstance().asInstanceOf[() => T]
    } finally {
      jar.close()
    }
  }

}
