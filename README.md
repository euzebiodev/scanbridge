# ScanBridge

ScanBridge e um projeto Spring Boot para disponibilizar, via navegador, o scanner instalado em um servidor de impressao Windows ou Linux.

## Recursos

- Entrada simples por e-mail, sem senha.
- Digitalizacoes separadas por usuario.
- Arquivos de cada usuario guardados em um ZIP proprio.
- Miniaturas em cards.
- Visualizador com selecao, arraste e redimensionamento do recorte.
- Exportacao em JPG ou PDF com nome escolhido pelo usuario.

## Como executar

Requisitos no servidor:

- Java 21 instalado.
- Windows: driver WIA do scanner instalado e funcionando.
- Linux: SANE/`scanimage` instalado e reconhecendo o scanner.
- A aplicacao precisa rodar no servidor onde o scanner aparece.

Comandos:

```powershell
.\mvnw.cmd spring-boot:run
```

No Linux:

```bash
./mvnw spring-boot:run
```

Depois acesse:

```text
http://localhost:8080
```

Na rede, substitua `localhost` pelo nome ou IP do servidor de impressao.

## Configuracao

Edite `src/main/resources/application.properties`:

```properties
scanbridge.scanner.output-directory=data/scans
scanbridge.scanner.script-path=scripts/scan-wia.ps1
scanbridge.scanner.sane-script-path=scripts/scan-sane.sh
scanbridge.scanner.device-name=
scanbridge.scanner.timeout-seconds=120
```

Se houver mais de um scanner no servidor, preencha `scanbridge.scanner.device-name`.

Para listar scanners WIA no Windows:

```powershell
.\scripts\list-scanners.ps1
```

Para listar scanners SANE em Linux:

```bash
./scripts/list-scanners.sh
```

## Servico Linux

Em producao, instale o JAR em `/opt/scanbridge` e use `systemd` para manter o servico ativo. Os dados podem ficar em `/var/lib/scanbridge`.
