(ns index-test
  (:use     [nudge.types]
            [nudge.debug]
            [clojure.test]
            [nudge.query :only [q ? doc]]
            [nudge.store :only [memory-store]])
  (:require [nudge.db :as db]))

