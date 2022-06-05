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

    elastic.execute(indexExists(name)).map {
      case response: RequestSuccess[IndexExistsResponse] if !response.result.isExists || settings.clearElastic =>
        for {
          _ <- elastic.execute(deleteIndex(name))
          _ <- elastic.execute(createIndex(name).mapping(mapping))
        } yield {
          if (response.result.isExists) {
            logger.info(s"Elastic index $name was cleared!")
          } else {
            logger.info(s"Elastic index $name was created!")
          }
        }
    }
  }
}
