# ScanBridge

ScanBridge e um projeto Spring Boot para disponibilizar, via navegador, o scanner instalado em um servidor de impressao Windows.

## Como executar

Requisitos no servidor:

- Java 21 instalado.
- Driver WIA do scanner instalado e funcionando no Windows.
- A aplicacao precisa rodar no servidor onde o scanner aparece em `Dispositivos e Impressoras`.

Comandos:

```powershell
.\mvnw.cmd spring-boot:run
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
scanbridge.scanner.device-name=
scanbridge.scanner.timeout-seconds=120
```

Se houver mais de um scanner no servidor, preencha `scanbridge.scanner.device-name` com parte do nome exibido pelo Windows.

Para listar os scanners WIA reconhecidos pelo servidor:

```powershell
.\scripts\list-scanners.ps1
```

## Observacoes importantes

Servidores Windows podem bloquear acesso ao scanner quando a aplicacao roda como servico sem sessao interativa. Primeiro teste com `.\mvnw.cmd spring-boot:run` usando a conta que consegue digitalizar no Windows.

Esta versao inicial nao inclui login. Recomenda-se liberar o acesso apenas na rede interna ou adicionar autenticacao antes de publicar fora da LAN.
