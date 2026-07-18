# 실험 A
## docker ps
```shell
CONTAINER ID   IMAGE            COMMAND                  CREATED          STATUS                 PORTS                                         NAMES
bebff1f829e3   nginx:alpine     "/docker-entrypoint.…"   10 minutes ago   Up 3 seconds           0.0.0.0:8080->80/tcp, [::]:8080->80/tcp       study_with_ai-nginx-1
19fc170e80ae   traefik/whoami   "/whoami"                10 minutes ago   Up 10 minutes          80/tcp                                        study_with_ai-app-b-1
59ff7964b5eb   traefik/whoami   "/whoami"                10 minutes ago   Up 10 minutes          80/tcp                                        study_with_ai-app-a-1
```

## curl -v localhost:8080/a/

```shell
curl -v localhost:8080/a/
* Host localhost:8080 was resolved.
* IPv6: ::1
* IPv4: 127.0.0.1
*   Trying [::1]:8080...
* Connected to localhost (::1) port 8080
> GET /a/ HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/8.7.1
> Accept: */*
>
* Request completely sent off
  < HTTP/1.1 200 OK
  < Server: nginx/1.31.2
  < Date: Wed, 15 Jul 2026 12:35:39 GMT
  < Content-Type: text/plain; charset=utf-8
  < Content-Length: 159
  < Connection: keep-alive
  <
  Hostname: 59ff7964b5eb
  IP: 127.0.0.1
  IP: ::1
  IP: 172.20.0.3
  RemoteAddr: 172.20.0.4:42804
  GET /a/ HTTP/1.1
  Host: app-a
  User-Agent: curl/8.7.1
  Accept: */*

* Connection #0 to host localhost left intact
```
- hostname을 보면 59ff7964b5eb가 받음. container id를 확인해 보면 study_with_ai-app-a-1가 받은 것을 확인 할 수 있음.

# 실험 B
## curl -v localhost:8080/b
```shell
curl -v localhost:8080/b
* Host localhost:8080 was resolved.
* IPv6: ::1
* IPv4: 127.0.0.1
*   Trying [::1]:8080...
* Connected to localhost (::1) port 8080
> GET /b HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/8.7.1
> Accept: */*
>
* Request completely sent off
< HTTP/1.1 404 Not Found
< Server: nginx/1.31.2
< Date: Wed, 15 Jul 2026 12:31:21 GMT
< Content-Type: text/html
< Content-Length: 153
< Connection: keep-alive
<
<html>
<head><title>404 Not Found</title></head>
<body>
<center><h1>404 Not Found</h1></center>
<hr><center>nginx/1.31.2</center>
</body>
</html>
* Connection #0 to host localhost left intact
```

## curl -v localhost:8080/b/
``` shell
curl -v localhost:8080/b/
* Host localhost:8080 was resolved.
* IPv6: ::1
* IPv4: 127.0.0.1
*   Trying [::1]:8080...
* Connected to localhost (::1) port 8080
> GET /b/ HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/8.7.1
> Accept: */*
>
* Request completely sent off
  < HTTP/1.1 404 Not Found
  < Server: nginx/1.31.2
  < Date: Wed, 15 Jul 2026 12:32:03 GMT
  < Content-Type: text/html
  < Content-Length: 153
  < Connection: keep-alive
  <
<html>
<head><title>404 Not Found</title></head>
<body>
<center><h1>404 Not Found</h1></center>
<hr><center>nginx/1.31.2</center>
</body>
</html>
* Connection #0 to host localhost left intact
```

## curl -v localhost:8080/a/b
```shell

curl -v localhost:8080/a/b
* Host localhost:8080 was resolved.
* IPv6: ::1
* IPv4: 127.0.0.1
*   Trying [::1]:8080...
* Connected to localhost (::1) port 8080
> GET /a/b HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/8.7.1
> Accept: */*
>
* Request completely sent off
  < HTTP/1.1 200 OK
  < Server: nginx/1.31.2
  < Date: Wed, 15 Jul 2026 12:32:26 GMT
  < Content-Type: application/octet-stream
  < Content-Length: 16
  < Connection: keep-alive
  <
  prefix-no-regex
* Connection #0 to host localhost left intact
```

## curl -v localhost:8080/a/b/c
```shell

curl -v localhost:8080/a/b/c
* Host localhost:8080 was resolved.
* IPv6: ::1
* IPv4: 127.0.0.1
*   Trying [::1]:8080...
* Connected to localhost (::1) port 8080
> GET /a/b/c HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/8.7.1
> Accept: */*
>
* Request completely sent off
  < HTTP/1.1 200 OK
  < Server: nginx/1.31.2
  < Date: Wed, 15 Jul 2026 12:32:55 GMT
  < Content-Type: application/octet-stream
  < Content-Length: 6
  < Connection: keep-alive
  <
  regex
* Connection #0 to host localhost left intact
```
## curl -v localhost:8080/a/x
```shell

curl -v localhost:8080/a/x
* Host localhost:8080 was resolved.
* IPv6: ::1
* IPv4: 127.0.0.1
*   Trying [::1]:8080...
* Connected to localhost (::1) port 8080
> GET /a/x HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/8.7.1
> Accept: */*
>
* Request completely sent off
  < HTTP/1.1 200 OK
  < Server: nginx/1.31.2
  < Date: Wed, 15 Jul 2026 12:33:18 GMT
  < Content-Type: application/octet-stream
  < Content-Length: 6
  < Connection: keep-alive
  <
  regex
* Connection #0 to host localhost left intact
```

# 실험 C

```shell
/docker-entrypoint.sh: /docker-entrypoint.d/ is not empty, will attempt to perform configuration
/docker-entrypoint.sh: Looking for shell scripts in /docker-entrypoint.d/
/docker-entrypoint.sh: Launching /docker-entrypoint.d/10-listen-on-ipv6-by-default.sh
10-listen-on-ipv6-by-default.sh: info: can not modify /etc/nginx/conf.d/default.conf (read-only file system?)
/docker-entrypoint.sh: Sourcing /docker-entrypoint.d/15-local-resolvers.envsh
/docker-entrypoint.sh: Launching /docker-entrypoint.d/20-envsubst-on-templates.sh
/docker-entrypoint.sh: Launching /docker-entrypoint.d/30-tune-worker-processes.sh
/docker-entrypoint.sh: Configuration complete; ready for start up
2026/07/15 12:34:20 [emerg] 1#1: host not found in upstream "app-c" in /etc/nginx/conf.d/default.conf:5
nginx: [emerg] host not found in upstream "app-c" in /etc/nginx/conf.d/default.conf:5
```

# nginx -t
1. docker container에 nginx가 running 상태
2. nginx.conf 파일을 수정
3. docker exec [nginx container name] nginx -t로 nginx test
   - nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
   - nginx: configuration file /etc/nginx/nginx.conf test is successful
4. docker exec [nginx container name] -s reload

# docker exec [nginx container name] -s reload vs docker container restart [nginx container name]
1. docker restart의 경우 