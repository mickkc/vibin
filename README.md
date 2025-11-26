<div align="center">

![Banner](assets/Banner-MD.jpg)

### Your Personal Music Streaming Server

*Upload, manage, and stream your music collection from anywhere*

[![Build Status](http://jks.ndu.wtf/buildStatus/icon?job=vibin-server%2Fmain&subject=Build)](https://jks.ndu.wtf/job/vibin-server/job/main/)
[![Lines of Code](https://jks.ndu.wtf/buildStatus/icon?job=vibin-server%2Fmain&subject=Lines%20of%20Code&status=${lineOfCode}&color=blue)](https://jks.ndu.wtf/job/vibin-server/job/main/)
[![Code Coverage](https://jks.ndu.wtf/buildStatus/icon?job=vibin-server%2Fmain&subject=Coverage&status=${instructionCoverage}&color=${colorInstructionCoverage})](https://jks.ndu.wtf/job/vibin-server/job/main/)
[![License](https://jks.ndu.wtf/buildStatus/icon?job=vibin-server%2Fmain&subject=License&status=GPLv3&color=teal)](LICENSE)

</div>

---

> [!CAUTION]
> **Development Status**: This project is actively being developed. Breaking changes may occur without prior notice. Use at your own risk.

<!-- TOC -->
  * [Features](#features)
  * [Music Search Language](#music-search-language)
    * [Search Operators](#search-operators)
      * [Metadata Search](#-metadata-search)
      * [Filters](#-filters)
      * [Advanced Search](#-advanced-search)
      * [Logical Operators](#-logical-operators)
    * [Wildcards](#wildcards)
  * [Installation](#installation)
    * [Manual Installation](#manual-installation)
    * [Docker](#docker)
    * [Apps](#apps)
  * [Usage](#usage)
  * [Contributing](#contributing)
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

## Music Search Language

Vibin features a powerful query language for precise music searching. All searches are **case-insensitive**.

### Search Operators

#### Metadata Search

| Operator         | Description                    | Example                           |
|------------------|--------------------------------|-----------------------------------|
| `multiple words` | Search artist, album, or title | `never gonna give you up`         |
| `t:`             | Search by **title**            | `t:"never gonna give you up"`     |
| `a:`             | Search by **artist**           | `a:"rick astley"`                 |
| `al:`            | Search by **album**            | `al:"whenever you need somebody"` |
| `p:`             | Search by **playlist**         | `p:"best of 80s"`                 |
| `c:`             | Search by **comment**          | `c:"chill vibes"`                 |

#### Filters

| Operator | Description         | Examples                                                                             |
|----------|---------------------|--------------------------------------------------------------------------------------|
| `y:`     | Year or year range  | `y:1987` or `y:1980-1994`<br>`y:-1990` (up to 1990)<br>`y:2000-` (from 2000)         |
| `d:`     | Duration in seconds | `d:300` (exactly 5 min)<br>`d:180-240` (3-4 min)<br>`d:-120` (up to 2 min)           |
| `b:`     | Bitrate in kbps     | `b:320` (exactly 320 kbps)<br>`b:128-256` (128-256 kbps)<br>`b:320-` (from 320 kbps) |
| `e:`     | Explicit content    | `e:true` / `e:yes` / `e:1`<br>`e:false` / `e:no` / `e:0`                             |
| `l:`     | Has lyrics          | `l:true` / `l:yes` / `l:1`<br>`l:false` / `l:no` / `l:0`                             |

#### Advanced Search

| Operator | Description               | Example                        |
|----------|---------------------------|--------------------------------|
| `lc:`    | Search **lyrics content** | `lc:"never gonna give you up"` |
| `+tag`   | Include tag               | `+favorite`                    |
| `-tag`   | Exclude tag               | `-chill`                       |

#### Logical Operators

Combine search criteria with `AND` and `OR` operators:

```
a:"rick astley" AND t:"never gonna give you up"
a:"rick astley" AND e:false
+chill AND (+lofi OR +jazz) AND more
```

**Operator Precedence:** `AND` > `OR`

> **Note:** Use parentheses `()` to group conditions and control evaluation order.
> Conditions without a prefix are treated as `AND` by default.
> Escape quotes with backslash: `\"`

### Wildcards

| Wildcard | Description                        | Example                                                  |
|----------|------------------------------------|----------------------------------------------------------|
| `%`      | Matches any sequence of characters | `t:"never%you%"` matches "never gonna give you up"       |
| `_`      | Matches any single characte r      | `a:"rick_astley"` matches "rick astley" or "rick-astley" |

---

## Installation

### Manual Installation

> Coming soon

### Docker

> Coming soon

### Apps

> Coming soon

---

## Usage

> Documentation in progress

---

## Contributing

This project is not currently accepting contributions.

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

