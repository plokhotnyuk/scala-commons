package com.avsystem.commons
package jiop

import scala.collection.convert.{DecorateAsJava, DecorateAsScala}

trait JavaInterop extends Any
  with JBasicUtils
  with JCollectionUtils
  with DecorateAsJava
  with DecorateAsScala
  with Java8CollectionUtils
  with JFunctionUtils
  with JStreamUtils
  with JOptionalUtils
  with GuavaUtils

object JavaInterop extends JavaInterop