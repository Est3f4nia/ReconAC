import re


# Patterns table (ignore)
_RAW_PATTERNS: list[tuple[str, str]] = [
    # Server
    (r"Apache[/ ](\d+\.\d+(?:\.\d+)?)",           "cpe:/a:apache:http_server:{v}"),
    (r"nginx[/ ](\d+\.\d+(?:\.\d+)?)",             "cpe:/a:nginx:nginx:{v}"),
    (r"Microsoft-IIS[/ ](\d+\.\d+)",               "cpe:/a:microsoft:internet_information_services:{v}"),
    (r"lighttpd[/ ](\d+\.\d+(?:\.\d+)?)",          "cpe:/a:lighttpd:lighttpd:{v}"),
    (r"Tomcat[/ ](\d+\.\d+(?:\.\d+)?)",            "cpe:/a:apache:tomcat:{v}"),
    (r"Jetty[/ ](\d+\.\d+(?:\.\d+)?)",             "cpe:/a:eclipse:jetty:{v}"),
    (r"OpenSSL[/ ](\d+\.\d+(?:\.\d+)?[a-z]?)",    "cpe:/a:openssl:openssl:{v}"),
    # X-Powered-By
    (r"PHP[/ ](\d+\.\d+(?:\.\d+)?)",               "cpe:/a:php:php:{v}"),
    (r"ASP\.NET[/ ]([\d.]+)",                       "cpe:/a:microsoft:asp.net:{v}"),
    (r"Express[/ ]([\d.]+)",                        "cpe:/a:expressjs:express:{v}"),
    (r"Django[/ ]([\d.]+)",                         "cpe:/a:djangoproject:django:{v}"),
    (r"Laravel[/ ]([\d.]+)",                        "cpe:/a:laravel:laravel:{v}"),
    # X-AspNet-Version — value is just "4.0.30319", no prefix
    (r"^(\d+\.\d+(?:\.\d+)?(?:\.\d+)?)$",          "cpe:/a:microsoft:asp.net:{v}"),
    # X-Generator
    (r"WordPress ([\d.]+)",                         "cpe:/a:wordpress:wordpress:{v}"),
    (r"Drupal ([\d.]+)",                            "cpe:/a:drupal:drupal:{v}"),
    (r"Joomla! ([\d.]+)",                           "cpe:/a:joomla:joomla:{v}"),
]

_PATTERNS = [
    (re.compile(pattern, re.IGNORECASE), template)
    for pattern, template in _RAW_PATTERNS
]

# headers to inspect (in order)
_FINGERPRINT_HEADERS = [
    "Server",
    "X-Powered-By",
    "X-AspNet-Version",
    "X-Generator",
    "Via",
]