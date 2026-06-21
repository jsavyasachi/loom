(ns loom.test.gen
  (:require [clojure.test :refer (deftest testing is are)]
            [loom.graph :refer (graph digraph weighted-graph nodes edges)]
            [loom.gen :refer (gen-circle gen-newman-watts)]))

(deftest gen-circle-test
  (testing "ring structure is deterministic"
    (let [g1 (gen-circle (graph) 5 1)
          g3 (gen-circle (digraph) 6 2)]
      (is (= (set (range 5)) (nodes g1)))
      (is (= #{[0 1] [1 0] [1 2] [2 1] [2 3] [3 2] [3 4] [4 3] [4 0] [0 4]}
             (set (edges g1))))
      (is (= (set (range 6)) (nodes g3)))
      (is (= #{[0 1] [1 2] [2 3] [3 4] [4 5] [5 0] [0 2] [1 3] [2 4] [3 5] [4 0] [5 1]}
             (set (edges g3))))))
  (testing "out-degree must fit"
    (is (thrown? AssertionError (gen-circle (graph) 4 2)))))

(deftest gen-newman-watts-test
  (testing "seeded result is a superset of the ring and is reproducible"
    (let [ring (set (edges (gen-circle (graph) 20 2)))
          a (gen-newman-watts (graph) 20 2 0.5 42)
          b (gen-newman-watts (graph) 20 2 0.5 42)]
      (is (= (set (range 20)) (nodes a)))
      (is (clojure.set/subset? ring (set (edges a))))
      (is (>= (count (edges a)) (count ring)))
      ;; same seed -> identical graph
      (is (= (set (edges a)) (set (edges b)))))))
