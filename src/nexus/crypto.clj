(ns nexus.crypto
  "Cryptographic utilities for key generation, encoding, and signature operations."
  (:require [clojure.tools.logging :as log])
  (:import java.security.SecureRandom
           (javax.crypto Mac KeyGenerator)
           (javax.crypto.spec SecretKeySpec)
           java.util.Base64))

(def ^:private key-generator-thread-local
  "Thread-local storage for KeyGenerator instances to ensure thread safety."
  (ThreadLocal.))

(defn- generate-key-impl [algo rng]
  "Generates a cryptographic key using the specified algorithm and random number generator (rng)."
  (log/debug "Generating key with algorithm:" algo)
  (try
    (let [gen (or (.get key-generator-thread-local)
                  (doto (KeyGenerator/getInstance algo)
                    (.init rng)))]
      (.set key-generator-thread-local gen)
      (.generateKey gen))
    (catch Exception e
      (throw (ex-info "Failed to generate cryptographic key" {:algorithm algo} e)))))

(defn generate-key
  "Generates a cryptographic key using the specified algorithm.
  Optionally accepts a seed for the random number generator."
  ([algo]      (generate-key-impl algo (SecureRandom.)))
  ([algo seed] (let [rng (-> seed
                             (.getBytes)
                             (SecureRandom.))]
                 (generate-key-impl algo rng))))

(defn encode-key [key]
  "Encodes a cryptographic key into a Base64 string with its algorithm."
  (try
    (let [encoded-key (.encodeToString (Base64/getEncoder)
                                       (.getEncoded key))]
      (format "%s:%s" (.getAlgorithm key) encoded-key))
    (catch Exception e
      (throw (ex-info "Failed to encode cryptographic key" {:key key} e)))))

(defn decode-key [key-str]
  "Decodes a Base64 encoded key string back into a SecretKeySpec object."
  (try
    (let [[algo encoded-key] (.split key-str ":" 2)
          key-bytes (.decode (Base64/getDecoder) encoded-key)]
      (SecretKeySpec. key-bytes algo))
    (catch Exception e
      (throw (ex-info "Failed to decode cryptographic key" {:key-str key-str} e)))))

(defn generate-signature [key data]
  "Generates a Base64 encoded signature for the given data using the specified key."
  (try
    (let [algo (.getAlgorithm key)
          mac (doto (Mac/getInstance algo)
                (.init key)
                (.update (.getBytes data)))]
      (.encodeToString (Base64/getEncoder) (.doFinal mac)))
    (catch Exception e
      (throw (ex-info "Failed to generate signature" {:key key :data data} e)))))

(defn validate-signature [key data sig]
  "Validates a signature by comparing it with a locally generated one for the given data and key."
  (try
    (let [local-sig (generate-signature key data)]
      (.equals sig local-sig))
    (catch Exception e
      (throw (ex-info "Failed to validate signature" {:key key :data data :signature sig} e)))))
(ns nexus.crypto-test
  (:require [clojure.test :refer :all]
            [nexus.crypto :as crypto]))

(deftest test-generate-key
  (testing "Key generation with default algorithm"
    (is (instance? javax.crypto.SecretKey (crypto/generate-key "HmacSHA512"))))
  (testing "Key generation with seed"
    (is (instance? javax.crypto.SecretKey (crypto/generate-key "HmacSHA512" "seed")))))

(deftest test-encode-decode-key
  (let [key (crypto/generate-key "HmacSHA512")
        encoded (crypto/encode-key key)
        decoded (crypto/decode-key encoded)]
    (testing "Encoding and decoding key"
      (is (= (.getAlgorithm key) (.getAlgorithm decoded)))
      (is (= (.getEncoded key) (.getEncoded decoded))))))

(deftest test-generate-signature
  (let [key (crypto/generate-key "HmacSHA512")
        data "test data"
        signature (crypto/generate-signature key data)]
    (testing "Signature generation"
      (is (string? signature)))))

(deftest test-validate-signature
  (let [key (crypto/generate-key "HmacSHA512")
        data "test data"
        signature (crypto/generate-signature key data)]
    (testing "Signature validation"
      (is (crypto/validate-signature key data signature))
      (is (not (crypto/validate-signature key "different data" signature))))))
(ns nexus.keygen-test
  (:require [clojure.test :refer :all]
            [nexus.keygen :as keygen]
            [nexus.crypto :as crypto]
            [clojure.java.io :as io]))

(deftest test-gen-key
  (testing "Key generation with default algorithm"
    (is (instance? javax.crypto.SecretKey (keygen/gen-key {:algorithm "HmacSHA512"}))))
  (testing "Key generation with seed"
    (is (instance? javax.crypto.SecretKey (keygen/gen-key {:algorithm "HmacSHA512" :seed "seed"})))))

(deftest test-write-key
  (let [key (crypto/generate-key "HmacSHA512")
        filename "test-key.txt"]
    (testing "Writing key to file"
      (keygen/write-key {:key key :filename filename})
      (is (.exists (io/file filename)))
      (finally
        (io/delete-file filename)))))

(deftest test-main
  (testing "Main function with verbose flag"
    (with-out-str
      (keygen/-main "-v" "-a" "HmacSHA512" "test-key.txt"))
    (is (.exists (io/file "test-key.txt")))
    (finally
      (io/delete-file "test-key.txt"))))
