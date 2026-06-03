# Deployment of OC SSO Query Service

For security reasons, cookies can only be set and read on the current resource's top domain and its subdomains, and not
for another domain and its subdomains. Since the Engineblock should be able to access the cookies that the SSO
Notification service places, the service should be hosted on a (sub)domain where the Engineblock application is running.

For example, if Engineblock is running on `engine.vm.openconext.org` then the SSO Query service can be hosted
on a subdomain of `vm.openconext.org` e.g. `ssoquery.vm.openconext.org`. And the domain `vm.openconext.org`
should be configured in the service. Please check the configuration of SSO Query service in
[readme.md](../../../../../readme.md).