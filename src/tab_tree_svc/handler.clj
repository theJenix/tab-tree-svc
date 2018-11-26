(ns tab-tree-svc.handler
  (:require [clojure.java.io :as io]
            [compojure.core :refer [defroutes routes context GET POST]]
            [cheshire.core :as json]
            [ring.middleware.json :refer [wrap-json-params]]
            [hiccup.middleware :refer [wrap-base-url]]
            [compojure.handler :as handler]
            [compojure.route :as route]
 ;           [clj-datastore.datastore :as d]
;            [clj-datastore.file :refer [local-storage make-file-data-store]]
            [google-apps-clj.credentials :as cred]
            [google-apps-clj.google-drive :as drive]
            

            ))

;; In 3 steps:
;; 1) replicate wholesale load/save from client, and save/load to a simple file
;; 2) Implement clj-datastore back end
;; 3) Incremental updates/reads using change token chain
;; 4) Improve change token/incremental updates

(def nodes-filename "nodes")
;(def nodes-keys [:node-id :parent-id :url :title :description :color :other])
;(def nodes (delay (make-file-data-store nodes-keys nodes-filename local-storage)))

(def creds  
  {:client-id     (System/getenv "TAB_TREE_SVC_GOOGLE_CLIENT_ID") 
   :client-secret (System/getenv "TAB_TREE_SVC_GOOGLE_CLIENT_SECRET")
   :redirect-uris ["URI 1" "URI 2"]
   :auth-map {:access-token  (System/getenv "TAB_TREE_SVC_GOOGLE_ACCESS_TOKEN")
              :expires-in    3600
              :refresh-token (System/getenv "TAB_TREE_SVC_GOOGLE_REFRESH_TOKEN")
              :token-type    "Bearer"}})

(def scopes ["https://www.googleapis.com/auth/drive"
;             "https://docs.google.com/feeds/"
;             "https://spreadsheets.google.com/feeds" 
;             "https://www.googleapis.com/auth/calendar"
             ])

(defn init []
  (println "tab-tree-svc is starting"))

(defn destroy []
  (println "tab-tree-svc is shutting down"))

(defn json-response [data & [status]]
  (when (some? data)
    {:status (or status 200)
     :headers {"Content-Type" "application/json"}
     :body (json/generate-string data)}))

;; To refresh credentials, open a repl and run this.  Needs to be in a repl or some place where we read text and provide input
(defn get-auth []
  (let [auth-map (cred/get-auth-map creds scopes)]
    (println auth-map)))

(defn read-file [fname]
  (if (.exists (io/as-file fname))
    (let [string (slurp fname)
          contents (read-string string)]
      contents)
    {}))

(def path "tab-tree")
(def tab-tree-folder-id (atom nil))

(defn load-tab-tree-folder []
  (let [all-files (drive/execute-query! creds (drive/all-files-query))
        [the-file] (filter (comp #{"tab-tree"} :title) all-files)]
    (when-not the-file
      (throw (ex-info "Unable to find tab-tree folder.  Something's wrong")))
    (reset! tab-tree-folder-id (:id the-file))))

(defn handle-get-all-nodes [req]
  (println "in handle-get-all-notes")
  (when-not @tab-tree-folder-id
    (load-tab-tree-folder))

  (->> (str nodes-filename ".edn")
       vector
       (drive/find-file! creds @tab-tree-folder-id)
       (drive/download-file! creds)
       slurp
       json-response))


(defn string->stream
  ([s] (string->stream s "UTF-8"))
  ([s encoding]
   (-> s
       (.getBytes encoding)
       (java.io.ByteArrayInputStream.))))

(defn handle-post-all-nodes [{{:strs [nodes]} :params :as req}]
  (println "in handle-post-all-notes")
  
  (when-not @tab-tree-folder-id
    (load-tab-tree-folder))
  
  (let [filename (str nodes-filename ".edn")
        {:keys [id] :as existing}
        (->> (str nodes-filename ".edn")
             vector
             (drive/find-file! creds @tab-tree-folder-id))
        stream (string->stream (pr-str nodes))]

    (when-not id
      (throw (ex-info "Unable to find tab-tree/nodes.edn, something is wrong")))

    (->> (drive/file-update-query id {:content stream})
         vector
         (drive/execute! creds))
    ""
    ))

(defroutes app-routes
  (context "/v1" []

    (GET "/nodes/all" req (handle-get-all-nodes req))
    ;(POST "/nodes" {{:keys [change-token]} :params} )
    (POST "/nodes/all" req (handle-post-all-nodes req)))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> (routes app-routes)
      (wrap-json-params)
      (handler/site)
      (wrap-base-url)))
