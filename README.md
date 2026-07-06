<div align="center">

![Banner](assets/banner.png)

### Your Personal Music Streaming Server

*Upload, manage, and stream your music collection from anywhere*

![Build Status](https://jks.ndu.wtf/buildStatus/icon?job=vibin-server%2Fmain&subject=Build)
![Lines of Code](https://jks.ndu.wtf/buildStatus/icon?job=vibin-server%2Fmain&subject=Lines%20of%20Code&status=${lineOfCode}&color=blue)
![Code Coverage](https://jks.ndu.wtf/buildStatus/icon?job=vibin-server%2Fmain&subject=Coverage&status=${instructionCoverage}&color=${colorInstructionCoverage})
[![License](https://jks.ndu.wtf/buildStatus/icon?job=vibin-server%2Fmain&subject=License&status=GPLv3&color=teal)](LICENSE)
[![Docker Pulls](https://img.shields.io/docker/pulls/mickkc/vibin)](https://hub.docker.com/r/mickkc/vibin)
![Docker Image Version](https://img.shields.io/docker/v/mickkc/vibin)

</div>

---

> [!CAUTION]
> **Development Status**: This project is actively being developed. Breaking changes may occur without prior notice. Use at your own risk.

<!-- TOC -->
  * [Features](#features)
  * [Quickstart](#quickstart)
  * [Usage](#usage)
  * [License](#license)
<!-- TOC -->


## Features

<table>
<tr>
<td width="50%">

**Core Functionality**
- Upload and manage your music collection
- Stream to any device with a browser or app
- Create and manage playlists
- Advanced search and filtering
- User management and permissions

</td>
<td width="50%">

**Advanced Capabilities**
- Dynamic playlist creation
- Powerful query language
- Tag-based organization
- Lyrics & metadata search support
- Customizable widgets for embedding

</td>
</tr>
</table>

## Quickstart

1. Make sure you have [Docker](https://docs.docker.com/engine/install) and [Docker Compose](https://docs.docker.com/compose/install/)
   installed on your system.
2. Download / copy the [example docker-compose.yml](docker-compose.yml.example) file and the [example .env](.env.example) file and 
   modify them to your needs using the [configuration wiki](https://codeberg.org/mickkc/vibin/wiki/Configuration.md#environment-variables).
3. Run the following command:
    ```bash
    docker compose up -d
    ```
4. You can now access the application at `http://localhost:8080`. (Or the port you specified in the docker-compose.yml file.)
5. Login with the default credentials (username `Admin`, password `admin`)
6. CHANGE THE PASSWORD OF THE ADMIN USER IMMEDIATELY!

> [!NOTE]
> For other installation methods, check out the [configuration wiki page](https://codeberg.org/mickkc/vibin/wiki/Configuration.md).

---

## Usage

To learn more about Vibin', you can check out the [Wiki](https://codeberg.org/mickkc/vibin/wiki) (work in progress).

---

## License

This project is licensed under the **[GNU General Public License v3.0](LICENSE)** (GPLv3).

```
Vibin - Self-hosted music streaming server
Copyright (C) 2025

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
```

