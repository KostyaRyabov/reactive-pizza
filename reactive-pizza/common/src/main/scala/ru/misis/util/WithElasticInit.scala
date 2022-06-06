package ru.misis.util

import com.sksamuel.elastic4s.ElasticApi.{createIndex, deleteIndex}
import com.sksamuel.elastic4s.ElasticDsl.{CreateIndexHandler, DeleteIndexHandler, IndexExistsHandler, indexExists}
import com.sksamuel.elastic4s.requests.indexes.admin.IndexExistsResponse
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess}
import ru.misis.elastic.ElasticClearSettings

import scala.concurrent.ExecutionContext

trait WithElasticInit extends WithLogger {
  def elastic: ElasticClient

  implicit def executionContext: ExecutionContext

  def settings: ElasticClearSettings

  def initElastic(name: String)(mapping: MappingDefinition): Unit = {
    logger.info(s"Init elastic index '$name' ...")

    elastic.execute(indexExists(name)).map({
      case response: RequestSuccess[IndexExistsResponse] if !response.result.isExists || settings.clearElastic =>
        for {
          _ <- elastic.execute(deleteIndex(name)).map(s => logger.info(s"Elastic index '$name': ${s.toString}"))
          _ <- elastic.execute(createIndex(name).mapping(mapping)).map(s => logger.info(s"Elastic index '$name': ${s.toString}"))
        } yield {
          if (response.result.isExists) {
            logger.info(s"Elastic index '$name' was cleared!")
          } else {
            logger.info(s"Elastic index '$name' was created!")
          }
        }
      case response: RequestSuccess[IndexExistsResponse] =>
        logger.info(s"Elastic index '$name' already exists!")
      case ex => logger.info(s"Elastic index '$name' failed: $ex")
    })
  }
}
