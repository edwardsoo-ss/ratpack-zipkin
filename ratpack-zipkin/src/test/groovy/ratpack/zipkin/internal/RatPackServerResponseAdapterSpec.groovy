/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.zipkin.internal

import ratpack.zipkin.ResponseAnnotationExtractor

import static org.assertj.core.api.Assertions.assertThat

import com.github.kristofa.brave.KeyValueAnnotation
import com.github.kristofa.brave.ServerResponseAdapter
import ratpack.func.Function
import ratpack.http.Response
import ratpack.http.internal.DefaultStatus
import spock.genesis.Gen
import spock.lang.Specification


/**
 * Test suite for {@link RatpackServerResponseAdapter}.
 */
class RatpackServerResponseAdapterSpec extends Specification {
    def Response response = Stub(Response)
    def ResponseAnnotationExtractor extractor = Mock(ResponseAnnotationExtractor)
    def ServerResponseAdapter responseAdapter

    def setup() {
        responseAdapter = new RatpackServerResponseAdapter(response, extractor)
    }
    def 'Should include annotations from extractor function'() {
        def expected = KeyValueAnnotation.create("foo", "bar")
        setup:
            extractor.annotationsForRequest(response) >> Collections.singleton(expected)
        when:
            def Collection<KeyValueAnnotation> result = responseAdapter.responseAnnotations()
        then:
            assertThat(result)
                .contains(expected)
    }
    def 'Should return if extractor returns null'() {
        setup:
            extractor.annotationsForRequest(_) >> null
        when:
            def result = responseAdapter.responseAnnotations()
        then:
            result != null
    }
    def 'Should return if extractor errors'() {
        setup:
            extractor.annotationsForRequest(_) >> new IllegalArgumentException()
        when:
            def result = responseAdapter.responseAnnotations()
        then:
            result != null
    }
    def 'Should not return response annotation for 2xx status'(int statusCode) {
        setup:
            response.getStatus() >>  DefaultStatus.of(statusCode)
        when:
            def result = responseAdapter.responseAnnotations()
        then:
            result.isEmpty()
        where:
            statusCode << Gen.integer(200..299).take(10)

    }

    def 'Should return annotations for status (< 2xx)'(int statusCode) {
        setup:
            response.getStatus() >>  DefaultStatus.of(statusCode)
        when:
            def result = responseAdapter.responseAnnotations()
        then:
            !result.isEmpty()
            def entry = result.find {annotation -> annotation.getKey() == "http.responsecode"}
            entry.getValue() == statusCode.toString()
        where:
            statusCode << Gen.integer(100..199).take(10)
    }

    def 'Should return annotations for status (>= 3xx)'(int statusCode) {
        setup:
            response.getStatus() >>  DefaultStatus.of(statusCode)
        when:
            def result = responseAdapter.responseAnnotations()
        then:
            !result.isEmpty()
            def entry = result.find {annotation -> annotation.getKey() == "http.responsecode"}
            entry.getValue() == statusCode.toString()
        where:
            statusCode << Gen.integer(300..500).take(10)
    }
}