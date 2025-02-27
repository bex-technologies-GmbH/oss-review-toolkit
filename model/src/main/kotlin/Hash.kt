/*
 * Copyright (C) 2017 The ORT Project Authors (see <https://github.com/oss-review-toolkit/ort/blob/main/NOTICE>)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package org.ossreviewtoolkit.model

import java.io.File
import java.util.Base64

import org.ossreviewtoolkit.utils.common.decodeHex
import org.ossreviewtoolkit.utils.common.encodeHex

/**
 * A class that bundles a hash algorithm with its hash value.
 */
data class Hash(
    /**
     * The value calculated using the hash algorithm.
     */
    val value: String,

    /**
     * The algorithm used to calculate the hash value.
     */
    val algorithm: HashAlgorithm
) {
    companion object {
        /**
         * A constant to specify that no hash value (and thus also no hash algorithm) is provided.
         */
        val NONE = Hash(HashAlgorithm.NONE.toString(), HashAlgorithm.NONE)

        /**
         * Create a [Hash] instance from a known hash [value]. If the [HashAlgorithm] cannot be determined, the original
         * [value] is returned with [HashAlgorithm.UNKNOWN], or with [HashAlgorithm.NONE] if the value is blank.
         */
        fun create(value: String): Hash {
            val splitValue = value.split('-')
            return if (splitValue.size == 2) {
                // Support Subresource Integrity (SRI) hashes, see
                // https://w3c.github.io/webappsec-subresource-integrity/
                Hash(
                    value = Base64.getDecoder().decode(splitValue.last()).encodeHex(),
                    algorithm = HashAlgorithm.fromString(splitValue.first())
                )
            } else {
                Hash(value, HashAlgorithm.create(value))
            }
        }

        /**
         * Create a [Hash] instance from a known hash [value] and [algorithm]. This is mostly used for deserialization
         * to verify the algorithm matches the one determined by the value.
         */
        fun create(value: String, algorithm: String): Hash =
            create(value).also { hash ->
                require(hash.algorithm == HashAlgorithm.fromString(algorithm)) {
                    "'$value' is not a $algorithm hash."
                }
            }
    }

    /**
     * Return the hash in Support Subresource Integrity (SRI) format.
     */
    fun toSri() = algorithm.name.lowercase() + "-" + Base64.getEncoder().encodeToString(value.decodeHex())

    /**
     * Verify that the [file] matches this hash.
     */
    fun verify(file: File): Boolean {
        require(algorithm in HashAlgorithm.VERIFIABLE) {
            "Cannot verify algorithm '$algorithm'. Supported algorithms are ${HashAlgorithm.VERIFIABLE}."
        }

        return algorithm.calculate(file).equals(value, ignoreCase = true)
    }

    /**
     * Verify that the provided [hash] matches this hash.
     */
    fun verify(hash: Hash): Boolean = algorithm == hash.algorithm && value.equals(hash.value, ignoreCase = true)
}
