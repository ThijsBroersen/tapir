package sttp.tapir.examples.client

import cats.effect.{ExitCode, IO, IOApp}
import io.circe.generic.auto.*
import sttp.tapir.*
import sttp.tapir.client.http4s.Http4sClientInterpreter
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

import org.slf4j.{Logger, LoggerFactory}
val logger: Logger = LoggerFactory.getLogger(getClass.getName)

object Http4sClientExample extends IOApp:

  case class User(id: Int, name: String)

  // Define the endpoint that will be interpreted as a request.
  private val userEndpoint =
    endpoint.get
      .in("users" / path[Int]("userId"))
      .out(jsonBody[User])

  // Define http4s routes that will be used to test the request.
  private val http4sRoutes =
    import io.circe.generic.auto.*
    import io.circe.syntax.*
    import org.http4s.*
    import org.http4s.circe.CirceEntityEncoder.*
    import org.http4s.dsl.io.*
    import org.http4s.implicits.*

    HttpRoutes
      .of[IO] { case GET -> Root / "users" / IntVar(userId) =>
        Ok(User(userId, "Joanna").asJson)
      }
      .orNotFound

  override def run(args: List[String]): IO[ExitCode] =
    val userId = 5

    // Interpret the endpoint as a request and a response parser.
    val (userRequest, parseResponse) = Http4sClientInterpreter[IO]().toRequest(userEndpoint, baseUri = None).apply(userId)

    logger.info("Welcome to the http4s client interpreter example!")
    logger.info(s"The following request was derived from the endpoint definition: $userRequest")
    logger.info(s"Now we'll run the request against http4s routes...")

    for {
      response <- http4sRoutes.run(userRequest)
      _ <- IO(logger.info(s"We received the following response: $response"))
      _ <- IO(logger.info(s"Now let's decode the response body using the parser derived from the endpoint definition..."))
      result <- parseResponse(response)
      _ <- IO(logger.info(s"The result is: $result"))
    } yield ExitCode.Success
