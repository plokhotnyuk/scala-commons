package com.avsystem.commons
package rpc

import com.avsystem.commons.macros.rpc.RpcMacros

trait RpcMetadataCompanion[M[_]] extends RpcImplicitsProvider {
  def materializeForRpc[Real]: M[Real] = macro RpcMacros.rpcMetadata[M[Real], Real]

  final class Lazy[T](metadata: => M[T]) {
    lazy val value: M[T] = metadata
  }
  object Lazy {
    def apply[T](metadata: => M[T]): Lazy[T] = new Lazy(metadata)

    // macro effectively turns `metadata` param into by-name param (implicit params by themselves cannot be by-name)
    implicit def lazyMetadata[T](implicit metadata: M[T]): Lazy[T] = macro RpcMacros.lazyMetadata
  }
}
