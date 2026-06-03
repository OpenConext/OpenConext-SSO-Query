/*
 * Copyright 2021, Stichting Kennisnet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.kennisnet.services.web.controller;

import net.logstash.logback.marker.Markers;
import nl.kennisnet.services.web.config.CacheConfig;
import nl.kennisnet.services.web.service.WhitelistRestProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.logging.log4j.util.Strings.isEmpty;

/**
 * This controller represent the OC SSO Query service.
 * <p>
 * This service will return a response to user using a redirect with an additional parameter result (or a JSON response)
 * if requested by the user.
 * <p>
 * The 'result' parameter contains with one of these values:
 * <ul>
 * <li>true - If a SSO Session cookie is available.</li>
 * <li>remote - If no SSO Session is available but a SSO Notification cookie is present.</li>
 * <li>false - If true or remote is not the case.</li>
 * </ul>
 */
@RestController
public class SsoQueryController {

    /**
     * Optional request parameter: {@value}, identifying the return type to use json or a redirect.
     */
    public static final String PARAM_FORMAT = "format";

    /**
     * The logger instance for this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SsoQueryController.class);


    /**
     * The logger instance to log the result of SSO Query requests
     */
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("oc-sso-query-eventlogger");

    /**
     * The constant identifying a SSO Notification is set ({@value}).
     */
    private static final String REMOTE_RESULT = "remote";

    /**
     * The value of the format parameter identifying a JSON response should be returned ({@value})
     */
    private static final String JSON_FORMAT = "json";

    /**
     * The name of the parameter ('{@value}') to return the result for.
     */
    private static final String PARAM_RESULT = "result";

    /**
     * The format used to return a JSON response, expecting a string value with the result. ({@value})
     */
    private static final String JSON_RESULT_STRING_FORMAT = "{\"" + PARAM_RESULT + "\":\"%s\"}";

    /**
     * The format used to create the direct url with, expecting three string arguments. ({@value})
     */
    private static final String REDIRECT_URL_FORMAT = "%s%s" + PARAM_RESULT + "=%s";

    /**
     * The http protocol postfix to concatenate the http protocol with the domain.
     */
    private static final String HTTP_PROTOCOL_POSTFIX = "://";

    /**
     * Backup cache in case that the cache could not be update remotely
     */
    private List<String> whiteList = new ArrayList<>();

    /**
     * True if wildcard should be used to match subdomains in the whitelist verification.
     * <p>
     * For example a configured domain www.example.com will be changed to *.example.com which will match any subdomain
     * before example.com, please note this is only applicable to one level.
     */
    @Value("${host.wildcard.enabled}")
    private boolean wildcardEnabled;

    private final CacheConfig cacheConfig;

    private final WhitelistRestProvider whitelistProvider;

    public SsoQueryController(CacheConfig cacheConfig, WhitelistRestProvider whitelistProvider) {
        this.cacheConfig = cacheConfig;
        this.whitelistProvider = whitelistProvider;
    }

    /**
     * Processes the SSO Query request.
     * Verifies that given as parameter 'response_url' is whitelisted.
     * Depending on 'format' parameter value redirected to response url or return current state in json format.
     *
     * @param responseUrl         response url param.
     * @param format              response format.
     * @param sessionCookie       session cookie value need to determine current state.
     * @param notificationCookie  notification cookie value need to determine current state.
     * @param response            HTTP Servlet Response.
     * @return current state in json format or empty string in response redirect case
     * @throws IOException if something goes wrong during redirect
     */
    @GetMapping(value = "/sso/ssoquery", produces = MediaType.APPLICATION_JSON_VALUE)
    public String processSsoQuery(@RequestParam(value = "response_url") String responseUrl,
                                  @RequestParam(required = false) String format,
                                  @CookieValue(value = "sso_id", required = false) String sessionCookie,
                                  @CookieValue(value = "ssonot", required = false) String notificationCookie,
                                  HttpServletRequest request, HttpServletResponse response) throws IOException {

        LOG.debug("Performing 'sso query'. Request sent from IP: " + request.getRemoteAddr());

        URL parsedResponseUrl = verifyResponseUrl(responseUrl);

        // Resolve the state to return
        String state = resolveState(sessionCookie, notificationCookie);

        Marker result = getResult(format, sessionCookie, notificationCookie, parsedResponseUrl, state);
        EVENT_LOGGER.info(result, "Successfully handled OC SSO Query");

        return createStateResponse(format, response, parsedResponseUrl, state);
    }

    /**
     * Create a special logging format for the logback file appender so that it is easier to create statistics.
     *
     * @param format                response format.
     * @param sessionCookie         session cookie value need to determine current state.
     * @param notificationCookie    notification cookie value need to determine current state.
     * @param parsedResponseUrl     response url param.
     * @param state                 the result.
     * @return Marker containing log information of the SSO Query request
     */
    private Marker getResult(String format,
                             String sessionCookie,
                             String notificationCookie,
                             URL parsedResponseUrl,
                             String state)
    {
        Marker result = Markers.append("result", state);
        result.add(Markers.append("response_url", parsedResponseUrl));
        result.add(Markers.append("format", format));
        result.add(Markers.append("sessionCookie", sessionCookie));
        result.add(Markers.append("notificationCookie", notificationCookie));
        return result;
    }

    /**
     * The exception handler for IllegalArgumentException to convert this exception to a BAD_REQUEST status.
     *
     * @param exception the exception which occurred
     * @param response  the servlet response
     * @throws IOException if something went wrong
     */
    @ExceptionHandler(IllegalArgumentException.class)
    void handleIllegalArgumentException(IllegalArgumentException exception, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
    }

    /**
     * @param format            format response format.
     * @param response          current state in json format or empty string in response redirect case
     * @param parsedResponseUrl the validated redirect url
     * @param state             true, false or remote based on the state.
     * @return current state in json format or empty string in response redirect case
     * @throws IOException if something goes wrong during redirect
     */
    private String createStateResponse(String format, HttpServletResponse response, URL parsedResponseUrl, String state)
            throws IOException {

        if (JSON_FORMAT.equalsIgnoreCase(format)) {
            response.setHeader("Vary", "Origin");
            return String.format(JSON_RESULT_STRING_FORMAT, state);
        } else {
            response.sendRedirect(
                    String.format(REDIRECT_URL_FORMAT,
                            parsedResponseUrl,
                            isEmpty(parsedResponseUrl.getQuery()) ? "?" : "&",
                            state)
            );
        }
        return "";
    }

    /**
     * Determine the state to return.
     * <p>
     * See the class description for values and when these are returned.
     *
     * @param sessionCookie         the value of the sessionCookie if present
     * @param notificationCookie    the value of the notification cookie if present
     * @return true, false or remote based on the state.
     */
    private String resolveState(String sessionCookie,
                                String notificationCookie) {
        if (!isEmpty(sessionCookie)) {
            return Boolean.TRUE.toString();
        }
        if (!isEmpty(notificationCookie)) {
            return REMOTE_RESULT;
        }
        return Boolean.FALSE.toString();
    }

    /**
     * Verify if the host of the supplied url is whitelisted.
     * <p>
     * If wildcard is enabled subdomain are also supported (*.example.com).
     *
     * @param   url the url to verify
     * @return the supplied url converted into a URL if the url is whitelisted and formatted well.
     * @throws IllegalArgumentException if the supplied hostname is not whitelisted.
     * @see #wildcardEnabled
     */
    private URL verifyResponseUrl(String url) {
        URL parsedURL;
        try {
            parsedURL = new URL(url);
            List<String> remoteWhiteList = whitelistProvider.getWhitelist();

            // If the remote whitelist is empty, something probably went wrong. Use the "old" cache and discard the
            // new remote cache.
            if (remoteWhiteList.isEmpty()) {
                remoteWhiteList = whiteList;
                cacheConfig.cacheEvict();
            }
            whiteList = remoteWhiteList;

            if (whiteList.isEmpty()) {
                LOG.error("Query data could not be retrieved. Please check if the Data Services application is " +
                        "still running or whether the static whitelist file is not empty.");
                throw new IllegalArgumentException("Query data could not be retrieved.");
            }

            // If no whitelisting record is present at all we won't process the request and still return a
            // a IllegalArgumentException.
            String domain = parsedURL.getProtocol() + HTTP_PROTOCOL_POSTFIX + parsedURL.getHost();
            if (whiteList.contains(domain)) {
                return parsedURL;
            }
            if (wildcardEnabled) {
                // Get all whitelisted patterns that contain a wildcard and replace wildcard with a valid Regex pattern
                List<String> wildcardPatterns = whiteList.stream()
                        .filter(whitelistUrl -> whitelistUrl.contains("*"))
                        .map(u -> u.replace("*.", ".*\\."))
                        .toList();
                for (String wildcardPattern : wildcardPatterns) {
                    String parsedUrlInput = parsedURL.getProtocol() + HTTP_PROTOCOL_POSTFIX + parsedURL.getHost();
                    Matcher matcher = Pattern.compile(wildcardPattern).matcher(parsedUrlInput);
                    if (matcher.matches()) {
                        return parsedURL;
                    }
                }
            }
            LOG.info("Hostname isn't whitelisted: {}", domain);
            throw new IllegalArgumentException("Hostname isn't whitelisted: " + domain);
        } catch (MalformedURLException e) {
            LOG.info("The supplied url {} is not a valid url.", url);
            throw new IllegalArgumentException("The supplied url " + url + " is not a valid url.");
        }
    }

}
