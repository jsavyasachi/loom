(ns loom.test.flow
  (:require [loom.graph :refer (weighted-digraph successors predecessors weight)]
            [loom.flow :refer (edmonds-karp is-admissible-flow?)]
            [loom.alg :refer [max-flow]]
            #?@(:clj [[clojure.test :refer :all]]
                :cljs [cljs.test]))
  #?@(:cljs [(:require-macros [cljs.test :refer (deftest testing are is)])]))

(defn- exception-data [f]
  (try
    (f)
    nil
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core/ExceptionInfo) e
      (ex-data e))))


;; Trivial case
(def g0
  (weighted-digraph
   [:s :t 100]))

;; From Cormen et al. Algorithms, 3 ed. p. 726-727
(def g1
  (weighted-digraph
   [:s :v1 16]
   [:s :v2 13]
   [:v1 :v3 12]
   [:v2 :v1 4]
   [:v2 :v4 14]
   [:v3 :v2 9]
   [:v3 :t 20]
   [:v4 :v3 7]
   [:v4 :t 4]))

;; Source and sink disconnected
(def g2
  (weighted-digraph
   [:s :a 5]
   [:b :t 10]))


(deftest edmonds-karp-test
  (are [max-value network]
       (let [[flow value] (edmonds-karp (successors network)
                                        (predecessors network)
                                        (weight network)
                                        :s :t)]
         (and (= max-value value)
              (is-admissible-flow? flow (weight network)
                                   :s :t)))
       23 g1
       100 g0
       0 g2))


(deftest max-flow-convenience-test
  (are [max-value network]
       (let [[flow value] (max-flow (weighted-digraph network) :s :t)]
         (and (= max-value value)
              (is-admissible-flow? flow (weight network) :s :t)))
       23 g1))

(deftest flow-validation-test
  (testing "max-flow rejects negative capacities"
    (is (= {:type :loom.flow/negative-capacity :edge [:s :t] :capacity -1}
           (exception-data #(max-flow (weighted-digraph [:s :t -1]) :s :t)))))
  (testing "max-flow rejects missing source and sink nodes"
    (is (= {:type :loom.flow/missing-node :node :missing :role :source}
           (exception-data #(max-flow g0 :missing :t))))))
