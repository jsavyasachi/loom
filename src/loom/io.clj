(ns ^{:doc "Output and view graphs in various formats"
      :author "Justin Kramer"}
  loom.io
  (:require [loom.graph :refer [directed? weighted? nodes weight src dest]]
            [loom.alg :refer [distinct-edges]]
            [loom.attr :refer [attr? attr attrs]]
            [clojure.string :refer [escape]]
            [clojure.java.io :refer [file]]
            [clojure.java.shell :refer [sh]]))

(defn- dot-esc
  [s]
  (escape s {\" "\\\"" \newline "\\n"}))

(defn- dot-attrs
  [attrs]
  (when (seq attrs)
    (let [sb (StringBuilder. "[")]
      (doseq [[k v] attrs]
        (when (pos? (.length (str v)))
          (when (< 1 (.length sb))
            (.append sb \,))
          (doto sb
            (.append \")
            (.append (dot-esc (if (keyword? k) (name k) (str k))))
            (.append "\"=\"")
            (.append (dot-esc (if (keyword? v) (name v) (str v))))
            (.append \"))))
      (.append sb "]")
      (str sb))))

(defn dot-str
  "Renders graph g as a DOT-format string. It calls (node-label node) and
  (edge-label n1 n2) to get labels for nodes and edges. Weights become edge
  labels unless a label is specified. Labels include attributes when the graph
  satisfies AttrGraph."
  [g & {:keys [graph-name node-label edge-label]
        :or {graph-name "graph"} :as opts }]
  (let [d? (directed? g)
        w? (weighted? g)
        a? (attr? g)
        node-label (or node-label
                       (if a?
                         #(attr g % :label)
                         (constantly nil)))
        edge-label (or edge-label
                       (cond
                         a? #(if-let [a (attr g %1 %2 :label)]
                               a
                               (if w? (weight g %1 %2)))
                         w? #(weight g %1 %2)
                         :else (constantly nil)))
        sb (doto (StringBuilder.
                  (if d? "digraph \"" "graph \""))
             (.append (dot-esc graph-name))
             (.append "\" {\n"))]
    (doseq [k [:graph :node :edge]]
      (when (k opts)
        (doto sb
          (.append (str "  " (name k) " "))
          (.append (dot-attrs (k opts))))))
    (doseq [edge (distinct-edges g)]
      (let [n1 (src edge)
            n2 (dest edge)
            el (edge-label n1 n2)
            eattrs (assoc (if a?
                            (attrs g n1 n2) {})
                     :label el)]
        (doto sb
          (.append (str (hash n1)))
          (.append (if d? " -> " " -- "))
          (.append (str (hash n2))))
        (when (or (:label eattrs) (< 1 (count eattrs)))
          (.append sb \space)
          (.append sb (dot-attrs eattrs)))
        (.append sb "\n")))
    (doseq [n (nodes g)]
      (let [nl (dot-esc (str (or (node-label n) n)))]
        (doto sb
          (.append (str (hash n) " [label=\"" nl "\"]"))))
      (when-let [nattrs (when a?
                          (dot-attrs (attrs g n)))]
        (.append sb \space)
        (.append sb nattrs))
      (.append sb "\n"))
    (str (doto sb (.append "}")))))

(defn dot
  "Writes graph g to f (a string or File) in DOT format. It passes args to dot-str."
  [g f & args]
  (spit (str (file f)) (apply dot-str g args)))

(defn- os
  "Returns :win, :mac, :unix, or nil"
  []
  (condp
      #(<= 0 (.indexOf ^String %2 ^String %1))
      (.toLowerCase (System/getProperty "os.name"))
    "win" :win
    "mac" :mac
    "nix" :unix
    "nux" :unix
    nil))

(defn- open
  "Opens the given file (a string, File, or file URI) in the default application
  for the current desktop environment. Returns nil."
  [f]
  (let [f (file f)]
    ;; java.awt.Desktop has an 'open' method. It hangs on Windows with Clojure
    ;; Box and makes the process a GUI process on Mac OS X.
    (condp = (os)
      :mac (sh "open" (str f))
      :win (sh "cmd" (str "/c start " (-> f .toURI .toURL str)))
      :unix (sh "xdg-open" (str f)))
    nil))

(defn- open-data
  "Writes data (a string or bytes) to a temporary file. The extension can be a
  string or keyword, with or without a dot. Opens the file in the default
  application for that extension. Returns nil."
  [data ext]
  (let [ext (name ext)
        ext (if (= \. (first ext)) ext (str \. ext))
        tmp (java.io.File/createTempFile (subs ext 1) ext)]
    (if (string? data)
      (with-open [w (java.io.FileWriter. tmp)]
        (.write w ^String data))
      (with-open [w (java.io.FileOutputStream. tmp)]
        (.write w ^bytes data)))
    (.deleteOnExit tmp)
    (open tmp)))

(defn render-to-bytes
  "Renders graph g in an image format with GraphViz. Returns data as a byte
  array. GraphViz's 'dot' or a specified algorithm must be in the shell path.
  Algorithms include :dot, :neato, :fdp, :sfdp, :twopi, and :circo. Formats
  include :png, :ps, :pdf, and :svg."
  [g & {:keys [alg fmt] :or {alg "dot" fmt :png} :as opts}]
  (let [dot (apply dot-str g (apply concat opts))
        {:keys [out]} (sh (name alg) (str "-T" (name fmt)) :in dot :out-enc :bytes)]
    out))

(defn view
  "Converts graph g to a temporary image file with GraphViz. Opens the file in
  the default viewer for the current desktop environment. GraphViz's 'dot' or a
  specified algorithm must be in the shell path. Algorithms include :dot,
  :neato, :fdp, :sfdp, :twopi, and :circo. Formats include :png, :ps, :pdf,
  and :svg."
  [g & {:keys [fmt] :or {fmt :png} :as opts}]
    (open-data (apply render-to-bytes g (apply concat opts)) fmt))
