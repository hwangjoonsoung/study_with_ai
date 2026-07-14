이제 기록하며 관찰할 것:
conf, 로그, html 루트가 각각 어디 있는가? 이걸 어떻게 알아냈는가? (문서? 검색? — 컨테이너라면 Dockerfile/이미지 문서 한 곳에 있다)
- conf path : /opt/homebrew/etc/nginx 의 nginx.conf
- log path : /opt/homebrew/etc/nginx에 있는 nginx.conf에 의하면 html로 적혀 있음 해당 경로는 상대 경로로 nginx가 설치되어 있는 곳의 html dir를 생성해서 사용. 근데 그러게 해도 안되서 절대경로를 입력하여 Root를 매핑함.
- 결국 conf파일은 nginx root에 있으며 하위 파일들은 conf에서 경로를 변경하여 사용 해야 함.
버전은 뭐가 깔렸는가? 1.24를 깔고 싶었다면? (brew는 특정 버전 고정이 번거롭다 — 이미지 태그 nginx:1.24-alpine 한 줄과 대조)
- 버전이 설치 됨 1.31.2
- brew install nginx만 했기 때문에 최신 버전이 설치 됨 brew install nginx@1.24
Nginx를 하나 더 띄우고 싶다면? (포트 충돌, conf 분리… 컨테이너라면 그냥 서비스 하나 추가)
- conf를 분리하지 않고 nginx.conf에서 server{} 부분을 수정하는 방향으로 해결 가능 할 것같음.
이 Nginx를 흔적 없이 제거하려면 뭘 다 지워야 하는가? brew uninstall 후에도 남는 것은? (docker compose down과 대조)
- /opt/homebrew/etc/nginx에 server를 설정한 파일 전부 남아 있는 문제 발생.

### 직접 설치 했을 때 귀찮았던 점.
1. nginx를 uninstall하는 경우 별도의 파일을 지워야 하는 과정 생김

### 불편한 점이 아니라고 생각되는 부분
1. nginx를 설치하는 과정에서 brew install nginx를 통해 설치 해서 lastest version이 설치 됨. 하지만 특정 버전을 설치할 수 있도록 명령을 수정하면 불편한 점이 아니라고 생각됨.
2. nginx.conf의 내용을 수정하는 것으로 별도의 root를 지정하고 conf, log, root 경로를 수정할 수 있음으로 편할 수 있음.
