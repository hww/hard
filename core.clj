(ns hard.core
  (:require arcadia.core arcadia.linear clojure.string)
  (:import
    [UnityEngine Debug Resources GameObject PrimitiveType 
    Application Color Input Screen Gizmos Camera Component Vector3 Mathf Quaternion]
    ArcadiaState
    Hard.Helper))

(declare position!)

(defn playing? [] (. Application isPlaying))

(defn load-scene [sn] (Application/LoadLevel sn))

(defn loaded-scene [] Application/loadedLevel)

(defn quit [] (Application/Quit))

(defn screen-size [] [(Screen/width)(Screen/height)])

(defn main-camera [] (UnityEngine.Camera/main))

(defn resource [s] (UnityEngine.Resources/Load s))

(defn vector2? [x] (instance? UnityEngine.Vector2 x))
(defn vector3? [x] (instance? UnityEngine.Vector3 x))
(defn vector4? [x] (instance? UnityEngine.Vector4 x))
(defn transform? [x] (instance? UnityEngine.Transform x))
(defn quaternion? [x] (instance? UnityEngine.Quaternion x))
(defn color? [x] (instance? UnityEngine.Color x))
(defn gameobject? [x] (instance? UnityEngine.GameObject x))
(defn component? [x] (instance? UnityEngine.Component x))

(defn- get-or [col idx nf] (or (get col idx) nf))


(def  ->go arcadia.core/gobj)
(defn ->transform [v] (arcadia.core/cmpt v UnityEngine.Transform))

(defmacro >v3 [o]
  `(.position (.transform ~o)))

(defn X [o] (.x (>v3 o)))
(defn Y [o] (.y (>v3 o)))
(defn Z [o] (.z (>v3 o)))


(defn destroy! [o]
  (if (sequential? o)
    (dorun (map #(UnityEngine.Object/Destroy %) o))
    (UnityEngine.Object/Destroy o)))

(defonce CLONED (atom []))
(defonce DATA (atom {}))

(defn clear-cloned! [] 
  (destroy! @CLONED) 
  (reset! CLONED [])
  (reset! DATA {}))

(defmacro clone! [kw]
  (if (keyword? kw)
    (let [source (clojure.string/replace (subs (str kw) 1) #"[:]" "/")]
      `(let [^UnityEngine.GameObject source# (~'UnityEngine.Resources/Load ~source)
             ^UnityEngine.GameObject gob# (~'UnityEngine.GameObject/Instantiate source#)]
        (~'set! (.name gob#) (.name source#))
        (swap! ~'hard.core/CLONED #(cons gob# %))
        gob#))
    `(-clone! ~kw)))


(defn ^UnityEngine.GameObject -clone!
  ([ref] (-clone! ref nil))
  ([ref pos]
    (when (playing?)
      (when-let [^UnityEngine.GameObject source 
                  (cond  (string? ref)  (resource ref)
                         (keyword? ref) (resource (clojure.string/replace (subs (str ref) 1) #"[:]" "/"))
                         :else nil)]

            (let [pos   (or pos (.position (.transform source)))
                  quat  (.rotation (.transform source))
                  ^UnityEngine.GameObject gob   (arcadia.core/instantiate source pos quat)]
              (set! (.name gob) (.name source))
              (swap! CLONED #(cons gob %)) gob)))))


(defn data! 
  ([^UnityEngine.GameObject o v] 
    (swap! DATA assoc o v) o)
  ([^UnityEngine.GameObject o k v] 
    (swap! DATA assoc-in [o k] v) o))

(defn data 
  ([^UnityEngine.GameObject o] 
    (get @DATA o))
  ([^UnityEngine.GameObject o k] 
    (get-in @DATA [o k])))

(defn update-data! 
  ([^UnityEngine.GameObject o f] 
    (swap! DATA update o f) o)
  ([^UnityEngine.GameObject o k f] 
    (swap! DATA update-in [o k] f) o))


(defn state! [^UnityEngine.GameObject o v] 
  (reset! (.state (arcadia.core/ensure-cmpt o ArcadiaState)) v))



(defn color 
  ([col] (if (> (count col) 2) (apply color (take 4 col)) (color 0 0 0 0)))
  ([r g b]   (UnityEngine.Color. r g b 1.0))
  ([r g b a] (UnityEngine.Color. r g b a)))


(defn clamp-v3 [v lb ub]
  (let [lb (float lb) ub (float ub)]
  (Vector3. 
      (Mathf/Clamp (.x v) lb ub)
      (Mathf/Clamp (.y v) lb ub)
      (Mathf/Clamp (.z v) lb ub))))




(defn name! [o s] (set! (.name o) (str s)) o)

(defn ^GameObject parent! [^GameObject a ^GameObject b]
  (set! (.parent (.transform a)) (.transform b)) a)

(defn unparent! ^GameObject [^GameObject child]
  (set! (.parent (.transform child)) nil) child)

(defn world-position [o]
  (when-let [o (->go o)] (.TransformPoint (.transform o) (>v3 o))))

(defn position! [^UnityEngine.GameObject o ^UnityEngine.Vector3 pos]
  (set! (.position (.transform o)) pos) o)

(defn ^UnityEngine.Vector3 
  local-position [^UnityEngine.GameObject o] 
  (.localPosition (.transform o)))

(defn ^UnityEngine.Vector3 
  local-position! [^UnityEngine.GameObject o pos]
  (set! (.localPosition (.transform o)) pos) o)

(defn ^UnityEngine.Vector3 
  local-direction [^GameObject o ^UnityEngine.Vector3  v]
  (.TransformDirection (.transform o) v))

(defn ^UnityEngine.Vector3 
  transform-point [o v]
  (when-let [o (->go o)]
    (.TransformPoint (.transform o) v)))

(defn ^UnityEngine.Vector3 
  inverse-transform-point [o v]
  (when-let [o (->go o)]
    (.InverseTransformPoint (.transform o) v)))

(defn ^UnityEngine.Vector3 
  inverse-transform-direction [o v]
  (when-let [o (->go o)]
    (.InverseTransformDirection (.transform o) v)))

(defn move-towards [v1 v2 step]
  (Vector3/MoveTowards v1 v2 step))

(defn ^UnityEngine.Vector3 lerp [^UnityEngine.Vector3 v1 ^UnityEngine.Vector3 v2 ratio]
  (Vector3/Lerp v1 v2 ratio))

(defn ^UnityEngine.GameObject local-scale [^UnityEngine.GameObject o]
  (when-let [o (->go o)] (.localScale (.transform o) )))

(defn ^UnityEngine.GameObject local-scale! [^UnityEngine.GameObject o ^UnityEngine.Vector3 v]
  (when-let [o (->go o)] (set! (.localScale (.transform o)) v) o))

(defn rotate-around! [o point axis angle]
  (when-let [o (->go o)]
  (. (.transform o) (RotateAround point axis angle))))

(defn rotation [o]
  (when-let [o (->go o)] (.rotation (.transform o) )))

(defn local-rotation [o]
  (when-let [o (->go o)] (.localRotation (.transform o) )))

(defn rotate! ^GameObject [^GameObject o ^Vector3 rot]
  (.Rotate (.transform o) rot) o)

(defn rotation! ^GameObject [^GameObject o ^UnityEngine.Quaternion rot]
  (set! (.rotation (.transform o)) rot) o)

(defn local-rotation! [o ^UnityEngine.Quaternion rot]
  (when-let [o (->go o)]
    (set! (.localRotation (.transform o)) rot)) o)

(defn look-at! 
  ([a b] (.LookAt (->transform a) b))
  ([a b c] (.LookAt (->transform a) b c)))

(defn look-quat [a b]
  (Quaternion/LookRotation (arcadia.linear/v3- b (>v3 a))))

(defn lerp-look! [^UnityEngine.GameObject a b ^double v]
  (let [^UnityEngine.Transform at (.transform a)
        aq (.rotation at)
        lq (look-quat a b)
        res (Quaternion/Lerp aq lq (float v))]
    (set! (.rotation at) res)))


(defn parent-component [thing sym]
  (when-not (string? sym)
    (when-let [gob (->go thing)]
      (.GetComponentInParent gob sym))))

(defn child-component [thing sym]
  (when-not (string? sym)
    (when-let [gob (->go thing)]
      (.GetComponentInChildren gob sym))))

(defn parent-components [thing sym]
  (when-not (string? sym)
    (when-let [gob (->go thing)]
      (.GetComponentsInParent gob sym))))

(defn child-components [thing sym]
  (when-not (string? sym)
    (when-let [gob (->go thing)]
      (.GetComponentsInChildren gob sym))))

(defn child-transforms [go]
  (rest (child-components go UnityEngine.Transform)))


(defn direct-children [go]
  (butlast
    (loop [^UnityEngine.Transform o (->transform go) col '()]
    (if-not (.parent o) (cons o col)
      (recur (.parent o) (cons o col))))))


(defn ^UnityEngine.GameObject child-named [^UnityEngine.GameObject go ^System.String s]
  (Hard.Helper/ChildNamed go s))

(defn ^UnityEngine.GameObject direct-child-named [^UnityEngine.GameObject go ^System.String s]
  (if-let [^UnityEngine.Transform t (.Find (.transform go) s)]
    (.gameObject t)))


;MACROS

(defmacro >v2 [o]
  `(UnityEngine.Vector2. 
    (.x (.position (.transform ~o)))
    (.y (.position (.transform ~o)))))

;@pjago
(defmacro v3map [f & v3s]
  (let [vs (repeatedly (count v3s) #(gensym "v"))
        xs (map #(list '.x %) vs)
        ys (map #(list '.y %) vs)
        zs (map #(list '.z %) vs)]
    `(let [~@(interleave vs v3s)]
        (v3 (~f ~@xs) (~f ~@ys) (~f ~@zs)))))

(defmacro <> [o [f & xs]] `(let [o# ~o] (~f o# ~@xs) o#))

#_(-> (GameObject.)
    (<> (cmpt+ UnityEngine.Rigidbody2D))
    (<> (set-state! :dog 'good))
    (<> (cmpt+ UnityEngine.BoxCollider2D)))

'nasser
(defmacro ∆ [x] `(* UnityEngine.Time/deltaTime ~x))

(defmacro pow [a b] `(UnityEngine.Mathf/Pow ~a ~b))
(defmacro abs [a] `(UnityEngine.Mathf/Abs ~a))
(defmacro sin [a] `(UnityEngine.Mathf/Sin ~a))
(defmacro cos [a] `(UnityEngine.Mathf/Cos ~a))

(defmacro prop* [s]
  `(fn [o#] (~(symbol (str "." s)) o#)))

(defmacro ?f 
  ([] `(~'UnityEngine.Random/value))
  ([n] `(* ~'UnityEngine.Random/value ~n))
  ([a b] `(~'UnityEngine.Random/Range ~a ~b)))

(defmacro ?sphere 
  ([] `(~'UnityEngine.Random/insideUnitSphere))
  ([n] `(arcadia.linear/v3* ~'UnityEngine.Random/insideUnitSphere ~n)))

(defmacro ?circle 
  ([] `(~'UnityEngine.Random/insideUnitCircle))
  ([n] `(arcadia.linear/v2* ~'UnityEngine.Random/insideUnitCircle ~n)))

(defmacro ?on-sphere 
  ([] `(~'UnityEngine.Random/onUnitSphere))
  ([n] `(arcadia.linear/v3* ~'UnityEngine.Random/onUnitSphere ~n)))

(defmacro ?rotation [] `(~'UnityEngine.Random/rotation))

;thanks @nasser
(defmacro foreach [[local-sym arr] & body]
  `(let [len# (.Length ~arr)]
     (loop [i# (int 0)]
       (when (< i# len#)
         (let [~local-sym (aget ~arr i#)]
           ~@body
           (recur (inc i#)))))))

(defmacro ? [& body]
  (if (> 4 (count body))
    `(if ~@body)
    (let [
      body (if (even? (count body)) 
               body
               (concat (butlast body) (list :else (last body))))]
         `(do (prn [~@body])
            (cond ~@body)))))


(defn- un-dot [syms]
  (loop [col syms] 
    (if (> (count col) 2)
      (let [[obj op field] (take 3 col)
            form (cond (= '. op) (list op obj field)
                       (= '> op) (list '.GetComponent obj (str field)))]
        (recur (cons form (drop 3 col))))
        (first col))))

(defmacro .* [& more]
  (let [address (last more)
        matcher (re-matcher #"[\>\.]|[^\>\.]+" (str address))
        broken (loop [col []] (if-let [res (re-find matcher)] (recur (conj col res)) col))
        s-exp (un-dot (concat (butlast more) (map symbol broken)))]
  `(~@s-exp)))


(defn- ->name [m] 
  (or (first (filter #(instance? System.Text.RegularExpressions.Regex %) m))
      (apply str (interpose " " m))))

(defmacro the [& m]
  (let [k (->name m)] 
    (if (string? k) 
      `(arcadia.core/object-named ~k)
      `(first (arcadia.core/objects-named ~k)))))

(defmacro every [& m] `(arcadia.core/objects-named ~(->name m)))


(defn gizmo-color [c] (set! Gizmos/color c))
(defn gizmo-line [^Vector3 from ^Vector3 to]  (Gizmos/DrawLine from to))
(defn gizmo-ray  [^Vector3 from ^Vector3 dir] (Gizmos/DrawRay from dir))
(defn gizmo-cube [^Vector3 v ^Vector3 s] (Gizmos/DrawWireCube v s)) 
(defn gizmo-point 
  ([^Vector3 v] (Gizmos/DrawSphere v 0.075))
  ([^Vector3 v r] (Gizmos/DrawSphere v r)))


(defn material-color! [^UnityEngine.GameObject o ^UnityEngine.Color c] 
  (let [^UnityEngine.Renderer r (.GetComponent o UnityEngine.Renderer)]
    (set! (.color (.material r)) c)))

(defn text! [^UnityEngine.GameObject o ^System.String s] 
  (let [^UnityEngine.TextMesh tm (.GetComponent o UnityEngine.TextMesh)]
    (set! (.text tm) s)))


'(hard.core)

