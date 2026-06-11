;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.deps.config
  "Functions for reading and writing tool config files."
  (:require
    [clojure.java.io :as jio]
    [clojure.pprint :as pprint]
    [clojure.string :as str]
    [clojure.tools.deps.edn :as edn]
    [rewrite-clj.zip :as z]))

(set! *warn-on-reflection* true)

(defn- location-dir [location]
  (case location
    :user (edn/user-config-dir)
    :project (edn/project-dir)
    (throw (ex-info "location must be :user or :project" {:location location}))))

(defn- validate-lib [lib]
  (when-not (qualified-symbol? lib)
    (throw (ex-info "lib must be a qualified symbol (group/artifact)" {:lib lib}))))

(defn config-file
  "Returns a java.io.File at <location>/.cli-config/<lib-ns>/<lib-name>.edn
  with location defined by :user or :project. The file may not exist."
  ^java.io.File [location lib]
  (validate-lib lib)
  (jio/file (location-dir location) ".cli-config" (namespace lib) (str (name lib) ".edn")))

(defn read-config
  "Returns the value at <location>/.cli-config/<lib-ns>/<lib-name>.edn
  with location defined by :user or :project. Returns nil if the file
  does not exist or is empty."
  [location lib]
  (let [file (config-file location lib)]
    (when (.exists file)
      (with-open [r (jio/reader file)]
        (edn/read-edn r :path (.getPath file))))))

(defn config
  "Returns the merge of :defaults < user config < project config < :overrides
  for the config at <lib-ns>/<lib-name>.edn. Configs are expected to be
  1-level maps with keyword keys."
  [lib & {:keys [defaults overrides]}]
  (merge defaults
         (read-config :user lib)
         (read-config :project lib)
         overrides))

(defn write-config
  "Writes config as EDN to <location>/.cli-config/<lib-ns>/<lib-name>.edn
  with location defined by :user or :project. Overwrites any existing file."
  [location lib config]
  (let [file (config-file location lib)]
    (jio/make-parents file)
    (with-open [w (jio/writer file)]
      (binding [*print-namespace-maps* false]
        (pprint/pprint config w)))))

(defn assoc-config
  "Sets the value at k in <location>/.cli-config/<lib-ns>/<lib-name>.edn
  with location defined by :user or :project. Preserves all existing
  formatting. Creates the file with {k v} if it does not exist or is empty.
  Throws if the file cannot be parsed as a single EDN map."
  [location lib k v]
  (let [file (config-file location lib)
        content (when (.exists file) (slurp file))]
    (if (str/blank? content)
      (write-config location lib {k v})
      (let [zloc (z/of-string content)
            is-map? (= :map (z/tag zloc))
            nothing-after? (nil? (z/right zloc))
            path (.getPath file)]
        (if (and is-map? nothing-after?)
          (spit file (-> zloc (z/assoc k v) z/root-string))
          (throw (ex-info (format "Expected single map in %s" path) {:path path})))))))

(defn data-dir
  "Returns a java.io.File at <location>/.cli-config/<lib-ns>/<lib-name>
  with location defined by :user or :project. The directory may not exist."
  [location lib]
  (validate-lib lib)
  (jio/file (location-dir location) ".cli-config" (namespace lib) (name lib)))

(defn data-file
  "Returns a java.io.File at <location>/.cli-config/<lib-ns>/<lib-name>/<path>
  with location defined by :user or :project and <path> a relative path
  that may be a filename or include sub-segments separated by '/'.
  The file may not exist."
  [location lib path]
  (jio/file (data-dir location lib) path))
