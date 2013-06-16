package epic.parser.models

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
import epic.framework.{Feature, Model, ModelFactory}
import breeze.linalg._
import epic.parser.{BaseGrammar, Parser}
import epic.trees.TreeInstance
import epic.lexicon.Lexicon
import java.io.File
import epic.constraints.ChartConstraints
import epic.util.CacheBroker

/**
 *
 * @author dlwh
 */

trait ParserExtractable[L, W] {
  def baseGrammar: BaseGrammar[L]
  def lexicon: Lexicon[L, W]
  def extractParser(weights: DenseVector[Double]): Parser[L, W]
}


trait ParserExtractableModelFactory[L,W] {
  def make(train: IndexedSeq[TreeInstance[L, W]], constrainer: ChartConstraints.Factory[L, W])(implicit broker: CacheBroker): MyModel

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

  type MyModel <: Model[TreeInstance[L,W]] with ParserExtractable[L,W]
}