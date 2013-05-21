package epic.util

import java.io.{File, IOException}
import java.util.Collections
import org.mapdb.DBMaker
import scala.collection.JavaConverters._
import scala.collection.concurrent.Map
import java.util

import CacheBroker._
import scala.annotation.migration
import scala.collection.{mutable, GenTraversableOnce}
import scala.collection.parallel.Combiner
import scala.collection.parallel.mutable.ParMap

@SerialVersionUID(1L)
case class CacheBroker(path: File = null, autocommit: Boolean = true, disableWriteAheadLog: Boolean = false) extends Serializable {
  // this is how one makes a transient lazy val, sadly.
  @transient
  private var _actualCache:ActualCache = null
  private def actualCache = synchronized {
    lazy val dbMaker = if(path eq null) {
      DBMaker.newMemoryDB()
    } else {
      DBMaker.newFileDB(path)
    }.closeOnJvmShutdown().cacheSoftRefEnable()

    if(_actualCache eq null) {
      _actualCache = getCacheBroker(path, dbMaker, autocommit)
    }
    if(disableWriteAheadLog) _actualCache.dbMaker.writeAheadLogDisable()

    _actualCache
  }


  def dbMaker = actualCache.dbMaker
  def db = actualCache.db


  def commit() { db.commit()}
  def close() {db.close()}

  def make[K,V](name: String): Map[K, V] = new Map[K, V] {
    @transient
    private var _theMap : Map[K, V] = null
    def theMap = synchronized {
      if(_theMap eq null) {
        _theMap = db.getHashMap[K, V](name).asScala
      }
      _theMap
    }


    def +=(kv: (K, V)): this.type = {theMap += kv; this}

    def -=(key: K): this.type = {theMap -= key; this}

    def get(key: K): Option[V] = theMap.get(key: K)

    def iterator: Iterator[(K, V)] = theMap.iterator

    def putIfAbsent(k: K, v: V): Option[V] = theMap.putIfAbsent(k: K, v: V)

    def remove(k: K, v: V): Boolean = theMap.remove(k: K, v: V)

    def replace(k: K, oldvalue: V, newvalue: V): Boolean = theMap.replace(k: K, oldvalue: V, newvalue: V)

    def replace(k: K, v: V): Option[V] = theMap.replace(k: K, v: V)

    override def size: Int =  theMap.size

    override def put(key: K, value: V): Option[V] = theMap.put(key: K, value: V): Option[V]

    override def update(key: K, value: V) {
      theMap.update(key, value)
    }

    override def updated[B1 >: V](key: K, value: B1): mutable.Map[K, B1] = theMap.updated(key, value)

    override def +[B1 >: V](kv: (K, B1)): mutable.Map[K, B1] = theMap.+(kv)

    override def +[B1 >: V](elem1: (K, B1), elem2: (K, B1), elems: (K, B1)*): mutable.Map[K, B1] = theMap.+(elem1, elem2, elems:_*)

    override def ++[B1 >: V](xs: GenTraversableOnce[(K, B1)]): mutable.Map[K, B1] = theMap.++(xs)

    override def remove(key: K): Option[V] = theMap.remove(key)

    override def -(key: K): mutable.Map[K, V] = theMap.-(key)

    override def clear() {
      theMap.clear()
    }

    override def getOrElseUpdate(key: K, op: => V): V = theMap.getOrElseUpdate(key, op)

    override def transform(f: (K, V) => V): this.type = {theMap.transform(f);this}

    override def retain(p: (K, V) => Boolean): this.type = {theMap.retain(p); this}

    override def clone(): mutable.Map[K, V] = theMap.clone()

    override def result(): mutable.Map[K, V] = theMap.result()

    override def -(elem1: K, elem2: K, elems: K*): mutable.Map[K, V] = theMap.-(elem1, elem2, elems:_*)

    override def --(xs: GenTraversableOnce[K]): mutable.Map[K, V] = theMap.--(xs)

  }

}

object CacheBroker {
  private class ActualCache private[CacheBroker] (val path: File, val dbMaker: DBMaker, val autocommit: Boolean) {
    lazy val db = {if(autocommit) cacheThread.start(); dbMaker.make()}
    private lazy val cacheThread: Thread = new Thread(new Runnable {
      def run() {
        while(true) {
          Thread.sleep(1000 * 60 * 5)
          db.commit()
        }
      }
    }) {
      setDaemon(true)
    }
  }

  private val cacheCache = Collections.synchronizedMap(new util.HashMap[File, ActualCache]()).asScala

  private def getCacheBroker(path: File, dbMaker: =>DBMaker, autocommit: Boolean) = {
    if(path eq null) new ActualCache(path, dbMaker, autocommit)
    else cacheCache.getOrElseUpdate(path, new ActualCache(path, dbMaker, autocommit))
  }

}