package com.avsystem.commons
package rpc

import com.avsystem.commons.macros.misc.MiscMacros

/**
  * @author ghik
  */
object RpcUtils {
  def missingArg(rpcName: String, argName: String): Nothing =
    throw new IllegalArgumentException(s"Can't interpret raw RPC call $rpcName: argument $argName is missing")

  def unknownRpc(rpcName: String, rawMethodName: String): Nothing =
    throw new IllegalArgumentException(s"RPC $rpcName does not map to raw method $rawMethodName")

  def missingOptionalRpc(rawMethodName: String): Nothing =
    throw new IllegalArgumentException(s"no matching real method for optional raw method $rawMethodName")

  def compilationError(error: String): Nothing = macro MiscMacros.compilationError
}