package net.matthaynes.ner.service

import edu.stanford.nlp.ie.crf._
import edu.stanford.nlp.ie.AbstractSequenceClassifier
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation

import scala.collection.BufferedIterator
import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._

class NamedEntityService {

  val classifier = CRFClassifier.getClassifierNoExceptions("all.3class.distsim.crf.ser.gz")

  def classify(text : String) : NamedEntityContainer = {

    val results   = classifier.classify(text)

    asScalaBuffer(results).foldLeft(new NamedEntityContainer()) {

      (container, sentence) => {

        var tokens = asScalaBuffer(sentence).map(_.asInstanceOf[CoreLabel]).iterator.buffered

        while (tokens.hasNext) {

          getAnnotationType(tokens.head) match {
            case "ORGANIZATION" => container.add(new Organization(getNamedEntityForAnnotation(tokens, "ORGANIZATION")))
            case "LOCATION"     => container.add(new Location(getNamedEntityForAnnotation(tokens, "LOCATION")))
            case "PERSON"       => container.add(new Person(getNamedEntityForAnnotation(tokens, "PERSON")))
            case _              => tokens.next
          }

        }

        container

      }
    }

  }

  private def getNamedEntityForAnnotation(tokens : BufferedIterator[CoreLabel], annotation : String) : String = {
    tokens.takeWhile { s => getAnnotationType(s) == annotation }.map(_.word).mkString(" ")
  }

  private def getAnnotationType(token : CoreLabel) : String = {
    token.get[String, AnswerAnnotation](classOf[AnswerAnnotation])
  }

}

class NamedEntity(val text:String, var frequency:Int = 0)
class Organization(text:String) extends NamedEntity(text)
class Person(text:String) extends NamedEntity(text)
class Location(text:String) extends NamedEntity(text)

class NamedEntityContainer(val entities : HashMap[String, NamedEntity] = new HashMap[String, NamedEntity]) {
  import net.liftweb.json._
  import net.liftweb.json.JsonDSL._

  def add(entity : NamedEntity) = entities.getOrElseUpdate(entity.text, entity).frequency += 1
  def locations     = entities.filter(_._2.isInstanceOf[Location])
  def organizations = entities.filter(_._2.isInstanceOf[Organization])
  def people        = entities.filter(_._2.isInstanceOf[Person])

  def toJson : String = {
    compact(render(
      ("entities" -> (entities.map { case (_, entity) =>
        (("text" -> entity.text) ~ ("type" -> entity.getClass.getSimpleName.toString) ~ ("frequency" -> entity.frequency))
      }))
    ))
  }

}