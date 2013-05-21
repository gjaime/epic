package epic.framework

/*
 Copyright 2012 David Hall

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/
import java.io.File
import breeze.linalg.{DenseVector, Counter}
import breeze.util._

/**
 * Interface for producing Models from training data.
 * @author dlwh
 */
trait ModelFactory[Datum] {
  type MyModel <: Model[Datum]

  def make(train: IndexedSeq[Datum]): MyModel

  def readWeights(in: File):Counter[Feature, Double] = if(in != null && in.exists) {
    try {
      val ctr = breeze.util.readObject[Counter[Feature, Double]](in)
      ctr
    } catch {
      case e: Exception => Counter[Feature, Double]()
    }
  } else {
    Counter[Feature, Double]()
  }
}
