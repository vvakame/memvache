Travis-CI (master) [![Build Status](https://secure.travis-ci.org/vvakame/memvache.png?branch=master)](http://travis-ci.org/vvakame/memvache)

Travis-CI (develop) [![Build Status](https://secure.travis-ci.org/vvakame/memvache.png?branch=develop)](http://travis-ci.org/vvakame/memvache)


# Memvache #

Memvache is a reduce pricing library for GAE/J. licensed under Apache License 2.0.

---

## How to use ##

Please add this settings.

*web.xml*

	<filter>
		<filter-name>memvache</filter-name>
		<filter-class>net.vvakame.memvache.MemvacheFilter</filter-class>
	</filter>
	<filter-mapping>
		<filter-name>memvache</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>

## Alghorithm ##

[For the Japanese](https://github.com/vvakame/memvache/wiki)

