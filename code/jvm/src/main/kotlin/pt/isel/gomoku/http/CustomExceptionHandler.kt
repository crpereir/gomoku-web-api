package pt.isel.gomoku.http

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import pt.isel.gomoku.http.model.output.Problem

/**
 * Used when unknown errors. Can be either due to user input or due to the server.
 */
@ControllerAdvice
class CustomExceptionHandler : ResponseEntityExceptionHandler() {

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        log.info("Handling MethodArgumentNotValidException: {}", ex.message)
        return Problem.response(
            400,
            Problem.Type.INVALID_REQUEST_CONTENT,
            Problem.Title.INVALID_REQUEST_CONTENT
        )
    }

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        log.info("Handling HttpMessageNotReadableException: {}", ex.message)
        return Problem.response(
            400,
            Problem.Type.INVALID_REQUEST_CONTENT,
            Problem.Title.INVALID_REQUEST_CONTENT
        )
    }

    @ExceptionHandler(
        Exception::class
    )
    fun handleAll(): ResponseEntity<Any> {
        return Problem.response(
            500,
            Problem.Type.INTERNAL_SERVER_ERROR,
            Problem.Title.INTERNAL_SERVER_ERROR,
            Problem.Detail.INTERNAL_SERVER_ERROR
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(CustomExceptionHandler::class.java)
    }
}
