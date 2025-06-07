(ns nexus.crypto-test
  (:require [clojure.test :refer :all]
            [nexus.crypto :as crypto]))

(deftest test-encode-decode-key
  (let [key (crypto/generate-key "HmacSHA512")
        encoded (crypto/encode-key key)
        decoded (crypto/decode-key encoded)]
    (testing "Encoding and decoding key"
      (is (= (.getAlgorithm key) (.getAlgorithm decoded)))
      (is (= (seq (.getEncoded key)) (seq (.getEncoded decoded)))))))
