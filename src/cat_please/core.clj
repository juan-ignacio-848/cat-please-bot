(ns cat-please.core
  (:require [morse.api :as t]
            [morse.handlers :as h]
            [morse.polling :as p]
            [clojure.java.io :as io]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [environ.core :refer [env]]))

(defonce token (env :cat-please-bot))

(def telegram-base-url "https://api.telegram.org/bot")
(def base-cat-api-query "https://api.thecatapi.com/v1/images/search")

(defn cat-query [limit]
  (str base-cat-api-query "?limit=" limit))

(defn cat-urls [query]
  (let [response (-> (http/get query)
                     :body
                     (json/parse-string true))]
    (vec (map :url response))))

(defonce cats (cat-urls (cat-query 100)))

(defn random-cat-url []
  (rand-nth cats))

(defn send-photo-url [token chat-id url]
  (http/post (str telegram-base-url token "/sendPhoto") {:content-type :json
                                                         :as           :json
                                                         :form-params  {:chat_id chat-id
                                                                        :photo   url}}))

(h/defhandler bot-api

              (h/command-fn "start" (fn [{{id :id :as chat} :chat}]
                                      (println "Bot joined new chat: " chat)
                                      (t/send-text token id "Hi, type /cat every time you need a cat.")))


              (h/command "help" {{id :id :as chat} :chat}
                         (println "Help was requested in " chat)
                         (t/send-text token id "Click or type this: /cat"))

              (h/command-fn "cat" (fn [msg]
                                    (let [id (get-in msg [:chat :id])]
                                      (send-photo-url token id (random-cat-url)))))


              (h/message message (println "Intercepted message:" message)))

(def channel (p/start token (var bot-api)))

(comment
  (p/stop channel))