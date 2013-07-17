(ns db-test
  (:use     [nudge.types]
            [nudge.debug]
            [clojure.test])
  (:require [nudge.store :as store]
            [nudge.convert :as convert]
            [nudge.db :as db]))

(def test1 {:nudge/id   :doc1
            :nudge/type :test1
            :a         1})

(do
  (def testdata (store/memory-store))
  (db/with-db testdata
    (db/write test1)))

(deftest fetch-by-vhash
  (is (= (store/fetch testdata [(vhash test1) :value])
         test1)))

(deftest fetch-by-id
  (is (= (db/with-db testdata
           (db/get-document-by-id :doc1))
         test1)))