(ns app.main
  {:clj-kondo/config '{:lint-as {reagent.core/with-let clojure.core/let}}}
  (:require [reagent.dom :as rdom]
            [reagent.core :as r]
            [reagent.format :refer [format]]))

(defn get-time [t1 d1 d2]
  (* t1 (Math/pow (/ d2 d1) 1.06)))

(comment
  (get-time (+ 12 (* 60 42)) 10 21)) ; 5559.2493474422945

(defn minutes-from-pace [pace]
  (-> pace
      (rem (* 60 60)) ; Strip the "hour" component
      (/ 60)          ; Divide seconds by 60 for minutes
      Math/floor      ; Floor to nearest minute
      int))           ; Convert to integer

(comment
  ;; these should equal, because it's 5 minutes and an hour and 5 minutes
  (minutes-from-pace 300) ; 5
  (minutes-from-pace (+ (* 60 60) 300))) ; 5

(defn hours-from-pace [pace]
  (-> pace
      (/ (* 60 60))
      Math/floor
      int))

(comment
  (hours-from-pace 300) ; 0
  (hours-from-pace (+ 300 (* 60 60)))) ; 1

(defn seconds-from-pace [pace]
  (-> pace
      (rem 60)
      Math/floor ; Seems reasonable to floor, but could also round
      int))

(defn format-pace [pace]
  (when (not= pace "")
    (let [hours (hours-from-pace pace)
          minutes (minutes-from-pace pace)
          seconds (seconds-from-pace pace)]
      (if (= hours 0)
        (str minutes ":" (format "%02d" seconds))
        (str hours ":" (format "%02d" minutes) ":" (format "%02d" seconds))))))

(comment
  (format-pace 5559))

(defn total-seconds [hours minutes seconds]
  (+ seconds (* 60 minutes) (* 60 60 hours)))

(defn calculator
  []
  (r/with-let [have-run-distance (r/atom "")
               have-run-hours (r/atom 0)
               have-run-minutes (r/atom 0)
               have-run-seconds (r/atom 0)
               want-run-distance (r/atom "")
               pace (r/atom "")]

    [:section
     [:h2 {:class "text-xl"} "Calculator"]

     [:label {:for "have-run-distance"} "I have run ..."]
     [:input {:id "have-run-distance"
              :type "number"
              :class "border"
              :min "0"
               ;; TODO: does the int conversion here truncate?
              :on-change #(reset! have-run-distance (-> % .-target .-value int))}]

     [:fieldset
      [:legend "In the following time"]

      [:select {:id "hours"
                :class "border"
                :on-change #(reset! have-run-hours (-> % .-target .-value int))}
       (for [hour (range 24)]
         [:option {:value hour
                   :key (str "hour-" hour)}
          hour])]
      [:label {:for "hours"} "Hours"]

      [:select {:id "minutes"
                :class "border"
                :on-change #(reset! have-run-minutes (-> % .-target .-value int))}
       (for [minute (range 60)]
         [:option {:value minute
                   :key (str "minute-" minute)}
          minute])]
      [:label {:for "minutes"} "Minutes"]

      [:select {:id "seconds"
                :class "border"
                :on-change #(reset! have-run-seconds (-> % .-target .-value int))}
       (for [second (range 60)]
         [:option {:value second
                   :key (str "second-" second)}
          second])]
      [:label {:for "seconds"} "Seconds"]]

     [:label {:for "want-run-distance"} "I want to run ..."]
     [:input {:id "want-run-distance"
              :type "number"
              :class "border"
              :min "0"
              :on-change #(reset! want-run-distance (-> % .-target .-value int))}]

     [:div
      [:output {:for "pace"} @pace]]

     [:button {:on-click
               #(let [new-pace (format-pace (get-time
                                             (total-seconds @have-run-hours @have-run-minutes @have-run-seconds)
                                             @have-run-distance
                                             @want-run-distance))]
                  (reset! pace new-pace))
               :id "pace"}
      "Calculate Time"]]))

(defn source []
  [:blockquote {:cite "https://www.hillrunner.com/calculators/race-conversion/"}
   [:b "How does this get your predicted time?"]
   [:p "
        The formula used for this calculator is one devised by Pete Riegel in the late 70s.
        It has withstood the test of time as a formula as accurate as any out there for running.
        In the early 80s,
        Riegel refined it for other sports for an article entitled “Athletic Records and Human Endurance” published in American Scientist.
        "]
   [:p "And now, the formula:"]
   [:p [:var "t2"] "=" [:var "t1"] "×" "(" [:var "d2"] "/" [:var "d1"] ")" [:sup "1.06"]]])

(defn pace-table []
  [:section
   [:h2 {:class "text-xl"} "Pace Table"]
   [:table {:class "border w-screen md:w-auto p-0"}
    [:thead {:class "sticky top-0 bg-white"}
     [:tr
      [:th {:class "border"} "1 mile"]
      [:th {:class "border"} "5k"]
      [:th {:class "border"} "10k"]
      [:th {:class "border"} "Half"]
      [:th {:class "border"} "Marathon"]]]
    [:tbody {:class "tabular-nums"}
     (for [pace-per-mile (range 300 600 5)]
       [:tr
        [:td {:class "border text-right"} (format-pace pace-per-mile)]
        [:td {:class "border text-right"} (format-pace (get-time pace-per-mile 1 3.107))]
        [:td {:class "border text-right"} (format-pace (get-time pace-per-mile 1 6.214))]
        [:td {:class "border text-right"} (format-pace (get-time pace-per-mile 1 13.1))]
        [:td {:class "border text-right"} (format-pace (get-time pace-per-mile 1 26.2))]])]]])

(defn app []
  [:main {:class "p-2 md:p-8"}
   [:h1 {:class "text-2xl"} "Pace Calculator"]
   [:a {:href "https://git.sr.ht/~svbn/pace-calculator"
        :class "text-blue-600"} "Source Code"]
   [:br]
   [source]
   [:br]
   [calculator]
   [:br]
   [pace-table]])

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ^:export main! []
  (rdom/render [app] (js/document.querySelector "#app")))
