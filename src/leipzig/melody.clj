(ns leipzig.melody
  (:use [leipzig.scale]))

(defn bpm
  "Returns a function that translates a beat number into milliseconds.
  e.g. ((bpm 90) 5)" 
  [beats] (fn [beat] (-> beat (/ beats) (* 60) (* 1000))))

(defn rhythm 
  "Translates a sequence of durations into a rhythm.
  e.g. (rhythm [1 1 2])"
  [durations]
  (let [times (reductions + 0 durations)]
    (map #(zipmap [:time :duration] [%1 %2]) times durations)))

(defn having
  "Zips an arbitrary quality onto a melody.
  e.g. (->> (rhythm [1 1/2]) (having :drum [:kick :snare]))"
  [k values notes]
  (map #(assoc %1 k %2) notes values))

(def is
  "Synonym for constantly.
  e.g. (->> notes (wherever (comp not :part), :part (is :bass)))" 
  constantly)

(defn all
  "Set key k to value v for all notes.
  e.g. (->> notes (all :part :piano))"
  [k v notes] (map #(assoc % k v) notes))

(defn wherever
  "Applies f to the k key of each note wherever condition? holds. 
  e.g. (->> notes (wherever (comp not :part), :part (is :piano))"
  [condition? k f notes]
  (map
    (fn [note] (if (condition? note)
                 (update-in note [k] f)
                 note))
    notes))

(defn where
  "Applies f to the k key of each note in notes. Leaves notes without
  a k key untouched.
  e.g. (->> notes (where :time (bpm 90)))"
  [k f notes]
  (wherever k, k f notes))

(defn phrase
  "Translates a sequence of durations and pitches into a melody.
  nil pitches signify rests.
  e.g. (phrase [1/2 1/2 1/2 3/2 1/2 1/2 1/2] [0 1 2 nil 4 4/5 5])" 
  [durations pitches]
  (->> (rhythm durations)
       (having :pitch pitches)
       (wherever (comp not :pitch), :rest (is true))))

(defn after
  "Delay notes by wait.
  e.g. (->> melody (after 3))"
  [wait notes] (where :time (from wait) notes))

(defn before? [a b] (< (:time a) (:time b)))

(defn with
  "Accompanies two melodies with each other.
  e.g. (->> melody (with bass))"
  [[a & other-as :as as] [b & other-bs :as bs]]
  (cond
    (empty? as) bs
    (empty? bs) as
    (before? b a) (cons b (lazy-seq (with as other-bs)))
    :otherwise    (cons a (lazy-seq (with bs other-as)))))

(defn duration [notes]
  (->> notes
       (map (fn [{t :time d :duration}] (+ t d)))
       (reduce max)))

(defn then 
  "Sequences later after earlier. 
  e.g. (->> call (then response))"
  [later earlier]
  (->> earlier
       (with
         (->> later (after (duration earlier))))))

(defn times
  "Repeats notes n times.
  e.g. (->> bassline (times 4))"
  [n notes] (reduce then (repeat n notes)))
