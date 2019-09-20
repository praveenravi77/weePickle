package com.rallyhealth.upickle.v1

import utest._
import com.rallyhealth.upickle.v1.default.read
import acyclic.file
import com.rallyhealth.ujson.v1.{IncompleteParseException, ParseException}
import com.rallyhealth.upickle.v1.core.AbortException
case class Fee(i: Int, s: String)
sealed trait Fi
object Fi{
  implicit def rw2: com.rallyhealth.upickle.v1.default.ReadWriter[Fi] = com.rallyhealth.upickle.v1.default.ReadWriter.merge(Fo.rw2, Fum.rw2)
  case class Fo(i: Int) extends Fi
  object Fo{
    implicit def rw2: com.rallyhealth.upickle.v1.default.ReadWriter[Fo] = com.rallyhealth.upickle.v1.default.macroRW
  }
  case class Fum(s: String) extends Fi
  object Fum{
    implicit def rw2: com.rallyhealth.upickle.v1.default.ReadWriter[Fum] = com.rallyhealth.upickle.v1.default.macroRW
  }
}
/**
* Generally, every failure should be a Invalid.Json or a
* InvalidData. If any assertion errors, match errors, number
* format errors or similar leak through, we've failed
*/
object FailureTests extends TestSuite {

  def tests = Tests {
//    test("test"){
//      read[com.rallyhealth.ujson.v1.Value](""" {unquoted_key: "keys must be quoted"} """)
//    }

    test("jsonFailures"){
      // Run through the test cases from the json.org validation suite,
      // skipping the ones which we don't support yet (e.g. leading zeroes,
      // extra commas) or will never support (e.g. too deep)

      val failureCases = Seq(
//        """ "A JSON payload should be an object or array, not a string." """,
        """ {"Extra value after close": true} "misplaced quoted value" """,
        """ {"Illegal expression": 1 + 2} """,
        """ {"Illegal invocation": alert()} """,
        """ {"Numbers cannot have leading zeroes": 013} """,
        """ {"Numbers cannot be hex": 0x14} """,
        """ ["Illegal backslash escape: \x15"] """,
        """ [\naked] """,
        """ ["Illegal backslash escape: \017"] """,
//        """ [[[[[[[[[[[[[[[[[[[["Too deep"]]]]]]]]]]]]]]]]]]]] """,
        """ {"Missing colon" null} """,

        """ {"Double colon":: null} """,
        """ {"Comma instead of colon", null} """,
        """ ["Colon instead of comma": false] """,
        """ ["Bad value", truth] """,
        """ ['single quote'] """,
        """ ["	tab	character	in	string	"] """,
        """ ["tab\   character\   in\  string\  "] """,
        """ ["line
          break"] """,
        """ ["line\
          break"] """,
        """ [0e] """,
        """ {unquoted_key: "keys must be quoted"} """,
        """ [0e+-1] """,

        """ ["mismatch"} """,
        """ ["extra comma",] """,
        """ ["double extra comma",,] """,
        """ [   , "<-- missing value"] """,
        """ ["Comma after the close"], """,
        """ ["Extra close"]] """,
        """ {"Extra comma": true,} """
      ).map(_.trim())
      val res =
        for(failureCase <- failureCases)
        yield try {
          intercept[ParseException] { read[com.rallyhealth.ujson.v1.Value](failureCase) }
          None
        }catch{
          case _:Throwable =>
          Some(failureCase)
        }

      val nonFailures = res.flatten
      assert(nonFailures.isEmpty)
      intercept[IncompleteParseException]{read[com.rallyhealth.ujson.v1.Value](""" {"Comma instead if closing brace": true, """)}
      intercept[IncompleteParseException]{read[com.rallyhealth.ujson.v1.Value](""" ["Unclosed array" """)}
    }

    test("facadeFailures"){
      def assertErrorMsgDefault[T: com.rallyhealth.upickle.v1.default.Reader](s: String, msgs: String*) = {
        val err = intercept[AbortException] { com.rallyhealth.upickle.v1.default.read[T](s) }
        for (msg <- msgs) assert(err.getMessage.contains(msg))
        err
      }
      test("caseClass"){
        // Separate this guy out because the read macro and
        // the intercept macro play badly with each other

        test("invalidTag"){
          test - assertErrorMsgDefault[Fi.Fo]("""{"$type": "omg"}]""", "invalid tag for tagged object: omg at index 1")
          test - assertErrorMsgDefault[Fi]("""{"$type": "omg"}]""", "invalid tag for tagged object: omg at index 1")
        }

        test("taggedInvalidBody"){
          test - assertErrorMsgDefault[Fi.Fo]("""{"$type": "com.rallyhealth.upickle.v1.Fi.Fo", "i": true, "z": null}""", "expected number got boolean at index 51")
          test - assertErrorMsgDefault[Fi]("""{"$type": "com.rallyhealth.upickle.v1.Fi.Fo", "i": true, "z": null}""", "expected number got boolean at index 51")
        }
      }
    }
    test("compileErrors"){
      compileError("write(new Object)")
      compileError("""read[Object]("")""")
//      compileError("""read[Array[Object]]("")""").msg
      // Make sure this doesn't hang the compiler =/
      compileError("implicitly[com.rallyhealth.upickle.v1.default.Reader[Nothing]]")
    }
  }
}