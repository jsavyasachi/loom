(ns loom.cljs
  (:refer-clojure :exclude (extend)))

;; This namespace provides `extend` for ClojureScript.
;;
;; Loom uses `extend` for protocol implementations of its graph types.
;; ClojureScript provides `extend-type` and `extend-protocol`, but not `extend`.
;; Refactoring all graph type definitions would duplicate protocol methods or
;; delegate to shared protocol functions. The protocol method maps in `loom.graph`
;; are public. Clojure consumers can base graph implementations on these maps.
;;
;; `def-protocol-impls` acts like `def` in Clojure. In ClojureScript, it stores the
;; provided map. This namespace provides `extend` for ClojureScript. It gets the
;; stored maps and uses their contents for an equivalent `extend-type` form.
;;
;; The code resolves `extend` map symbols and uses pattern matching for functions
;; that Loom uses to change protocol method maps (`get-in`, `merge`). If Loom uses
;; other functions to change these maps, change the code below to support them.
;;
;; This is not a general-purpose `extend` replacement. ClojureScript consumers
;; cannot reliably reuse base protocol method maps as Clojure consumers can.

(def ^:private protocol-impls (atom {}))

(defn- resolve-symbol [ns sym]
  (-> ns :name str (symbol (str sym))))

(defmacro def-protocol-impls [name impl-map]
  (if-let [ns (:ns &env)]
    (let [impl-map (reduce
                    (fn [impls [method impl]]
                      (case (first impl)
                        fn (assoc impls method impl)
                        get-in (let [[_ other-impl-map-name path] impl
                                      other-impl-map (@protocol-impls
                                                      (resolve-symbol ns other-impl-map-name))]
                                  (assoc impls method (get-in other-impl-map path)))))
                    {}
                    impl-map)]
      (swap! protocol-impls assoc (resolve-symbol ns name) impl-map)
      nil)
    `(def ~name ~impl-map)))

(defn- resolve-impl-map [env imap]
  (cond
    (map? imap) imap

    (and (seq? imap) (= 'merge (first imap)))
    (apply merge (map #(resolve-impl-map env %) (rest imap)))

    (symbol? imap)
    (@protocol-impls
     (if (namespace imap)
       imap
       (resolve-symbol (:ns env) imap)))
    :else
    (throw (ex-info "Unsupported `extend` impl map"
                    {:impl-map imap}))))

(defmacro extend [type & protocols+impls]
  `(extend-type ~type
         ~@(reduce
             (fn [impls [protocol imap]]
               (let [impl-map (resolve-impl-map &env imap)]
                 (-> (conj impls protocol)
                     (into (map (fn [[method [_ & arities]]]
                                  (cons (symbol (name method))
                                        arities))
                                impl-map)))))
             []
             (partition 2 protocols+impls))))
