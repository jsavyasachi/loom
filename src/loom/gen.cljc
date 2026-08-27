(ns ^{:doc "Graph-generating functions"
      :author "Justin Kramer"}
  loom.gen
  (:require [loom.graph :refer [weighted? directed? add-nodes* add-edges* nodes]]))

;; A small xorshift generator keeps seeded output identical on the JVM and JS.
;; The state is kept as a signed 32-bit integer by the platform bit operations.
(defn- next-random [state]
  (let [state (if (zero? state) 1 state)
        state (bit-and (bit-xor state (unsigned-bit-shift-right state 13))
                       0xFFFFFFFF)
        state (bit-and (bit-xor state (bit-shift-left state 17))
                       0xFFFFFFFF)
        state (bit-and (bit-xor state (unsigned-bit-shift-right state 5))
                       0xFFFFFFFF)]
    [state (/ (double (unsigned-bit-shift-right state 0)) 4294967296.0)]))

(defn- seeded-state [seed]
  (bit-or seed 0))

(defn- rand-double [state]
  (next-random state))

(defn- random-int [state n]
  (let [[state value] (rand-double state)]
    [state (mod (long (* value n)) n)]))

#?(:clj (defn- default-seed [] (System/nanoTime))
   :cljs (defn- default-seed [] (.getTime (js/Date.))))

(defn gen-circle
  "Adds num-nodes nodes to graph g and connects each one to out-degree
  nearest neighbors on a ring."
  [g num-nodes out-degree]
  {:pre [(> num-nodes (* out-degree 2))]}
  (let [nodes (range num-nodes)
        edges (for [n nodes
                    d (range 1 (inc out-degree))]
                [n (mod (+ n d) (count nodes))])]
    (-> g
        (add-nodes* nodes)
        (add-edges* edges))))

(defn- add-shortcuts
  "Adds the random shortcut edges of Newman & Watts (1999) to graph g: each
  node gets a shortcut to a random node with probability phi."
  [g phi seed]
  (let [ns (nodes g)
        [_state shortcuts] (reduce (fn [[state shortcuts] n]
                                    (let [[state probability] (rand-double state)]
                                      (if (> phi probability)
                                        (let [[state target] (random-int state (count ns))]
                                          [state (conj shortcuts [n target])])
                                        [state shortcuts])))
                                  [(seeded-state seed) []]
                                  ns)]
    (add-edges* g shortcuts)))

(defn gen-newman-watts
  "Generates a small-world graph as described in Newman & Watts (1999): a ring
  of num-nodes nodes each joined to out-degree neighbors, plus random shortcuts
  added with probability phi. A seed makes the result reproducible."
  ([g num-nodes out-degree phi seed]
   (-> g
       (gen-circle num-nodes out-degree)
       (add-shortcuts phi seed)))
  ([g num-nodes out-degree phi]
   (gen-newman-watts g num-nodes out-degree phi (default-seed))))

(defn gen-barabasi-albert
  "Generates a scale-free graph by preferential attachment (Barabasi & Albert,
  1999): starting from a connected core of m+1 nodes, each new node attaches to
  m existing nodes chosen with probability proportional to their degree. A seed
  makes the result reproducible."
  ([g num-nodes m seed]
   {:pre [(>= m 1) (> num-nodes m)]}
   (let [core-edges (for [i (range m)] [i (inc i)])
         g0 (-> g
                (add-nodes* (range (inc m)))
                (add-edges* core-edges))
         ;; The repeated-node pool has one entry per incident edge endpoint.
         ;; A uniform draw selects a node with probability proportional to its degree.
         pool0 (vec (mapcat identity core-edges))]
      (loop [g g0
            pool pool0
            new (inc m)
            state (seeded-state seed)]
       (if (>= new num-nodes)
         g
         (let [[state targets] (loop [ts #{} state state]
                         (if (>= (count ts) m)
                           [state ts]
                             (let [[state target] (random-int state (count pool))]
                             (recur (conj ts (nth pool target)) state))))]
           (recur (add-edges* g (for [t targets] [new t]))
                  (into pool (concat (repeat m new) targets))
                  (inc new)
                  state))))))
  ([g num-nodes m]
   (gen-barabasi-albert g num-nodes m (default-seed))))

(defn gen-rand
  "Adds num-nodes nodes and approximately num-edges edges to graph g. Nodes
  used for each edge are chosen at random and may be chosen more than once."
  [g num-nodes num-edges & {:keys [min-weight max-weight loops seed]
                            :or {min-weight 1
                                 max-weight 1
                                 loops false
                                 seed (default-seed)}}]
  {:pre [(or (not (weighted? g)) (< min-weight max-weight))]}
  (let [rand-w (fn [state]
                 (let [[state n] (random-int state (- max-weight min-weight))]
                   [state (+ n min-weight)]))
        weighted? (weighted? g)
        nodes (range num-nodes)
        [_state edges] (reduce (fn [[state edges] _]
                                (let [[state n1] (random-int state num-nodes)
                                      [state n2] (random-int state num-nodes)]
                                  (if (or loops (not= n1 n2))
                                    (let [[state edge] (if weighted?
                                                         (let [[state w] (rand-w state)]
                                                           [state [n1 n2 w]])
                                                         [state [n1 n2]])]
                                      [state (conj edges edge)])
                                    [state edges])))
                              [(seeded-state seed) []]
                              (range num-edges))]
    (-> g
        (add-nodes* nodes)
        (add-edges* edges))))

(defn gen-rand-p
  "Adds num-nodes nodes to graph g with probability p for an edge between
  each node."
  [g num-nodes p & {:keys [min-weight max-weight loops seed]
                    :or {min-weight 1
                         max-weight 1
                         loops false
                    seed (default-seed)}}]
  {:pre [(or (not (weighted? g)) (< min-weight max-weight))]}
  (let [rand-w (fn [state]
                 (let [[state n] (random-int state (- max-weight min-weight))]
                   [state (+ n min-weight)]))
        directed? (directed? g)
        weighted? (weighted? g)
        nodes (range num-nodes)
        [_state edges] (reduce (fn [[state edges] [n1 n2]]
                                (let [[state probability] (rand-double state)]
                                  (if (and (if directed?
                                            (or loops (not= n1 n2))
                                            (or (> n1 n2)
                                                (and loops (= n1 n2))))
                                           (> p probability))
                                    (let [[state edge] (if weighted?
                                                         (let [[state w] (rand-w state)]
                                                           [state [n1 n2 w]])
                                                         [state [n1 n2]])]
                                      [state (conj edges edge)])
                                    [state edges])))
                              [(seeded-state seed) []]
                              (for [n1 nodes n2 nodes] [n1 n2]))]
    (-> g
        (add-nodes* nodes)
        (add-edges* edges))))
