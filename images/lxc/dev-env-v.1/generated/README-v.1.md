# dev-env Version 1

Development environment with Docker-based Jenkins agent and SDKMAN Java management.

## Quick Start
```bash
cd dev-env-v.1/generated
sudo ./launch.sh
```

## Features
- Jenkins Agent: Docker container (no Java in LXC)
- Java: Version 21 via SDKMAN
- Docker: Full support
- Auto-config: Fetches Jenkins params from instance config

## Image
- Name: dev-env:1
- Built: Thu Sep 04 21:42:01 BDT 2025
- Base: debian/12

## Configuration
Edit `sample.conf`:
- `JENKINS_INSTANCE`: Which Jenkins to connect to
- `JENKINS_AGENT_NAME`: Agent name from Jenkins config
- `BIND_MOUNTS`: Project directories to mount
