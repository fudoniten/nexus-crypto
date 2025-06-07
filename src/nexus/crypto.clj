(ns nexus.crypto
  "Cryptographic utilities for key generation, encoding, and signature operations."
  (:import java.security.SecureRandom
           (javax.crypto Mac KeyGenerator)
           javax.crypto.spec.SecretKeySpec
           java.util.Base64))

(def ^:private key-generator-thread-local
  "Thread-local storage for KeyGenerator instances to ensure thread safety."
  (ThreadLocal.))

(defn- generate-key-impl [algo rng]
  "Generates a cryptographic key using the specified algorithm and random number generator (rng)."
  (let [gen (or (.get key-generator-thread-local)
                (doto (KeyGenerator/getInstance algo)
                  (.init rng)))]
    (.set key-generator-thread-local gen)
    (.generateKey gen)))

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
  (let [encoded-key (.encodeToString (Base64/getEncoder)
                                     (.getEncoded key))]
    (format "%s:%s" (.getAlgorithm key) encoded-key)))

(defn decode-key [key-str]
  "Decodes a Base64 encoded key string back into a SecretKeySpec object."
  (let [[algo encoded-key] (.split key-str ":" 2)
        key-bytes (.decode (Base64/getDecoder) encoded-key)]
    (SecretKeySpec. key-bytes algo)))

(defn generate-signature [key data]
  "Generates a Base64 encoded signature for the given data using the specified key."
  (let [algo (.getAlgorithm key)
        mac (doto (Mac/getInstance algo)
              (.init key)
              (.update (.getBytes data)))]
    (.encodeToString (Base64/getEncoder) (.doFinal mac))))

(defn validate-signature [key data sig]
  "Validates a signature by comparing it with a locally generated one for the given data and key."
  (let [local-sig (generate-signature key data)]
    (.equals sig local-sig)))
