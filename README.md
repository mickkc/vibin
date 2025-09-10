![Banner](assets/Banner-MD.jpg)

![Tests](https://github.com/mickkc/vibin/actions/workflows/test_server.yml/badge.svg)

Vibin is a self-hosted music streaming server that allows you to upload, manage, and stream your music collection from anywhere.

> [!CAUTION]
> THIS PROJECT IS IN EARLY DEVELOPMENT, NOT YET USABLE, AND LACKS A LOT OF FEATURES AS WELL AS A FRONTEND.

<!-- TOC -->
* [Features](#features)
  * [Basic Features](#basic-features)
  * [Music search language](#music-search-language)
    * [Basic Syntax](#basic-syntax)
      * [Placeholders](#placeholders)
* [Installation](#installation)
  * [Manual Installation](#manual-installation)
  * [Docker](#docker)
  * [Apps](#apps)
* [Usage](#usage)
* [Contributing](#contributing)
* [License](#license)
<!-- TOC -->


# Features

## Basic Features

- Upload and manage your music collection
- Stream music to any device with a web browser or compatible app
- Create and manage playlists
- Search and filter your music library
- Create dynamic playlists based on various criteria

## Music search language

### Basic Syntax

- `multiple words`: Search by artist, album, or title (Placeholders supported)
    - `never gonna give you up`
- `t:word` or `t:"multiple words"`: Search by title (Placeholders supported)
    - `t:"never gonna give you up"`
- `a:word` or `a:"multiple words"`: Search by artist (Placeholders supported)
    - `a:"rick astley"`
- `al:word` or `al:"multiple words"`: Search by album (Placeholders supported)
    - `al:"whenever you need somebody"`
- `p:word` or `p:"multiple words"`: Search by playlist (Placeholders supported)
    - `p:"best of 80s"`
- `y:year` or `y:start-end`: Search by year or range of years
    - `y:1987`
    - `y:1980-1994`
    - `y:-1990` - Up to 1990
    - `y:2000-` - From 2000 onwards
- `e:boolean`: Search by explicit content
    - `e:true`
    - `e:false`
    - `e:yes` - true
    - `e:no` - false
    - `e:1` - true
    - `e:0` - false
- `+tag` or `-tag`: Include or exclude specific tags
    - `+favorite`
    - `-chill`
- `c:word` or `c:"multiple words"`: Search by comment (Placeholders supported)
    - `c:"chill vibes"`
- `d:duration` or `d:min-max`: Search by duration in seconds or range of seconds
    - `d:300` - Exactly 5 minutes
    - `d:180-240` - Between 3 and 4 minutes
    - `d:-120` - Up to 2 minutes
    - `d:600-` - From 10 minutes onwards
- `b:bitrate` or `b:min-max`: Search by bitrate in kbps or range of kbps
    - `b:320` - Exactly 320 kbps
    - `b:128-256` - Between 128 and 256 kbps
    - `b:-192` - Up to 192 kbps
    - `b:320-` - From 320 kbps onwards
- `AND`, `OR`: Combine multiple search criteria
    - `a:"rick astley" AND t:"never gonna give you up"` - All Rick Astley songs titled "Never Gonna Give You Up"
    - `a:"rick astley" AND e:false` - All Rick Astley songs that are not explicit
    - `+favorite AND g:pop AND y:2010-` - All favorite pop songs from 2010 onwards
    - `+chill AND (+lofi OR +jazz) AND more` - All chill lofi or jazz songs that have "more" as their title, artist, or album
    - Precedence: `AND` > `OR`

All searches are case-insensitive.

Escape quotes `"` with a backslash `\"`.

Conditions without a prefix are treated as `AND` by default.

Use parentheses `()` to group conditions and control the order of evaluation.

#### Placeholders

- `%`: Matches any sequence of characters (including an empty sequence)
- `_`: Matches any single character

# Installation

## Manual Installation

## Docker

## Apps

TBD

# Usage

TBD

# Contributing

Not right now.

# License

This project is licensed under the [Vibin' Public License v1.0](LICENSE.md) (VPL-1.0).

