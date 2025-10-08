# MemoryLab

## 📌 프로젝트 소개
MemoryLab 추억현상소
"사라져가는 공간을 디지털로 기록하고 공유하는 웹 아카이빙 플랫폼"

## 🚀 주요 기능
- 게시글 업로드 및 조회
- 3D/동영상 자료 업로드
- 댓글 및 커뮤니케이션
- 사용자 인증 및 보안 (Spring Security)

## 🛠️ 기술 스택
- **Backend**: Java 21, Spring Boot, Spring Security, JPA
- **Frontend**: HTML, CSS, Thymeleaf, JavaScript (Three.js + Spark.js 오픈소스 활용)
- **Database**: H2 / MySQL
- **Build**: Gradle

## 📂 프로젝트 구조
src/
├─ main/java/com/memorylab
│ ├─ config
│ ├─ domain
│ ├─ service
│ ├─ repository
│ └─ web
└─ resources
├─ templates
└─ application.yml

## 프론트 표시 규칙
* READY 상태일 때 “3D 보기” 버튼 활성화 → Spark.js 오픈소스를 활용해 PLY를 Three.js 씬에 렌더링.

## 외부 라이브러리 사용
* 3D Gaussian splat 렌더링을 위해 [Spark.js](https://github.com/sparkjsdev/spark) 오픈소스를 활용.
* MIT 라이선스 기반, Three.js와 호환.


## 📅 진행 일정
- 2025.08 ~ 2025.12
