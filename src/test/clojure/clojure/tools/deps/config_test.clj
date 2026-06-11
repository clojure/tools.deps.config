(ns clojure.tools.deps.config-test
  (:require
    [clojure.java.io :as jio]
    [clojure.test :refer [deftest is]]
    [clojure.tools.deps.config :as sut]
    [clojure.tools.deps.util.dir :as dir])
  (:import
    [java.io File]
    [java.nio.file Files]
    [java.nio.file.attribute FileAttribute]))

(def ^:private base (.getCanonicalFile (File. ".")))

(deftest test-data-dir-builds-expected-path
  (let [actual (dir/with-dir base (sut/data-dir :project 'my.test/example-tool))
        expected (File. base ".cli-config/my.test/example-tool")]
    (is (= expected actual))))

(deftest test-data-file-builds-expected-path
  (let [actual (dir/with-dir base (sut/data-file :project 'my.test/example-tool "prompt.clj"))
        expected (File. base ".cli-config/my.test/example-tool/prompt.clj")]
    (is (= expected actual))))

(deftest test-config-file-builds-expected-path
  (let [actual (dir/with-dir base (sut/config-file :project 'my.test/example-tool))
        expected (File. base ".cli-config/my.test/example-tool.edn")]
    (is (= expected actual))))

(deftest test-read-config-returns-nil-for-missing-file
  (let [actual (dir/with-dir base (sut/read-config :project 'my.test/example-tool))]
    (is (nil? actual))))

(deftest test-config-no-args-returns-nil
  (dir/with-dir base (is (nil? (sut/config 'my.test/example-tool)))))

(deftest test-config-merges-defaults-and-overrides
  (let [actual (dir/with-dir base (sut/config 'my.test/example-tool
                                              :defaults {:a 1 :b 2}
                                              :overrides {:a 2}))
        expected {:a 2 :b 2}]
    (is (= expected actual))))

(deftest test-write-config-round-trip
  (let [tmp (.toFile (Files/createTempDirectory "cli-config-test" (into-array FileAttribute [])))
        expected {:a 1 :b "two"}
        actual (dir/with-dir tmp
                 (sut/write-config :project 'my.test/example-tool expected)
                 (sut/read-config :project 'my.test/example-tool))]
    (is (= expected actual))))

(deftest test-assoc-config-creates-file-when-missing
  (let [tmp (.toFile (Files/createTempDirectory "cli-config-test" (into-array FileAttribute [])))
        actual (dir/with-dir tmp
                 (sut/assoc-config :project 'my.test/example-tool :a 1)
                 (sut/read-config :project 'my.test/example-tool))
        expected {:a 1}]
    (is (= expected actual))))

(deftest test-assoc-config-preserves-formatting
  (let [tmp (.toFile (Files/createTempDirectory "cli-config-test" (into-array FileAttribute [])))
        file (dir/with-dir tmp (sut/config-file :project 'my.test/example-tool))
        original ";; highly customized\n{:a 1\n ;; the custom b setting\n :b 2}\n"
        expected ";; highly customized\n{:a 42\n ;; the custom b setting\n :b 2}\n"]
    (jio/make-parents file)
    (spit file original)
    (dir/with-dir tmp (sut/assoc-config :project 'my.test/example-tool :a 42))
    (is (= expected (slurp file)))))

(deftest test-assoc-config-adds-new-key
  (let [tmp (.toFile (Files/createTempDirectory "cli-config-test" (into-array FileAttribute [])))
        file (dir/with-dir tmp (sut/config-file :project 'my.test/example-tool))
        expected {:a 1 :b 2}]
    (jio/make-parents file)
    (spit file "{:a 1}")
    (dir/with-dir tmp (sut/assoc-config :project 'my.test/example-tool :b 2))
    (is (= expected (dir/with-dir tmp (sut/read-config :project 'my.test/example-tool))))))

(deftest test-validate-lib-rejects-unqualified-symbol
  (let [expected-msg #"^lib must be a qualified symbol \(group/artifact\)$"]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo expected-msg (#'sut/validate-lib 'tools.repl)))))

(deftest test-data-dir-rejects-unknown-location
  (let [expected-msg #"^location must be :user or :project$"]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo expected-msg (sut/data-dir :do-what-now 'my.test/example-tool)))))
