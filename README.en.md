# ScanBridge

ScanBridge is a Spring Boot project that exposes a scanner installed on a Windows or Linux print server through a web interface.

## Features

- Simple e-mail based access, without password.
- Scans separated by user.
- Each user's scans stored in a dedicated ZIP archive.
- Scan listing as thumbnail cards.
- Viewer with selectable, movable, and resizable crop area.
- Export to JPG or PDF with a user-defined document name.

## Running

Server requirements:

- Java 21.
- Windows: working WIA scanner driver.
- Linux: SANE/`scanimage` installed and detecting the scanner.
- The application must run on the server where the scanner is installed.

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux:

```bash
./mvnw spring-boot:run
```

Open:

```text
http://localhost:8080
```

From the network, replace `localhost` with the print server name or IP address.

## Configuration

Edit `src/main/resources/application.properties`:

```properties
scanbridge.scanner.output-directory=data/scans
scanbridge.scanner.script-path=scripts/scan-wia.ps1
scanbridge.scanner.sane-script-path=scripts/scan-sane.sh
scanbridge.scanner.device-name=
scanbridge.scanner.timeout-seconds=120
```

If the server has more than one scanner, set `scanbridge.scanner.device-name`.

List WIA scanners on Windows:

```powershell
.\scripts\list-scanners.ps1
```

List SANE scanners on Linux:

```bash
./scripts/list-scanners.sh
```

## Linux Service

For production, install the JAR under `/opt/scanbridge` and run it with `systemd`. Application data can be stored under `/var/lib/scanbridge`.
