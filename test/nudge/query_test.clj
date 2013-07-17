(ns query-test
  (:use     [nudge.types]
            [nudge.debug]
            [clojure.test]
            [nudge.query :only [q ? doc]]
            [nudge.store :only [memory-store]])
  (:require [nudge.db :as db]))

(do
  (def testdata (memory-store))

  (db/with-db testdata
    (db/write
     {:nudge/id   :doc1
      :nudge/type :test1
      :a         1
      :b         2}

     {:nudge/id   :doc2
      :nudge/type :test1
      :a         1
      :b         1}

     {:nudge/id   :doc3
      :nudge/type :test2
      :a              {:a 1
                       :b 2}}

     {:nudge/id   :doc4
      :nudge/type :test2
      :a              {:a 1
                       :b 1}}

     {:nudge/id   :doc5
      :nudge/type :double
      :a              {:a 1}
      :b              {:a 2}}

     {:nudge/id   :doc6
      :nudge/type :double
      :a              {:a 1}
      :b              {:a 1}}

     ;; subquery references

     {:nudge/id   :channel1
      :nudge/type :channel}

     {:nudge/id   :msg1
      :nudge/type :message
      :posted-in      (id-ref :channel1)
      :text           "how are you?"}

     {:nudge/id   :msg2
      :nudge/type :message
      :reply-to       (id-ref :msg1)
      :text           "excellent!"}

     {:nudge/id   :msg3
      :nudge/type :message
      :reply-to       (id-ref :msg1)
      :text           "duno really, must have caught something"})))

(do
  (def refdata (memory-store))
  (db/with-db refdata
    (db/write {:nudge/id   :id1
               :nudge/type :id-test
               :a              1}

              {:nudge/id   :id2
               :nudge/type :id-test
               :reference      (id-ref :id1)})))

(deftest simple-queries  
  (is (= (db/with-db testdata
           (q {:nudge/type :test1
               :a              1}))
           #{{:nudge/type :test1, :a 1, :nudge/id :doc1, :b 2}
             {:nudge/type :test1, :a 1, :nudge/id :doc2, :b 1}}))

  (is (= (db/with-db testdata
           (q {:nudge/type :test1
               :b 1}))
         #{{:nudge/type :test1, :a 1, :nudge/id :doc2, :b 1}})))

(deftest variables
  (db/with-db testdata
    (is (= (q {:nudge/type :test1
               :a (? x)
               :b (? x)})
           #{{:nudge/type :test1, :a 1, :nudge/id :doc2, :b 1}}))))

(deftest deep-queries
  (is (=
       (db/with-db testdata
         (q {:nudge/type :test2
             :a              {:b 1}}))
       #{{:nudge/type :test2, :a {:a 1, :b 1}, :nudge/id :doc4}}))
  (is (=
       (db/with-db testdata
         (q {:nudge/type :double
             :a              {:a 1}
             :b              {:a 1}})
         #{{:nudge/id :doc6, :nudge/type :double, :a {:a 1}, :b {:a 1}}}))))

(deftest subqueries
  (is (=
       (db/with-db testdata
         (q {:nudge/type :message
             :nudge/id   (? id)
             :posted-in      (doc :channel1)
             :*replies       {:nudge/type :message
                              :reply-to       (doc (? id))}}))

       #{{:*replies #{{:nudge/id :msg2,
                       :nudge/type :message,
                       :reply-to {:nudge/id :msg1,
                                  :nudge/type :message,
                                  :posted-in {:nudge/type :channel,
                                              :nudge/id :channel1},
                                  :text "how are you?"},
                       :text "excellent!"} 
                      {:nudge/id :msg3,
                       :nudge/type :message,
                       :reply-to {:nudge/id :msg1,
                                  :nudge/type :message,
                                  :posted-in {:nudge/type :channel,
                                              :nudge/id :channel1},
                                  :text "how are you?"},
                       :text "duno really, must have caught something"}},
          :nudge/id :msg1,
          :nudge/type :message,
          :posted-in {:nudge/type :channel, :nudge/id :channel1}, :text "how are you?"}})))

(deftest id-refs
  (is (=
       (db/with-db refdata
         (q {:nudge/id :id2}))
       #{{:nudge/id :id2, 
          :nudge/type :id-test, 
          :reference {:nudge/type 
                      :id-test, :a 1, 
                      :nudge/id :id1}}})))

(run-tests)