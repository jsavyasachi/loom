(ns ^{:doc "Defines derived graphs from existing graphs with maps and filters."
      :author "Horst Duchene"}
  loom.derived
  (:require [loom.graph :refer [digraph graph
                                nodes edges successors fly-graph predecessors
                                add-nodes* add-edges*
                                directed?]]
            clojure.set))

(defn mapped-by
  "Returns a Graph or DiGraph with the nodeset (set (map f (nodes g))). An edge
  [uu, vv] is in the result if g has an edge [u, v] where [uu, vv] = [(f u),
  (f v)]."
  [f g]
  (-> (if (directed? g) (digraph) (graph))
      (add-nodes* (map f (nodes g)))
      (add-edges* (map #(map f %) (edges g)))))

(defn subgraph-reachable-from
  "Returns a subgraph of the given graph. It contains all nodes and edges that
  are reachable from the given start node."
  [g start]
  (if (directed? g)
    (fly-graph :start start
               :successors #(successors g %)
               :predecessors #(predecessors g %))
    (fly-graph :start start
               :successors #(successors g %))))

(defn nodes-filtered-by
  "Returns a new graph with all nodes of g that satisfy the predicate."
  [pred g]
  (-> (if (directed? g) (digraph) (graph))
      (add-nodes* (filter pred (nodes g)))
      (add-edges* (filter #(and (pred (first %))
                                (pred (last %)))
                          (edges g)))))
(defn edges-filtered-by
  "Returns a new graph with all nodes of g and edges that satisfy the predicate."
  [pred g]
  (-> (if (directed? g) (digraph) (graph))
      (add-nodes* (nodes g))
      (add-edges* (filter pred (edges g)))))

(defn bipartite-subgraph
  "Returns the subgraph of g that contains only the edge subset E that leads
  outside the given subset. The result contains the start and endpoints of these
  edges. The result is the bipartite graph (U,V,E), where
  U = subset, V = (map last E).
  (see https://en.wikipedia.org/wiki/Bipartite_graph)."
  [g subset]
  (let [ ;; force set semantics
        subset (set subset)
        edges (filter #(and (subset (first %))
                            (not (subset (last %))))
                      (edges g))]
    (-> (if (directed? g) (digraph) (graph))
        (add-nodes* (flatten edges))
        (add-edges* edges))))

(defn surroundings
  "Returns the subgraph of g that contains nodes in the given subset and their
  direct successors."
  [g subset]
  (let [nodes-of-resulting-graph (->> subset
                                      ;; Get all successors of the subset.
                                      (map #(seq (successors g %)))
                                      flatten
                                      ;; Add the subset.
                                      (clojure.set/union (set subset))
                                      ;; Remove nil nodes.
                                      (remove nil?)
                                      ;; Remove duplicates.
                                      set)
        fsuccessors (fn [n] (->> (successors g n)
                                 (filter #(nodes-of-resulting-graph %))))]
    (if (directed? g)
      (fly-graph :nodes nodes-of-resulting-graph
                 :successors fsuccessors
                 :predecessors (fn [n] (->> (predecessors g n)
                                            (filter #(nodes-of-resulting-graph %)))))
      (fly-graph :nodes nodes-of-resulting-graph
                 :successors fsuccessors))))
