package pt.isel.gomoku.http.pipeline

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Record of all requests received and responses sent.
 */
@Component
class LoggerFilter : HttpFilter() {

    override fun doFilter(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val start = System.nanoTime()

        // Before processing the response
        logger.info("|----------------|")
        logger.info("#### REQUEST ####")
        logger.info("# uri = {}", request.requestURI)
        logger.info("# method = {}", request.method)
        logger.info("# user agent header = {}", request.getHeader("User-Agent"))
        logger.info("# authorization header = {}", request.getHeader("Authorization"))
        logger.info("# cookie header = {}", request.getHeader("Cookie"))
        logger.info("# content type header = {}", request.getHeader("Content-Type"))

        chain.doFilter(request, response)

        // After processing the response
        logger.info("# RESPONSE ####")
        logger.info("# status code = {}", response.status)
        logger.info("# content type header = {}", response.getHeader("Content-Type"))

        val end = System.nanoTime()
        val elapsedTimeInSeconds = (end - start) / 1_000_000_000.0

        logger.info("# time to process response = {}", elapsedTimeInSeconds)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(LoggerFilter::class.java)
    }
}
