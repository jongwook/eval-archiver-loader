package com.github.jongwook

trait AnimalFarm {
  def quack() { println("quack") }
  def woof() { println("woof") }
  def meow() { println("meow") }
  def run(): Unit
}
