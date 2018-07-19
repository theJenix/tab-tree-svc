(ns tab-tree-svc.handler
  (:require [clojure.java.io :as io]
            [compojure.core :refer [defroutes routes context GET POST]]
            [cheshire.core :as json]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [ring.middleware.json :refer [wrap-json-params]]
            [hiccup.middleware :refer [wrap-base-url]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [clj-datastore.datastore :as d]
            [clj-datastore.file :refer [local-storage make-file-data-store]]

            ))

;; In 3 steps:
;; 1) replicate wholesale load/save from client, and save/load to a simple file
;; 2) Implement clj-datastore back end
;; 3) Incremental updates/reads using change token chain
;; 4) Improve change token/incremental updates

(def nodes-filename "nodes")
(def nodes-keys [:node-id :parent-id :url :title :description :color :other])
(def nodes (delay (make-file-data-store nodes-keys nodes-filename local-storage)))

(defn init []
  (println "tab-tree-svc is starting"))

(defn destroy []
  (println "tab-tree-svc is shutting down"))

(defn json-response [data & [status]]
  (when (some? data)
    {:status (or status 200)
     :headers {"Content-Type" "application/json"}
     :body (json/generate-string data)}))

(defn read-file [fname]
  (if (.exists (io/as-file fname))
    (let [string (slurp fname)
          contents (read-string string)]
      contents)
    {}))

(defn handle-get-all-nodes [req]
  (-> (str nodes-filename ".edn")
      read-file 
      json-response))

(defn handle-post-all-nodes [{{:strs [nodes]} :params :as req}]
  (println "in handle-post-all-notes")
  ;(println req)
  (let [filename (str nodes-filename ".edn")]
    (spit filename nodes)
    ""))

(defroutes app-routes
  (context "/v1" []

    ;(POST "/nodes" {{:keys [change-token]} :params} )

    (GET "/nodes/all" req (handle-get-all-nodes req))
    (POST "/nodes/all" req (handle-post-all-nodes req)))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> (routes app-routes)
      (wrap-json-params)
      (handler/site)
      (wrap-base-url)))
