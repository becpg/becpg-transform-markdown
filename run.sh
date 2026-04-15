#!/bin/bash

echo -e "\e[38;2;0;255;189m888                 \e[38;2;0;92;102m.d8888b.  8888888b.   .d8888b. \e[38;2;0;255;189m" 
echo -e "888                \e[38;2;0;92;102md88P  Y88b 888   Y88b d88P  Y88b\e[38;2;0;255;189m" 
echo -e "888                \e[38;2;0;92;102m888    888 888    888 888    888\e[38;2;0;255;189m" 
echo -e "88888b.   .d88b.   \e[38;2;0;92;102m888        888   d88P 888       \e[38;2;0;255;189m" 
echo -e "888 \"88b d8P  Y8b  \e[38;2;0;92;102m888        8888888P\"  888  88888\e[38;2;0;255;189m" 
echo -e "888  888 88888888  \e[38;2;0;92;102m888    888 888        888    888\e[38;2;0;255;189m" 
echo -e "888 d88P Y8b.      \e[38;2;0;92;102mY88b  d88P 888        Y88b  d88P\e[38;2;0;255;189m" 
echo -e "88888P\"   \"Y8888    \e[38;2;0;92;102m\"Y8888P\"  888         \"Y8888P88\e[0m" 
echo -e " \e[91mCopyright (C) 2010-2026 beCPG.\e[0m"

export COMPOSE_FILE_PATH=${PWD}/target/docker-compose.yml
export MVN_EXEC="${PWD}/mvnw"

start() {
   	docker compose  -f $COMPOSE_FILE_PATH  up -d --remove-orphans
}

down() {
    docker compose -f $COMPOSE_FILE_PATH down

}

purge() {
    docker compose  -f  $COMPOSE_FILE_PATH down -v
}

build() {
  	 $MVN_EXEC clean package $EXTRA_ENV -DskipTests=true
  	 # Build with Docker BuildKit for optimal layer caching
  	 DOCKER_BUILDKIT=1 docker compose -f $COMPOSE_FILE_PATH build
}

# Build specific stage for debugging
build_stage() {
  	 local stage=$1
  	 $MVN_EXEC clean package $EXTRA_ENV -DskipTests=true
  	 DOCKER_BUILDKIT=1 docker build -f target/Dockerfile \
  	     --target $stage \
  	     -t becpg-transform-markdown:$stage \
  	     target/
}

# Show image size and layer analysis
analyze() {
  	 echo "=== Docker Image Analysis ==="
  	 docker images becpg/becpg-transform-markdown --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
  	 echo ""
  	 echo "=== Image Layers ==="
  	 docker history becpg/becpg-transform-markdown:latest
}

tail() {
    docker compose  -f $COMPOSE_FILE_PATH logs -f --tail=100 
}

case "$1" in
  build_start)
    build
    start
    tail
    ;;
  start)
    start
    tail
    ;;
  stop)
    down
    ;;
  build)
    build
    ;;
  build_stage)
    build_stage $2
    ;;
  analyze)
    analyze
    ;;
  purge)
    purge
    ;;
  tail)
    tail
    ;;
  *)
    echo "Usage: $0 {build|build_stage <stage>|analyze|build_start|start|stop|purge|tail}"
esac
