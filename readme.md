# OC SSO Query

This Application is created to implement a SSO Query service for OpenConext. With SSO Query, a Service
Provider is able to determine whether a user has a (potential) session with the Federation. Based on this information,
the Service Provider may decide next steps to take in the logging procedure. This is especially convenient to guide
existing users in a different manner than new users, for example guide them through the account creation process. In
cases where the Service Provider allows multiple Identity Providers (or Federations), this functionality may help
assessing the origin of the user, which also helps optimizing the login flow.

The SSO Query service has three types of response:
- TRUE: the user is known to this Federation, and has an active session within this Federation
- REMOTE: the user is known to this Federation through an SSO notification cookie, but does not have an active session currently
- FALSE: no session found for this user within this Federation

To ensure security, the SSO Query checks a whitelist to authorize the destination URL for user redirection following the
query execution. The whitelist can be provided in two ways, controlled by the `application.properties` configuration:

1. **Remote API (Data Services)** — Set `api.endpoint.url` to the URL of an application that returns the whitelist as
   a JSON array of strings (e.g. `["https://example.com", "https://other.org"]`). The service will call this endpoint
   to retrieve the whitelist and cache the result. Optionally, an API key can be configured via `api.key.header.key`
   and `api.key.header.value` to secure the endpoint. A cache-hash URL (`api.cacheHash.url`) can be configured to
   detect upstream changes and invalidate the cache proactively.

2. **Static file** — Leave `api.endpoint.url` empty and set `data.location` to a local JSON file containing the
   whitelist (e.g. `file:/conf/idp.data.json`). The file must contain a JSON array of allowed origin URLs. This
   option is useful when no external Data Services application is available. Example file content:

   ```json
   [
     "https://test.nl",
     "https://vm.openconext.org",
     "https://*.vm.openconext.org"
   ]
   ```

   Wildcard entries (e.g. `https://*.vm.openconext.org`) are supported when `host.wildcard.enabled=true` is set,
   and will match any single subdomain level (e.g. `https://foo.vm.openconext.org`).

In both cases the whitelist is cached in memory and refreshed automatically.

To handle increased load of the OC SSO Query service without interfering with the regular authentication a
microservice is created to handle only the OC SSO Queries.

## Development

For development you can start the Spring Boot application from the root of the project using:

    mvn clean install && ./run.sh

You can access the service by using (for example) Chrome Poster.

- /sso/ssoquery (you'll need to add an "response_url" to the URL query, not the header, e.g.
  ?response_url=https://test.vm.openconext.org). Optionally the JSON flag can also be set (&format=json).
- /actuator/health
- /actuator/info

These example requests can be executed in the browser as following:

- http://localhost:8080/sso/ssoquery?response_url=https%3A%2F%2Ftest.vm.openconext.org&format=json
- http://localhost:8080/sso/ssoquery?response_url=https%3A%2F%2Ftest.vm.openconext.org
- http://localhost:8080/actuator/health
- http://localhost:8080/actuator/info

To make use of the SSO Query microservice, it needs to run on the same domain as the Federation. On a development
environment this can be done by adding a domain name to your hosts file and point it towards the running Spring Boot /
Docker instance. E.g. for MacOS, the following line can be added:

    127.0.0.1 ssoquery.vm.openconext.org

Since the SSO Query service is now able to access the cookies set by the Federation, it can respond based on the
availability and the values of these cookies.

## Installation Manual

[Installation Manual](release/src/site/markdown/docs/installation-manual.md)

## External resources

* <https://developers.wiki.kennisnet.nl/index.php?title=KNF:Hoofdpagina> - Public developers Wiki
