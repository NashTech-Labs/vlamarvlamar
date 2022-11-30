package persistence

import awscala._
import awscala.dynamodbv2._
import scala.util._

case class DynamoInfo(name: String, accessId: String, accessKey: String)

trait DynamoItem {
  this: Product =>
  def id: Any
  def partitionKey: String = s"${this.productPrefix}#$id"
  def sortKey: String
  def retrieveCriteria: RetrieveCriteria = persistence.RetrieveCriteria(partitionKey, sortKey)
  def searchCriteria(x: String): RetrieveCriteria = persistence.RetrieveCriteria(kind, x)
  def kind: String
  def status: String
  def timestamp: Long
  def json: String
  def searchKey: String = sortKey.split("#").lastOption.getOrElse(jsonHash)
  def ttl: Long = Long.MaxValue
  def ownerId: String = "none"
  def externalOwnerId: String = "none"
  def externalId: String = "none"
  def record: DynamoRecord = DynamoRecord(partitionKey, sortKey, searchKey, kind, status, timestamp, json, ttl, ownerId, externalOwnerId, externalId)
  def jsonHash: String = java.security.MessageDigest.getInstance("MD5").digest(json.getBytes).map("%02X".format(_)).mkString
}

case class DynamoRecord(
                         @hashPK partitionKey: String,
                         @rangePK sortKey: String,
                         searchKey: String,
                         kind: String,
                         status: String,
                         timestamp: Long,
                         json: String,
                         ttl: Long = Long.MaxValue,
                         ownerId: String,
                         externalOwnerId: String,
                         externalId: String
                       )


case class RetrieveCriteria(partitionKey: String, sortKey: String, limit: Int = 100) {
  val pk, kind = partitionKey
  val sk, searchKey, status = sortKey
}

sealed trait DynAttType

object DynAttType {
  case object STR extends DynAttType
  case object NUM extends DynAttType
  case object SEQ extends DynAttType
}

object DynamoTable {
  type PK = String
  type SK = String
}

trait DynamoTable {
  import DynAttType._

  implicit val region: Region = Region.US_EAST_1

  val awsKeyId: String
  val awsAccessKey: String
  val name: String

  implicit val dynamoDB: DynamoDB
  val table: Table// = dynamoDB.table(name).get

  val kindSearchKeyIndex: GlobalSecondaryIndex = GlobalSecondaryIndex(
    name = "kind-searchKey-index",
    keySchema = Seq(KeySchema("kind", KeyType.Hash), KeySchema("searchKey", KeyType.Range)),
    projection = Projection(ProjectionType.All),
    provisionedThroughput = ProvisionedThroughput(readCapacityUnits = 10, writeCapacityUnits = 10))

  val kindStatusIndex: GlobalSecondaryIndex = GlobalSecondaryIndex(
    name = "kind-status-index",
    keySchema = Seq(KeySchema("kind", KeyType.Hash), KeySchema("status", KeyType.Range)),
    projection = Projection(ProjectionType.All),
    provisionedThroughput = ProvisionedThroughput(readCapacityUnits = 10, writeCapacityUnits = 10))

  val kindSkIndex: GlobalSecondaryIndex = GlobalSecondaryIndex(
    name = "kind-sk-index",
    keySchema = Seq(KeySchema("kind", KeyType.Hash), KeySchema("sk", KeyType.Range)),
    projection = Projection(ProjectionType.All),
    provisionedThroughput = ProvisionedThroughput(readCapacityUnits = 10, writeCapacityUnits = 10))

  val kindExternalIdIndex: GlobalSecondaryIndex = GlobalSecondaryIndex(
    name = "kind-externalId-index",
    keySchema = Seq(KeySchema("kind", KeyType.Hash), KeySchema("externalId", KeyType.Range)),
    projection = Projection(ProjectionType.All),
    provisionedThroughput = ProvisionedThroughput(readCapacityUnits = 10, writeCapacityUnits = 10))

  val kindExternalOwnerIdIndex: GlobalSecondaryIndex = GlobalSecondaryIndex(
    name = "kind-externalOwnerId-index",
    keySchema = Seq(KeySchema("kind", KeyType.Hash), KeySchema("externalOwnerId", KeyType.Range)),
    projection = Projection(ProjectionType.All),
    provisionedThroughput = ProvisionedThroughput(readCapacityUnits = 10, writeCapacityUnits = 10))


  // This maps each field to an attribute in dynamo
  def insertItem(item: DynamoItem): Either[String, String] = Try(table.putItem(item.partitionKey, item.sortKey, item)) match {
    case Success(_) => Right("Success")
    case Failure(e) => Left(e.getMessage)
  }

  def insertRecord(record: DynamoRecord): Either[String, DynamoRecord] = Try(table.putItem(record)) match {
    case Success(_) => Right(record)
    case Failure(e) => Left(e.getMessage)
  }

  def insertRecords(records: Seq[DynamoRecord]): Seq[Either[String, DynamoRecord]] = records.map(r => insertRecord(r))

  def itemToDynamoRecord(partitionKey: String, sortKey: String, item: Item): DynamoRecord = DynamoRecord(
    partitionKey = partitionKey,
    sortKey = sortKey,
    searchKey = if (retrieveAttribute(item,  "searchKey").isEmpty) "" else retrieveAttribute(item,  "searchKey").head,
    kind = if (retrieveAttribute(item, "kind").isEmpty) "" else retrieveAttribute(item, "kind").head,
    status = if (retrieveAttribute(item, "status").isEmpty) "" else retrieveAttribute(item, "status").head,
    timestamp = if (retrieveAttribute(item, "timestamp", NUM).isEmpty) 0 else retrieveAttribute(item, "timestamp", NUM).head.toLong,
    json = if (retrieveAttribute(item, "json").isEmpty) "" else retrieveAttribute(item, "json").head,
    ownerId = if (retrieveAttribute(item, "ownerId").isEmpty) "" else retrieveAttribute(item, "ownerId").head,
    externalOwnerId = if (retrieveAttribute(item, "externalOwnerId").isEmpty) "" else retrieveAttribute(item, "externalOwnerId").head,
    externalId = if (retrieveAttribute(item, "externalId").isEmpty) "" else retrieveAttribute(item, "externalId").head,
  )

  def retrieve(key: RetrieveCriteria): Option[DynamoRecord] = {
    table.get(key.pk, key.sk).map{ item => itemToDynamoRecord(key.pk, key.sk, item)}
  }

  def search(key: RetrieveCriteria): Seq[DynamoRecord] = {
    table.queryWithIndex(kindSearchKeyIndex, Seq("kind" -> cond.eq(key.kind), "searchKey" -> cond.beginsWith(key.searchKey))).map{ item => itemToDynamoRecord(key.pk, key.sk, item)}
  }

  def searchStatus(key: RetrieveCriteria): Seq[DynamoRecord] = {
    println("searchStatus:"+key)
    table.queryWithIndex(kindStatusIndex, Seq("kind" -> cond.eq(key.kind), "status" -> cond.beginsWith(key.status))).map{ item => itemToDynamoRecord(key.pk, key.sk, item)}
  }

  def searchOne(key: RetrieveCriteria): Option[DynamoRecord] = {
    table.queryWithIndex(kindSearchKeyIndex, Seq("kind" -> cond.eq(key.kind), "searchKey" -> cond.beginsWith(key.searchKey))).map{ item => itemToDynamoRecord(key.pk, key.sk, item) }.headOption
  }

  def searchAll(key: RetrieveCriteria): Seq[DynamoRecord] = {
    table.queryWithIndex(kindSearchKeyIndex, Seq("kind" -> cond.eq(key.kind), "searchKey" -> cond.beginsWith(key.searchKey))).map{ item => itemToDynamoRecord(key.pk, key.sk, item)}
  }

  def delete(key: RetrieveCriteria) = {
    table.delete(key.pk, key.sk)
  }

  def delete(item: DynamoItem) = {
    table.delete(item.partitionKey, item.sortKey)
  }

  def batch(keys: List[(String, String, String, String)]): Seq[Item] = {
    table.batchGet(keys)
  }

  def queryByPk(pk: String): Seq[DynamoRecord] = {
    table.query(Seq("pk" -> cond.eq(pk))).map{ item =>
      val sk = retrieveAttribute(item, "sk").head
      itemToDynamoRecord(pk, sk, item)}
  }

  def queryKind(kind: String): Seq[DynamoRecord] = {
    table.queryWithIndex(kindSkIndex, Seq("kind" -> cond.eq(kind))).map { item =>
      val pk = retrieveAttribute(item, "pk").head
      val sk = retrieveAttribute(item, "sk").head
      itemToDynamoRecord(pk, sk, item)
    }
  }

  def queryKindSk(kind: String, sk: String) = {
    table.queryWithIndex(kindSkIndex, Seq("kind" -> cond.eq(kind), "sk" -> cond.eq(sk))).map {
      item =>
        val pk = retrieveAttribute(item, "pk").head
        val sk = retrieveAttribute(item, "sk").head
        itemToDynamoRecord(pk, sk, item)
    }
  }

  def queryKindSearchKey(kind: String, searchKey: String): Seq[DynamoRecord] = {
    table.queryWithIndex(kindSearchKeyIndex, Seq("kind" -> cond.eq(kind), "searchKey" -> cond.beginsWith(searchKey))).map {
      item =>
        val pk = retrieveAttribute(item, "pk").head
        val sk = retrieveAttribute(item, "sk").head
        itemToDynamoRecord(pk, sk, item)
    }
  }

  def queryKindExternalId(kind: String, externalId: String): Seq[DynamoRecord] = {
    table.queryWithIndex(kindExternalIdIndex, Seq("kind" -> cond.eq(kind), "externalId" -> cond.eq(externalId))).map { item =>
      val pk = retrieveAttribute(item, "pk").head
      val sk = retrieveAttribute(item, "sk").head
      itemToDynamoRecord(pk, sk, item)
    }
  }

  def queryKindExternalOwnerId(kind: String, externalOwnerId: String): Seq[DynamoRecord] = {
    table.queryWithIndex(kindExternalOwnerIdIndex, Seq("kind" -> cond.eq(kind), "externalOwnerId" -> cond.eq(externalOwnerId))).map { item =>
      val pk = retrieveAttribute(item, "pk").head
      val sk = retrieveAttribute(item, "sk").head
      itemToDynamoRecord(pk, sk, item)
    }
  }

  /*
  Notes
  partition does not support begins with or contains.
  */
  def queryRangeGT(key: RetrieveCriteria) = {
    table.query(
      Seq(
        "pk" -> cond.eq(key.partitionKey), 
        "sk" -> cond.gt(key.sortKey)
      ),
      limit = key.limit
    ).map{ item => itemToDynamoRecord(key.partitionKey, key.sortKey, item)}
  }

  def queryRangeBtn(pk: String, from: Long, to: Long) = {
    table.query(Seq("pk" -> cond.eq(pk), "sk" -> cond.between(from.toString, to.toString))).map{ item => itemToDynamoRecord(pk, from.toString, item)}
  }

  def queryPkSk(pk: String, sk: String) = {
    table.query(Seq("pk" -> cond.eq(pk), "sk" -> cond.eq(sk))).map{ item => itemToDynamoRecord(pk, sk, item)}
  }

  def queryRangeBeginsWith(pk: String, sk: String) = {
    table.query(Seq("pk" -> cond.eq(pk), "sk" -> cond.beginsWith(sk))).map{ item => itemToDynamoRecord(pk, sk, item)}
  }

  def queryPartition(pk: String) = {
    table.query(Seq("pk" -> cond.eq(pk))).map{ item => itemToDynamoRecord(pk, "", item)}
  }

  def scanPartition(partition: String, maybeCondition: Option[com.amazonaws.services.dynamodbv2.model.Condition] = None, limit: Int = 10) = {
    val condition = maybeCondition match {
      case Some(c) => c
      case _       => cond.beginsWith(partition)
    }

    table.scan(Seq("pk" -> condition)).toList.map(i => itemToDynamoRecord(partition, "", i))
  }

  def scanPartitionRange(
    partition: String, 
    maybePkCondition: Option[com.amazonaws.services.dynamodbv2.model.Condition] = None, 
    range: String, 
    maybeSkCondition: Option[com.amazonaws.services.dynamodbv2.model.Condition] = None, 
    limit: Int = 10) = {
    
    val pkCondition = maybePkCondition match {
      case Some(c) => c
      case None    => cond.beginsWith(partition)
    }

    val skCondition = maybeSkCondition match {
      case Some(c) => c
      case None    => cond.beginsWith(range)
    }

    table.scan(Seq("pk" -> pkCondition, "sk" -> skCondition)).toList.map(i => itemToDynamoRecord(partition, range, i))
  }

  def retrieveAttribute(item: Item, key: String, kind: DynAttType = STR): List[String] = {
    val a = item.attributes.find(_.name == key).map(_.value)
    kind match {
      case STR => a.flatMap(_.s).map(s => List(s)).getOrElse(Nil)
      case NUM => a.flatMap(_.n).map(s => List(s)).getOrElse(Nil)
      case SEQ => a.map(x => x.ss.toList).getOrElse(Nil)
    }
  }
}

