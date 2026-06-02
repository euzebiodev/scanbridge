#!/usr/bin/env sh
set -eu

if [ "$#" -ne 1 ]; then
    echo "Informe o caminho do jar gerado."
    exit 2
fi

JAR_PATH="$(readlink -f "$1")"

case "$JAR_PATH" in
    /var/lib/jenkins/jobs/scanbridge-cd/workspace/target/scanbridge-0.0.1-SNAPSHOT.jar|/home/euzebio/Projetos/Repositorios/scanbridge/target/scanbridge-0.0.1-SNAPSHOT.jar)
        ;;
    *)
        echo "Caminho do jar nao permitido: $JAR_PATH"
        exit 2
        ;;
esac

if [ ! -f "$JAR_PATH" ]; then
    echo "Jar nao encontrado: $JAR_PATH"
    exit 2
fi

install -o root -g root -m 0644 "$JAR_PATH" /opt/scanbridge/scanbridge.jar
systemctl restart scanbridge.service
sleep 30
systemctl is-active scanbridge.service
curl -fsS http://127.0.0.1:8080/actuator/health
printf '\n'
