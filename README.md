# tools.deps.config

Per-tool configuration files

# Rationale

Clojure tools need a well-known place to store user and project configuration.
This library defines that place and provides functions for reading and writing
the files stored there.

# Release Information

TBD

# API

Tools are identified by a qualified lib symbol (e.g., `my.org/my-tool`). Tool
config files live under a `.cli-config` directory at one of two locations:

* `:user` - the [user config dir](https://clojure.github.io/tools.deps.edn/#clojure.tools.deps.edn/user-config-dir) shared by all of a user's projects
* `:project` - the [project dir](https://clojure.github.io/tools.deps.edn/#clojure.tools.deps.edn/project-dir) specific to a single project

Under `.cli-config` each tool has two well-known paths: a config file of
settings and a data directory for any other files the tool needs. This
library fully manages the config file. For the data directory, it manages
the location and the tool manages the contents. A tool may use either or
both. The examples below assume:

```clojure
(require '[clojure.tools.deps.config :as dc])
```

## Config

The config file lives at `<location>/.cli-config/<lib-ns>/<lib-name>.edn`
and is expected to contain a 1-level map with keyword keys. The write
functions create files and directories as needed.

### [config](https://clojure.github.io/tools.deps.config/#clojure.tools.deps.config/config)

`(config lib & {:keys [defaults overrides]})` - read and merge in order :defaults < user config < project config < :overrides

```clojure
;; With a user config file of {:color :dark}
;; and a project config file of {:width 120}:
(dc/config 'my.org/my-tool
           :defaults {:color :auto :width 80 :timeout 30 :silent false}
           :overrides {:silent true})
;; => {:color :dark, :width 120, :timeout 30, :silent true}
```

### [read-config](https://clojure.github.io/tools.deps.config/#clojure.tools.deps.config/read-config)

`(read-config location lib)` - read the config file at a single location, returns its value or nil

```clojure
;; The value of <user-config-dir>/.cli-config/my.org/my-tool.edn
(dc/read-config :user 'my.org/my-tool)
;; => {:color :dark}
```

### [write-config](https://clojure.github.io/tools.deps.config/#clojure.tools.deps.config/write-config)

`(write-config location lib config)` - write the config as EDN, overwriting any existing file

```clojure
(dc/write-config :project 'my.org/my-tool {:width 120})
```

### [assoc-config](https://clojure.github.io/tools.deps.config/#clojure.tools.deps.config/assoc-config)

`(assoc-config location lib k v)` - set a single key in the config file, preserving existing formatting and comments

```clojure
;; Persist one setting, creating the file if it does not exist
(dc/assoc-config :user 'my.org/my-tool :color :dark)
```

### [config-file](https://clojure.github.io/tools.deps.config/#clojure.tools.deps.config/config-file)

`(config-file location lib)` - return the config file as a java.io.File

```clojure
(dc/config-file :user 'my.org/my-tool)
;; => java.io.File at <user-config-dir>/.cli-config/my.org/my-tool.edn
```

## Data

For additional needs beyond the config map, each tool has a well-known data
directory at `<location>/.cli-config/<lib-ns>/<lib-name>/`. The directory
can hold any files in any format. The following functions hand you the paths
and the tool is in charge of managing the contents.

### [data-dir](https://clojure.github.io/tools.deps.config/#clojure.tools.deps.config/data-dir)

`(data-dir location lib)` - return the tool's data directory as a java.io.File

```clojure
(dc/data-dir :user 'my.org/my-tool)
;; => java.io.File at <user-config-dir>/.cli-config/my.org/my-tool
```

### [data-file](https://clojure.github.io/tools.deps.config/#clojure.tools.deps.config/data-file)

`(data-file location lib path)` - return a java.io.File at path within the tool's data directory

```clojure
(dc/data-file :user 'my.org/my-tool "prompt.clj")
;; => java.io.File at <user-config-dir>/.cli-config/my.org/my-tool/prompt.clj
```

# Developer Information

* [GitHub project](https://github.com/clojure/tools.deps.config)
* [How to contribute](https://clojure.org/community/contributing)
* [Bug Tracker](https://clojure.atlassian.net/browse/TDEPS)
* [Continuous Integration](https://github.com/clojure/tools.deps.config/actions/workflows/test.yml)

# Copyright and License

Copyright © Rich Hickey and contributors

All rights reserved. The use and
distribution terms for this software are covered by the
[Eclipse Public License 1.0] which can be found in the file
LICENSE at the root of this distribution. By using this software
in any fashion, you are agreeing to be bound by the terms of this
license. You must not remove this notice, or any other, from this
software.

[Eclipse Public License 1.0]: https://opensource.org/license/epl-1-0/
