(ns nexus.keygen
  "Command-line utility for generating and writing cryptographic keys."
  (:require [nexus.crypto :as crypto]
            [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log])
  (:gen-class))

(def cli-opts
  "Command-line options for the key generation utility."
  [["-a" "--algorithm ALGO" "Algorithm key to generate." :default "HmacSHA512"]
   ["-s" "--seed SEED"      "Seed used to generate key."]
   ["-h" "--help"]
   ["-v" "--verbose" "Enable verbose logging."]])

(defn- usage
  "Generates a usage message for the command-line utility.
  Optionally includes error messages."
  ([summary] (usage summary []))
  ([summary errors] (->> (concat errors
                                 ["usage: nexus-generate-key [opts] <FILENAME>"
                                  ""
                                  "Options:"
                                  summary])
                         (str/join \newline))))

(defn- msg-quit [status msg]
  "Prints a message and exits the program with the given status code."
  (println msg)
  (System/exit status))

(defn- write-key [{:keys [key filename]}]
  "Writes the encoded key to the specified filename."
  (log/debug "Writing key to file:" {:filename filename})
  (try
    (with-open [file (io/writer filename)]
      (.write file (crypto/encode-key key)))
    (catch Exception e
      (throw (ex-info "Failed to write key to file" {:filename filename} e)))))

(defn- gen-key [{:keys [algorithm seed]}]
  "Generates a cryptographic key using the specified algorithm and optional seed."
  (try
    (if seed
      (crypto/generate-key algorithm seed)
      (crypto/generate-key algorithm))
    (catch Exception e
      (throw (ex-info "Failed to generate key" {:algorithm algorithm :seed seed} e)))))

(defn -main [& args]
  "Main entry point for the command-line utility. Parses arguments and generates a key file."
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-opts)]
    (when (:verbose options) (log/info "Verbose logging enabled"))
    (when (seq errors) (msg-quit 1 (usage summary errors)))
    (when (:help options) (msg-quit 0 (usage summary)))
    (when (not (= 1 (count arguments))) (msg-quit 1 (usage summary ["missing required paramater FILENAME"])))
    (-> options
        (assoc :filename (first arguments))
        (assoc :key      (gen-key options))
        (write-key))))
