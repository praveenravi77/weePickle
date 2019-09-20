package com.rallyhealth.upack.v1
import com.rallyhealth.upickle.v1.core.Abort
import utest._

object UnitTests extends TestSuite{
  val tests = Tests {
    test("compositeKeys"){
      val msg = Obj(Arr(Int32(1), Int32(2)) -> Int32(1))
      val written = com.rallyhealth.upack.v1.write(msg)
      val writtenStr = com.rallyhealth.upickle.v1.core.Util.bytesToString(written)
      writtenStr ==> "81-92-01-02-01"

      com.rallyhealth.upack.v1.read(written) ==> msg


      intercept[Abort]{
        com.rallyhealth.upack.v1.transform(written, com.rallyhealth.ujson.v1.Value)
      }
      intercept[Abort] {
        com.rallyhealth.upack.v1.transform(msg, com.rallyhealth.ujson.v1.Value)
      }
    }
  }
}