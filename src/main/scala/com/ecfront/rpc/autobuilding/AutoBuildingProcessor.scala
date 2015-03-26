package com.ecfront.rpc.autobuilding

import com.ecfront.common.{BeanHelper, methodAnnotationInfo}
import com.ecfront.rpc.RPC.Result
import com.ecfront.rpc.Server
import com.typesafe.scalalogging.slf4j.LazyLogging

object AutoBuildingProcessor extends LazyLogging {

  def process(server: Server, instance: AnyRef, formatUrl: => (String) => String, preExecute: => (Map[String, String], Any) => Boolean, postExecute: => (Any) => Any) = {
    BeanHelper.findMethodAnnotations(instance.getClass, Seq(classOf[get], classOf[post], classOf[put], classOf[delete])).foreach {
      methodInfo =>
        val methodMirror = BeanHelper.invoke(instance, methodInfo.method)
        methodInfo.annotation match {
          case ann: get if server.isHighPerformance && ann.akka || !server.isHighPerformance && ann.http =>
            server.reflect.get(formatUrl(ann.uri), {
              (param, _) =>
                try {
                  if (preExecute(param, null)) {
                    postExecute(methodMirror(param))
                  }
                } catch {
                  case e: Exception =>
                    logger.error("Occurred unchecked exception.", e)
                    Result.serverError("Occurred unchecked exception.")
                }
            })
          case ann: post if server.isHighPerformance && ann.akka || !server.isHighPerformance && ann.http =>
            server.reflect.post(formatUrl(ann.uri), getClassFromMethodInfo(methodInfo), {
              (param, body) =>
                try {
                  if (preExecute(param, body)) {
                    postExecute(methodMirror(param, body))
                  }
                } catch {
                  case e: Exception =>
                    logger.error("Occurred unchecked exception.", e)
                    Result.serverError("Occurred unchecked exception.")
                }
            })
          case ann: put if server.isHighPerformance && ann.akka || !server.isHighPerformance && ann.http =>
            server.reflect.put(formatUrl(ann.uri), getClassFromMethodInfo(methodInfo), {
              (param, body) =>
                try {
                  if (preExecute(param, body)) {
                    postExecute(methodMirror(param, body))
                  }
                } catch {
                  case e: Exception =>
                    logger.error("Occurred unchecked exception.", e)
                    Result.serverError("Occurred unchecked exception.")
                }
            })
          case ann: delete if server.isHighPerformance && ann.akka || !server.isHighPerformance && ann.http =>
            server.reflect.delete(formatUrl(ann.uri), {
              (param, _) =>
                try {
                  if (preExecute(param, null)) {
                    postExecute(methodMirror(param))
                  }
                } catch {
                  case e: Exception =>
                    logger.error("Occurred unchecked exception.", e)
                    Result.serverError("Occurred unchecked exception.")
                }
            })
          case _ =>
        }
    }
  }

  private def getClassFromMethodInfo(methodInfo: methodAnnotationInfo): Class[_] = {
    val clazzStr = methodInfo.method.paramLists.head(1).info.toString
    clazzStr match {
      case "Int" => classOf[Int]
      case "Long" => classOf[Long]
      case "Float" => classOf[Float]
      case "Double" => classOf[Double]
      case "Boolean" => classOf[Boolean]
      case "Short" => classOf[Short]
      case "Byte" => classOf[Byte]
      case s if s.startsWith("Map") => Class.forName("scala.collection.immutable.Map")
      case s if s.startsWith("List") => Class.forName("scala.collection.immutable.List")
      case s if s.startsWith("Set") => Class.forName("scala.collection.immutable.Set")
      case s if s.startsWith("Seq") => Class.forName("scala.collection.immutable.Seq")
      //去泛型
      case s if s.endsWith("]") => Class.forName(s.substring(0, s.indexOf("[")))
      case s => Class.forName(s)
    }
  }

}